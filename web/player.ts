import {
  BufferGeometry,
  Float32BufferAttribute,
  Mesh,
  MeshBasicMaterial,
  Object3D,
  PlaneBufferGeometry,
  Renderer,
  sRGBEncoding,
  Texture,
  Uint32BufferAttribute,
  WebGLRenderer
} from 'three';
import { moduloBy, createElement } from './utils';

type AdvancedHTMLVideoElement = HTMLVideoElement & { requestVideoFrameCallback: (callback: (number, { }) => void) => void };
type onMeshBufferingCallback = (progress: number) => void;
type onFrameShowCallback = (frame: number) => void;
type onRenderingCallback = () => void;
enum PlayModeEnum {
  Single = 1,
  Random,
  Loop,
  SingleLoop,
}

export default class Player {
  // static defaultWorkerURL = new URL('./worker.build.es.js', import.meta.url).href
  static defaultWorkerURL = new URL('../../node_modules/volumetric/dist/worker.build.es.js', import.meta.url).href

  // Public Fields
  public frameRate: number = 30;
  public speed: number = 1.0; // Multiplied by framerate for final playback output rate
  public loop: boolean = true;
  public encoderWindowSize = 8; // length of the databox
  public encoderByteLength = 16;
  public videoSize = 1024;
  public playMode: PlayModeEnum;
  public waitForVideoLoad = 3 //3 seconds
  public hasPlayed: boolean = false;

  // Three objects
  public scene: Object3D;
  public renderer: Renderer;
  public mesh: Mesh;
  public paths: Array<String>;
  public material: MeshBasicMaterial;
  public failMaterial: MeshBasicMaterial;
  public bufferGeometry: BufferGeometry;

  // Private Fields
  private readonly _scale: number = 1;
  private _video: HTMLVideoElement | AdvancedHTMLVideoElement = null;
  private _videoTexture = null;
  private meshBuffer: Map<number, BufferGeometry> = new Map();
  private _worker: Worker;
  private onMeshBuffering: onMeshBufferingCallback | null = null;
  private onFrameShow: onFrameShowCallback | null = null;
  private rendererCallback: onRenderingCallback | null = null;
  fileHeader: any;
  tempBufferObject: BufferGeometry;

  private meshFilePath: String;

  private currentTrack: number = 0;
  private nextTrack: number = 0;
  private manifestFilePath: any;
  private videoFilePath: any;
  private counterCtx: CanvasRenderingContext2D;
  private actorCtx: CanvasRenderingContext2D;

  private numberOfFrames: number = 0;
  private numberOfNextFrames: number = 0;
  private isWorkerWaitNextLoop: boolean = false;
  private isWorkerReady: boolean = false;
  private stopOnNextTrack: boolean = false;
  private stopOnNextFrame: boolean = false;
  private isWorkerBusy: boolean = false;
  private isVideoReady: boolean = false;
  private maxNumberOfFrames: number;
  private actorCanvas: HTMLCanvasElement;
  currentFrame: number = 0;
  lastFrameRequested: number = 0;
  targetFramesToRequest: number = 30;

  set paused(value){
    if(!value) this.play();
    else {
      this._video.pause();
      this.hasPlayed = false;
      this.stopOnNextFrame = false;
    }
  }

  bufferLoop = () => {

    const isOnLoop = this.lastFrameRequested < this.currentFrame;
    
    for (const [key, buffer] of this.meshBuffer.entries()) {
      // If key is between current keyframe and last requested, don't delete
      if ((isOnLoop && (key > this.lastFrameRequested && key < this.currentFrame)) ||
        (!isOnLoop && key < this.currentFrame)) {
        // console.log("Destroying", key);
        if (buffer && buffer instanceof BufferGeometry) {
          buffer.dispose();
        }
        this.meshBuffer.delete(key);
      }
    }

    const minimumBufferLength = this.targetFramesToRequest * 2;
    const meshBufferHasEnoughToPlay = this.meshBuffer.size >= minimumBufferLength;
    const meshBufferHasEnoughForSnap = this.meshBuffer.size >= minimumBufferLength * 2;

    if (meshBufferHasEnoughToPlay) {
      if(this.isVideoReady && this._video.paused && this.hasPlayed)
        this._video.play();
    }
    if (!this.isWorkerBusy && this.isWorkerReady && !meshBufferHasEnoughForSnap) {
      if (this.isWorkerWaitNextLoop) {
        this.isWorkerWaitNextLoop = false
        this.handleNextLoop()
      }
      // if (moduloBy(this.lastFrameRequested - this.currentFrame, this.numberOfFrames) <= minimumBufferLength * 2) {
      else {
        let newLastFrame = Math.max(this.lastFrameRequested + minimumBufferLength, this.lastFrameRequested + this.targetFramesToRequest);
        
        if (newLastFrame >= this.numberOfFrames - 1) {
          newLastFrame = this.numberOfFrames - 1
        }
        newLastFrame = newLastFrame % this.numberOfFrames

        const payload = {
          frameStart: this.lastFrameRequested,
          frameEnd: newLastFrame
        }
        console.log("Posting request", payload);
        this._worker.postMessage({ type: "request", payload }); // Send data to our worker.
        this.isWorkerBusy = true;

        if (newLastFrame >= this.numberOfFrames - 1) {
          this.lastFrameRequested = 0
          this.isWorkerWaitNextLoop = true
        } else {
          this.lastFrameRequested = newLastFrame;
        }

        if (!meshBufferHasEnoughToPlay && typeof this.onMeshBuffering === "function") {
          // console.log('buffering ', this.meshBuffer.size / minimumBufferLength,',  have: ', this.meshBuffer.size, ', need: ', minimumBufferLength )
          this.onMeshBuffering(this.meshBuffer.size / minimumBufferLength);
        }
      }
    }
    requestAnimationFrame(() => this.bufferLoop());
  }

  constructor({
                scene,
                renderer,
                playMode,
                paths,
                targetFramesToRequest = 90,
                frameRate = 30,
                loop = true,
                scale = 1,
                encoderWindowSize = 8,
                encoderByteLength = 16,
                videoSize = 1024,
                video = null,
                onMeshBuffering = null,
                onFrameShow = null,
                rendererCallback = null,
                worker = null
              }: {
    scene: Object3D,
    renderer: WebGLRenderer,
    playMode?: PlayModeEnum,
    paths: Array<String>,
    targetFramesToRequest?: number,
    frameRate?: number,
    loop?: boolean,
    autoplay?: boolean,
    scale?: number,
    video?: any,
    encoderWindowSize?: number,
    encoderByteLength?: number,
    videoSize?: number,
    onMeshBuffering?: onMeshBufferingCallback
    onFrameShow?: onFrameShowCallback,
    rendererCallback?: onRenderingCallback,
    worker?: Worker
  }) {

    this.onMeshBuffering = onMeshBuffering;
    this.onFrameShow = onFrameShow;
    this.rendererCallback = rendererCallback;

    this.encoderWindowSize = encoderWindowSize;
    this.encoderByteLength = encoderByteLength;
    this.maxNumberOfFrames = Math.pow(2, this.encoderByteLength)-2;
    this.videoSize = videoSize;

    this.targetFramesToRequest = targetFramesToRequest;

    this._worker = worker ? worker : (new Worker(Player.defaultWorkerURL)); // spawn new worker;

    this.scene = scene;
    this.renderer = renderer;
    this.loop = loop;
    this._scale = scale;
    this._video = video ? video : createElement('video', {
      crossorigin: "",
      playsInline: "true",
      preload: "auto",
      style: {
        display: "none",
        position: 'fixed',
        zIndex: '-1',
        top: '0',
        left: '0',
        width: '1px'
      },
      playbackRate: 1
    });

    this.paths = paths

    if (playMode != undefined) {
      this.playMode = playMode
    } else {
      this.playMode = PlayModeEnum.Loop
    }

    this._video.setAttribute('crossorigin', '');
    this._video.setAttribute('preload', 'auto');

    this.frameRate = frameRate;

    const counterCanvas = document.createElement('canvas') as HTMLCanvasElement;
    counterCanvas.width = this.encoderByteLength;
    counterCanvas.height = 1;

    this.counterCtx = counterCanvas.getContext('2d');
    this.actorCanvas = document.createElement('canvas')
    this.actorCtx = this.actorCanvas.getContext('2d');

    this.actorCtx.canvas.width = this.actorCtx.canvas.height = this.videoSize;
    this.counterCtx.canvas.setAttribute('crossOrigin', 'Anonymous');
    this.actorCtx.canvas.setAttribute('crossOrigin', 'Anonymous');

    this.actorCtx.fillStyle = '#ACC';
    this.actorCtx.fillRect(0, 0, this.actorCtx.canvas.width, this.actorCtx.canvas.height);

    this._videoTexture = new Texture(this.actorCtx.canvas);
    this._videoTexture.encoding = sRGBEncoding;
    this.material = new MeshBasicMaterial({ map: this._videoTexture });

    this.failMaterial = new MeshBasicMaterial({ color: '#555555' });
    this.mesh = new Mesh(new PlaneBufferGeometry(0.00001, 0.00001), this.material);
    this.mesh.scale.set(this._scale, this._scale, this._scale);
    this.scene.add(this.mesh);


    const handleFrameData = (messages) => {
      // console.log(`received frames ${messages[0].keyframeNumber} - ${messages[messages.length-1].keyframeNumber}`)
      for (const frameData of messages) {
        let geometry = new BufferGeometry();
        geometry.setIndex(
          new Uint32BufferAttribute(frameData.bufferGeometry.index, 1)
        );
        geometry.setAttribute(
          'position',
          new Float32BufferAttribute(frameData.bufferGeometry.position, 3)
        );
        geometry.setAttribute(
          'uv',
          new Float32BufferAttribute(frameData.bufferGeometry.uv, 2)
        );

        this.meshBuffer.set(frameData.keyframeNumber, geometry );
      }

      if (typeof this.onMeshBuffering === "function") {
        const minimumBufferLength = this.targetFramesToRequest * 2;
        // console.log('buffering ', this.meshBuffer.size / minimumBufferLength,',  have: ', this.meshBuffer.size, ', need: ', minimumBufferLength )
        this.onMeshBuffering(this.meshBuffer.size / minimumBufferLength);
      }
    }

    this._worker.onmessage = (e) => {
      switch (e.data.type) {
        case 'initialized':
          console.log("Worker initialized");
          this.isWorkerReady = true
          Promise.resolve().then(() => {
            this.bufferLoop();
          });
          break;
        case 'framedata':
          Promise.resolve().then(() => {
            this.isWorkerBusy = false;
            handleFrameData(e.data.payload);
          });
          break;
      }
    };

    this.setTrackPath(this.currentTrack)
    this.setVideo(this.videoFilePath)
    this.setWorker(this.manifestFilePath, this.meshFilePath)
  }

  handleNextLoop() {
    if (this.playMode == PlayModeEnum.Random) {
      this.nextTrack = Math.floor(Math.random() * this.paths.length)
    } else if (this.playMode == PlayModeEnum.Single) {
      this.nextTrack = (this.currentTrack + 1) % this.paths.length
      if ((this.currentTrack + 1) == this.paths.length) {
        this.nextTrack = 0
        this.isWorkerReady = false
        this.stopOnNextTrack = true
      }
    } else if (this.playMode == PlayModeEnum.SingleLoop) {
      this.nextTrack = this.currentTrack
    } else { //PlayModeEnum.Loop
      this.nextTrack = (this.currentTrack + 1) % this.paths.length
    }
    this.setTrackPath(this.nextTrack)
    this.setWorker(this.manifestFilePath, this.meshFilePath)
  }

  handleLoop() {
    if (this.nextTrack == -1) {
      this.nextTrack = 0
      return
    }
    if (this.numberOfNextFrames != 0) this.numberOfFrames = this.numberOfNextFrames
    this.currentTrack = this.nextTrack
    this.setVideo(this.videoFilePath)
    this.hasPlayed = true;
    if (this.stopOnNextTrack) {
      this.paused = true
    }
  }

  setTrackPath(track) {
    const meshFilePath = this.paths[track % this.paths.length]
    this.meshFilePath = meshFilePath
    this.manifestFilePath = `${meshFilePath.substring(0, meshFilePath.lastIndexOf("."))}.manifest`
    this.videoFilePath = `${meshFilePath.substring(0, meshFilePath.lastIndexOf("."))}.mp4`
  }

  setVideo(videoFilePath) {
    this.isVideoReady = false
    if (!this._video.paused) {
      this._video.pause();
      this.hasPlayed = false;
      this.stopOnNextFrame = false;
    }
    this._video.setAttribute('src', videoFilePath);
    this._video.load()
    this._video.addEventListener('loadeddata', (event) => {
      setTimeout(() => {
        this.isVideoReady = true;
    }, this.waitForVideoLoad * 1000)
    });
  }

  setWorker(manifestFilePath, meshFilePath) {
    this.isWorkerReady = false;
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
      if (xhr.readyState !== 4) return;
      this.fileHeader = JSON.parse(xhr.responseText);
      this.frameRate = this.fileHeader.frameRate;

      // Get count of frames associated with keyframe
      this.numberOfNextFrames = this.fileHeader.frameData.length;
      if (this.numberOfFrames == 0) this.numberOfFrames = this.numberOfNextFrames

      if (this.numberOfNextFrames > this.maxNumberOfFrames) {
        console.error('There are more frames (%d) in file then our decoder can handle(%d) with provided encoderByteLength(%d)', this.numberOfNextFrames, this.maxNumberOfFrames, this.encoderByteLength);
      }

      this._worker.postMessage({ type: "initialize", payload: { targetFramesToRequest: this.targetFramesToRequest, meshFilePath, numberOfFrames: this.numberOfNextFrames, fileHeader: this.fileHeader } }); // Send data to our worker.
    };

    xhr.open('GET', manifestFilePath, true); // true for asynchronous
    xhr.send();
  }

  /**
   * emulated video frame callback
   * bridge from video.timeupdate event to videoUpdateHandler
   * @param cb
   */
  handleRender(cb?: onRenderingCallback) {
    if (!this.fileHeader) // || (this._video.currentTime === 0 || this._video.paused))
      return;

    // TODO: handle paused state
    this.processFrame(cb);
  }

  /**
   * sync mesh frame to video texture frame
   * calls this.rendererCallback and provided callback if frame is changed and render needs update
   * @param cb
   */
  processFrame(cb?: onRenderingCallback) {
    const frameToPlay = this.getCurrentFrameNumber();

    if (frameToPlay > this.numberOfFrames) {
      console.warn('video texture is not ready? frameToPlay:', frameToPlay);
      return;
    }

    if (this.currentFrame === frameToPlay) {
      return;
    }

    this.currentFrame = frameToPlay;

    if (this.currentFrame >= this.numberOfFrames - 1) {
      this.handleLoop()
    }

    if (this.stopOnNextFrame) {
      this._video.pause();
      this.hasPlayed = false;
      this.stopOnNextFrame = false;
    }

    const hasFrame = this.meshBuffer.has(frameToPlay);
    // If keyframe changed, set mesh buffer to new keyframe

    if (!hasFrame) {
      if (!this._video.paused) {
        this._video.pause();
      }
      if (typeof this.onMeshBuffering === "function") {
        this.onMeshBuffering(0);
      }
      this.mesh.material = this.failMaterial;
    } else {
      this.mesh.material = this.material;
      this.material.needsUpdate = true;

      this.mesh.material.needsUpdate = true;

      this.mesh.geometry = this.meshBuffer.get(frameToPlay) as BufferGeometry;
      this.mesh.geometry.attributes.position.needsUpdate = true;
      (this.mesh.geometry as any).needsUpdate = true;

      this.currentFrame = frameToPlay;

      if (typeof this.onFrameShow === "function") {
        this.onFrameShow(frameToPlay);
      }
      if(this.rendererCallback) this.rendererCallback();
      if(cb) cb();
    }
  }

  getCurrentFrameNumber():number {
    const encoderWindowWidth = this.encoderWindowSize * this.encoderByteLength;
    const encoderWindowHeight = this.encoderWindowSize / 2;
    // this.actorCtx.clearRect(0, 0, this.videoSize, this.videoSize);
    this.actorCtx.drawImage(this._video, 0, 0);

    // this.counterCtx.clearRect(0, 0, this.encoderByteLength, 1);
    this.counterCtx.drawImage(
      this.actorCtx.canvas,
      0,
      this.videoSize - encoderWindowHeight,
      encoderWindowWidth,
      encoderWindowHeight,
      0,
      0,
      this.encoderByteLength, 1);

    const imgData = this.counterCtx.getImageData(0, 0, this.encoderByteLength, 1);

    let frameToPlay = 0;
    for (let i = 0; i < this.encoderByteLength; i++) {
      frameToPlay += Math.round(imgData.data[i * 4] / 255) * Math.pow(2, i);
    }

    frameToPlay = Math.max(frameToPlay - 1, 0);

    this._videoTexture.needsUpdate = this.currentFrame !== frameToPlay;

    return frameToPlay;
  }

  get video():any {
    return this._video;
  }

  // Start loop to check if we're ready to play
  play() {
    this.hasPlayed = true;
    this.stopOnNextTrack = false
    this._video.playsInline = true;
    this.mesh.visible = true
    this._video.play()
  }

  pause() {
    this.paused = true
  }
  
  playOneFrame() {
    this.stopOnNextFrame = true;
    this.play();
  }

  dispose(): void {
    this._worker && this._worker.terminate();
    if (this._video) {
      this._video.pause();
      this._video = null;
      this._videoTexture.dispose();
      this._videoTexture = null;
    }
    if (this.meshBuffer) {
      for (let i = 0; i < this.meshBuffer.size; i++) {
        const buffer = this.meshBuffer.get(i);
        if (buffer && buffer instanceof BufferGeometry) {
          buffer.dispose();
        }
      }
      this.meshBuffer.clear();
    }
  }
}
