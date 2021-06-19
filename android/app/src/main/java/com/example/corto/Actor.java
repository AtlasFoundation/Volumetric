package com.example.corto;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Actor implements SurfaceTexture.OnFrameAvailableListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private static final String TAG = "CORTO_OPENGLES";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("libcorto");
    }

    private static final String TAG_ACTOR = "ACTOR_CLASS";
    private final MeshView view;
    private int textureId;

    public Mesh mesh;
    public String manifestUrl;
    public String uvolUrl;
    public String videoUrl;
    private SurfaceTexture surfaceTexture;
    private MediaPlayer mediaPlayer;
    private JSONArray frameData;
    private Context context;
    private boolean playerPrepared;
    public boolean updateSurface;
    private InputStream uvolInputStream;
    private int currentUvolPosition = 0;

    public float frameRate = 30;

    public int currentFrame = 0;
    public int lastFrame = -1;

    public void setCurrentFrameFromTime(){
        this.currentFrame = (int)(mediaPlayer.getCurrentPosition() * this.frameRate)/1000;
    }

    public void setLastFrameToCurrentFrame(){
        lastFrame = currentFrame;
    }

    public Actor(Context context, MeshView view, String manifestUrl, String uvolUrl, String videoUrl){
        this.manifestUrl = manifestUrl;
        this.uvolUrl = uvolUrl;
        this.videoUrl = videoUrl;
        this.mediaPlayer = MediaPlayer.create(context, Uri.parse(videoUrl));
        this.context = context;
        this.view = view;
        this.mesh = new Mesh();
        LoadManifest();
        LoadVideo();
        LoadUvol();
    }

    public void LoadManifest(){
        try {
            InputStream is = this.context.getAssets().open(this.manifestUrl);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();
            JSONObject manifest = new JSONObject(new String(buffer, "UTF-8"));
            this.frameRate = manifest.getInt("frameRate");
            this.frameData = manifest.getJSONArray("frameData");
        } catch (IOException | JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void LoadUvol(){
        // TODO: Open an input stream, set position to 0, and allocate input stream
        try {
            uvolInputStream = context.getAssets().open(this.uvolUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ActorData GetActorDataForFrame(){
        // TODO: Get start and end lengths from manifest
        try {
            Log.v(TAG, " this.currentFrame is " +  this.currentFrame);

            JSONObject frameData = this.frameData.getJSONObject(this.currentFrame);

            ActorData actorData = new ActorData();
            actorData.startBytePosition = frameData.getInt("startBytePosition");
            actorData.length = frameData.getInt("meshLength");
            actorData.vertices = frameData.getInt("vertices");
            actorData.faces = frameData.getInt("faces");

            Log.v(TAG, "length " + actorData.length);
            Log.v(TAG, "startBytePosition " + actorData.startBytePosition);


            Log.v(TAG, "available " + uvolInputStream.available());

            if(currentUvolPosition > actorData.startBytePosition){
                uvolInputStream = context.getAssets().open(this.uvolUrl);
                uvolInputStream.skip(actorData.startBytePosition);
            } else {
                uvolInputStream.skip(actorData.startBytePosition - currentUvolPosition);
            }

            currentUvolPosition = actorData.startBytePosition;

            byte[] bytes = new byte[actorData.length];
            uvolInputStream.read(bytes, 0, actorData.length);
            Log.v(TAG, "BYTES ARE " + actorData.length);

            // TODO: When object returns successfully, decode to the existing mesh
            // For now we are returning dummy
            this.mesh = decode(bytes);


        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void LoadVideo(){
//        try {
////            this.mediaPlayer.setDataSource(context, Uri.parse(this.videoUrl));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //Set loop play
        mediaPlayer.setLooping(true);
        mediaPlayer.setOnPreparedListener(this);

    }

    @Override
    public void onPrepared(MediaPlayer player) {
        playerPrepared = true;

        textureId = loadTexture(context);

        //SurfaceTexture is to get data of a new frame from the video stream and the camera data stream. Use updateTexImage to get the new data.
        //Use textureId to create a SurfaceTexture
        surfaceTexture = new SurfaceTexture(textureId);
        //Listening for a new frame data
        surfaceTexture.setOnFrameAvailableListener(this);
        //Use surfaceTexture to create a Surface
        Surface surface = new Surface(surfaceTexture);
        //Set the surface as the output surface of the mediaPlayer
        mediaPlayer.setSurface(surface);
        surface.release();

        Log.d(TAG_ACTOR, "Actor onCreate END");
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public void Play(){
        mediaPlayer.start();
    }

    public void Stop(){
        mediaPlayer.stop();
    }

    public void Destroy(){
        mediaPlayer.reset();
        try {
            uvolInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Deallocate input stream
    }

    public static int loadTexture(Context context){

        final int[] textures = new int[1];
        //Generate a texture to textures, and return a non-zero value if the generation is successful
        GLES20.glGenTextures(1, textures, 0);

        if (textures[0]==0){
            Log.d(TAG_ACTOR,"Failed to generate texture object");
            return 0;
        }
        //Bind the texture we just generated to OpenGL 3D texture, and tell OpenGL that this is a 3D texture
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);

        //Used to set texture filtering method, GL_TEXTURE_MIN_FILTER is the filtering method when zooming out, GL_TEXTURE_MAG_FILTER is zooming in
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        return textures[0];
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.setCurrentFrameFromTime();
        if(lastFrame != currentFrame){
            this.GetActorDataForFrame();
            updateSurface = true;
            this.setLastFrameToCurrentFrame();
        }
    }


    // Call native decode function

    public native Mesh decode(byte[] bytes);
}
