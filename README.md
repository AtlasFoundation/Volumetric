# Universal Volumetric

An open source .uvol format for streaming meshes and volumetric playback with extremely fast encode + decode.

### Example
This tech was built in partnership with Wild Capture and others. You can see an example on Wild Capture's site, here: http://wildcapture.co/volumetric.html

Current uvol files consist of a .uvol binary, manifest file and video texture. Future versions will embed everything into the uvol binary or in a single MP4 container.

Currently playback works in WebGL with three.js -- Unity and Unreal support are available and coming soon, get in touch if you're interested!

## Requirements
For encoding, you will need Node.js 12+ and Python 3 installed.

For decoding, currently WebGL is supported (especially three.js), Unreal and Unity will come in the next release.

## Encoding

### Mesh

First, you need a .ply sequence. If you have a .obj sequence, you can convert to to ply using this command:
```
bash src/encoder/make_plys.sh
```

Next, put your ply files in the src/encoder/encode folder. If you want to change this path, you can do so by modifying Encoder.js (NOTE: this will be moved to an argument in the next version).

Run the encoder on the meshes:
```bash
node ./src/Encoder.js example.uvol
```

If you want to set a specific framerange or frame rate, you can set that as well
```bash
// Extended Example: 25 FPS, 500 frames
node ./src/Encoder.js example.uvol 25 0 499
```

### Texture

Texture is stored as an H264 video.

Due to inadequacies in iOS frame sync (as well as multithreaded framesync issues in Unity) we are baking the frame number directly into the texture. This frame sync is 8px high, 128px wide. After some experimentation, we found that this is resistant to aliasing and macroblocking in video. However, it might cause issues with your textures unless you pre-process your textures be offset by 8 px from the bottom of your image. The next version will autoscale your UVs to have 8px available at the bottom.

### Encoding frame counter to texture
```python
python3 src/encoder/texture_encoder.py
```

### Encoding image sequence to MP4
```bash
ffmpeg -r 30 -s 1024x1024 -i src/encoder/encode/tex%05d.png -vcodec libx264 -crf 25 example.mp4
```

