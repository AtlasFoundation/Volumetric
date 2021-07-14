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

export default class Player {
  // Public Fields
  public frameRate: number = 30;
  public speed: number = 1.0; // Multiplied by framerate for final playback output rate
  public loop: boolean = true;
  public encoderWindowSize = 8; // length of the databox
  public encoderByteLength = 16;
  public videoSize = 1024;

  // Three objects
  public scene: Object3D;
  public renderer: Renderer;
  public mesh: Mesh;
  public meshFilePath: String;
  public material: MeshBasicMaterial;
  public failMaterial: MeshBasicMaterial;
  public bufferGeometry: BufferGeometry;

  // Private Fields
  private readonly _scale: number = 1;
  private _video: HTMLVideoElement | AdvancedHTMLVideoElement = null;
  private _videoTexture = null;
  private meshBuffer: Map<number, BufferGeometry> = new Map();
  private meshBufferRequested: Map<number, boolean> = new Map();
  private _worker: Worker;
  private onMeshBuffering: onMeshBufferingCallback | null = null;
  private onFrameShow: onFrameShowCallback | null = null;
  private rendererCallback: onRenderingCallback | null = null;
  fileHeader: any;
  tempBufferObject: BufferGeometry;

  private manifestFilePath: any;
  private counterCtx: CanvasRenderingContext2D;
  private actorCtx: CanvasRenderingContext2D;

  private numberOfFrames: number;
  private maxNumberOfFrames: number;
  private actorCanvas: HTMLCanvasElement;
  currentFrame: number = -1;
  lastFrameRequested: number = -1;
  targetFramesToRequest: number;
  minimumBufferLength = 400

  bufferLoop = () => {

    const minimumBufferLength = Math.min(this.minimumBufferLength, this.numberOfFrames);
    const isOnLoop = this.lastFrameRequested + 1 < this.currentFrame;
  
    for (const [key, buffer] of this.meshBuffer.entries()) {
      // If key is between current keyframe and last requested, don't delete
      if ((isOnLoop && (key > this.lastFrameRequested && key < this.currentFrame - 10)) ||
        (!isOnLoop && key < this.currentFrame - 10)) {
        // console.log("Destroying", key);
        if (buffer && buffer instanceof BufferGeometry) {
          buffer?.dispose();
        }
        this.meshBuffer.delete(key);
      }
    }

    const meshBufferHasEnoughToPlay = this.meshBuffer.size >= minimumBufferLength
    const ammountLeftToPlay = isOnLoop ? 
      this.numberOfFrames - this.currentFrame + this.lastFrameRequested :
      this.lastFrameRequested - this.currentFrame

    if (meshBufferHasEnoughToPlay && this.meshBuffer.has(this.currentFrame)) {
      if(this._video.paused && this.hasPlayed)
        this._video.play();
    }
    else if (ammountLeftToPlay < this.minimumBufferLength) {
      const newFirstFrame = (this.lastFrameRequested + 1) % this.numberOfFrames
      const newLastFrame = (this.lastFrameRequested + this.targetFramesToRequest) % this.numberOfFrames;
      const payload = {
        frameStart: newFirstFrame,
        frameEnd: newLastFrame
      }
      if (payload.frameEnd < payload.frameStart) {
        payload.frameEnd = this.numberOfFrames - 1 
      }
      this.lastFrameRequested = payload.frameEnd
      if (this.meshBuffer.has(payload.frameStart) && this.meshBuffer.has(payload.frameEnd)) {
        return
      }
      // console.log("Posting request", payload);
      // for (let i=payload.frameStart; i <= payload.frameEnd) {

      // }
      // this.meshBufferRequested.set()
      this._worker.postMessage({ type: "request", payload }); // Send data to our worker.;

      if (!meshBufferHasEnoughToPlay && typeof this.onMeshBuffering === "function") {
        const minimumBufferLength = Math.min(this.minimumBufferLength, this.numberOfFrames);
        console.log('currentFrame', this.currentFrame, 'buffering ', this.meshBuffer.size / minimumBufferLength,',  have: ', this.meshBuffer.size, ', need: ', minimumBufferLength )
        this.onMeshBuffering(this.meshBuffer.size / minimumBufferLength);
      }
    }

    // requestAnimationFrame(() => this.bufferLoop());
  }

  hasPlayed: boolean;

  constructor({
                scene,
                renderer,
                manifestFilePath = null,
                meshFilePath,
                videoFilePath,
                targetFramesToRequest = 40,
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
    manifestFilePath?: string,
    meshFilePath: string,
    videoFilePath: string,
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

    this._worker = worker ?? (new Worker('./workerFunction.ts')); // spawn new worker;

    this.scene = scene;
    this.renderer = renderer;
    this.meshFilePath = meshFilePath;
    this.manifestFilePath = manifestFilePath ?? meshFilePath.replace('uvol', 'manifest');
    this.loop = loop;
    this._scale = scale;
    this._video = video ?? createElement('video', {
      crossorigin: "",
      playsInline: "true",
      preload: "auto",
      loop: true,
      src: videoFilePath,
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

    this._videoTexture = new Texture(this.actorCanvas);
    this._videoTexture.encoding = sRGBEncoding;
    this.material = new MeshBasicMaterial({ map: this._videoTexture });

    this.failMaterial = new MeshBasicMaterial({ color: '#555555' });
    this.mesh = new Mesh(new PlaneBufferGeometry(0.00001, 0.00001), this.material);
    this.mesh.scale.set(this._scale, this._scale, this._scale);
    this.mesh.visible = true
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
        const minimumBufferLength = Math.min(this.minimumBufferLength, this.numberOfFrames);
        // console.log('buffering ', this.meshBuffer.size / minimumBufferLength,',  have: ', this.meshBuffer.size, ', need: ', minimumBufferLength )
        this.onMeshBuffering(this.meshBuffer.size / this.minimumBufferLength);
      }
    }

    worker.onmessage = (e) => {
      switch (e.data.type) {
        case 'initialized':
          console.log("Worker initialized");
          // Promise.resolve().then(() => {
          //   this.bufferLoop();
          // });
          break;
        case 'framedata':
          Promise.resolve().then(() => {
            handleFrameData(e.data.payload);
          });
          break;
      }
    };

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = () => {
      if (xhr.readyState !== 4) return;
      this.fileHeader = JSON.parse(xhr.responseText);
      this.frameRate = this.fileHeader.frameRate;

      // Get count of frames associated with keyframe
      this.numberOfFrames = this.fileHeader.frameData.length;

      if (this.numberOfFrames > this.maxNumberOfFrames) {
        console.error('There are more frames (%d) in file then our decoder can handle(%d) with provided encoderByteLength(%d)', this.numberOfFrames, this.maxNumberOfFrames, this.encoderByteLength);
      }

      worker.postMessage({ type: "initialize", payload: { targetFramesToRequest, meshFilePath, numberOfFrames: this.numberOfFrames, fileHeader: this.fileHeader } }); // Send data to our worker.
    };

    xhr.open('GET', this.manifestFilePath, true); // true for asynchronous
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
    this.bufferLoop()
    // Promise.resolve().then(this.bufferLoop)
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

    if (this.currentFrame > 0 && frameToPlay === 0) {
      // error getting current frame to play ? 
      return;
    }

    if (this.currentFrame === frameToPlay) {
      return;
    }

    this.currentFrame = frameToPlay;

    const hasFrame = this.meshBuffer.has(frameToPlay);
    // If keyframe changed, set mesh buffer to new keyframe

    if (!hasFrame) {
      console.log('missing frame! ', frameToPlay)
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
      this._videoTexture.needsUpdate = true;

      this.mesh.geometry = this.meshBuffer.get(frameToPlay) as BufferGeometry;
      this.mesh.geometry.attributes.position.needsUpdate = true;
      (this.mesh.geometry as any).needsUpdate = true;

      if (typeof this.onFrameShow === "function") {
        this.onFrameShow(frameToPlay);
      }
      if(this.rendererCallback) this.rendererCallback();
      if(cb) cb();
    }
  }

  getCurrentFrameNumber():number {
    // return Math.floor((this._video.currentTime / this._video.duration) * this.numberOfFrames)

    const encoderWindowWidth = this.encoderWindowSize * this.encoderByteLength;
    const encoderWindowHeight = this.encoderWindowSize / 2;

    // this.actorCtx.clearRect(0, 0, this.videoSize, this.videoSize);
    this.actorCtx.drawImage(this._video, 0, 0);

    // this.counterCtx.clearRect(0, 0, this.encoderByteLength, 1);
    this.counterCtx.drawImage(
      this.actorCanvas,
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

    return frameToPlay;
  }

  get video():any {
    return this._video;
  }

  // Start loop to check if we're ready to play
  play() {
    this.hasPlayed = true;
    this._video.playsInline = true;
    this._video.play()
  }

  dispose(): void {
    this._worker?.terminate();
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
          buffer?.dispose();
        }
      }
      this.meshBuffer.clear();
    }
  }
}
