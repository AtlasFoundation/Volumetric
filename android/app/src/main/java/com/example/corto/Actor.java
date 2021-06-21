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

import timber.log.Timber;

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
    public SurfaceTexture surfaceTexture;
    public MediaPlayer mediaPlayer;
    private JSONArray frameData;
    private Context context;
    public boolean updateSurface;
    private InputStream uvolInputStream;
    private int currentUvolPosition = 0;

    public boolean isPrepared = false;

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
        this.mesh = null;
        LoadManifest();
        LoadVideo();
        LoadUvol();
    }

    public void LoadManifest(){
        Timber.d("LoadManifest");
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

    public void GetActorDataForFrame(){
        Timber.d("GetActorDataForFrame");
        // TODO: Get start and end lengths from manifest
        try {
            Log.v(TAG, " this.currentFrame is " +  this.currentFrame);

            JSONObject frameData = this.frameData.getJSONObject(this.currentFrame);

            int startBytePosition = frameData.getInt("startBytePosition");
            int length = frameData.getInt("meshLength");
//            int vertices = frameData.getInt("vertices");
//            int faces = frameData.getInt("faces");

            Log.v(TAG, "length " + length);
            Log.v(TAG, "startBytePosition " + startBytePosition);


            Log.v(TAG, "available " + uvolInputStream.available());

            if(currentUvolPosition > startBytePosition){
                uvolInputStream = context.getAssets().open(this.uvolUrl);
                uvolInputStream.skip(startBytePosition);
            } else {
                uvolInputStream.skip(startBytePosition - currentUvolPosition);
            }

            currentUvolPosition = startBytePosition;

            byte[] bytes = new byte[length];
            uvolInputStream.read(bytes, 0, length);
            Log.v(TAG, "BYTES ARE " + length);

            this.mesh = decode(bytes);
            this.mesh.init();
            Log.v(TAG, "MESH INITED ");
            Timber.e("GetActorDataForFrame finish");

        } catch (JSONException | IOException e) {
            Timber.e("GetActorDataForFrame "+e);
            e.printStackTrace();
        }
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
//
        isPrepared = true;
        Play();

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

    public static int loadTexture(){

        final int[] textures = new int[1];
        //Generate a texture to textures, and return a non-zero value if the generation is successful
        GLES20.glGenTextures(1, textures, 0);

        if (textures[0]==0){
            Log.d(TAG_ACTOR,"Failed to generate texture object");
            return 0;
        }
        //Bind the texture we just generated to OpenGL 3D texture, and tell OpenGL that this is a 3D texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        //Used to set texture filtering method, GL_TEXTURE_MIN_FILTER is the filtering method when zooming out, GL_TEXTURE_MAG_FILTER is zooming in
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        return textures[0];
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.setCurrentFrameFromTime();
        if(lastFrame != currentFrame){
            updateSurface = true;
       }
    }


    // Call native decode function

    public native Mesh decode(byte[] bytes);
}
