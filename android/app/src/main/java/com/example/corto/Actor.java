package com.example.corto;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import timber.log.Timber;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

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

    public float[] mSTMatrix = new float[16];
    private int mTextureID;

    public boolean isPrepared = false;

    public float frameRate = 30;

    public int currentFrame = 0;
    public int lastFrame = -1;

    public void setCurrentFrameFromTime(){
        this.currentFrame = (int)(mediaPlayer.getCurrentPosition() * this.frameRate)/1000;
    }

    public void updateFrame()
    {

        synchronized(this)
        {
            if (updateSurface)
            {
                //Timber.d("updateFrame");
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(mSTMatrix);

//                GetActorDataForFrame();
                setLastFrameToCurrentFrame();
                updateSurface = false;
            }
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

    }
    public void setLastFrameToCurrentFrame(){
        lastFrame = currentFrame;
        surfaceTexture.updateTexImage();
    }
    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Timber.e(op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
    public Actor(Context context, MeshView view, String manifestUrl, String uvolUrl, MediaPlayer player){
        this.manifestUrl = manifestUrl;
        this.uvolUrl = uvolUrl;
        mediaPlayer = player;

        this.context = context;
        this.view = view;
        this.mesh = null;

        LoadManifest();
        LoadUvol();
        Matrix.setIdentityM(mSTMatrix, 0);
    }

    public void onSurfaceCreated()
    {
        Timber.d("onSurfaceCreated");
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

        Timber.d("mTextureID %d", mTextureID);
        checkGlError("glBindTexture mTextureID");

        surfaceTexture = new SurfaceTexture(mTextureID);
        surfaceTexture.setOnFrameAvailableListener(this);

        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);
        mediaPlayer.setScreenOnWhilePlaying(true);
        surface.release();

        mediaPlayer.setLooping(true);
        mediaPlayer.setOnPreparedListener(this);

        try {
            mediaPlayer.prepareAsync();
        } catch (Exception t) {
            Timber.e("media player prepare failed "+t);
            t.printStackTrace();
        }

        synchronized(this) {
            updateSurface = false;
        }

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
        Timber.d("GetActorDataForFrame %d",currentFrame);
        // TODO: Get start and end lengths from manifest
        try {

            JSONObject frameData = this.frameData.getJSONObject(this.currentFrame);

            int startBytePosition = frameData.getInt("startBytePosition");
            int length = frameData.getInt("meshLength");
//            int vertices = frameData.getInt("vertices");
//            int faces = frameData.getInt("faces");

            Timber.d("length " + length);
            Timber.d("startBytePosition " + startBytePosition);


            Timber.d("available " + uvolInputStream.available());

            if(currentUvolPosition > startBytePosition){
                uvolInputStream = context.getAssets().open(this.uvolUrl);
                uvolInputStream.skip(startBytePosition);
            } else {
                uvolInputStream.skip(startBytePosition - currentUvolPosition);
            }

            currentUvolPosition = startBytePosition;

            byte[] bytes = new byte[length];
            uvolInputStream.read(bytes, 0, length);
            Timber.d("BYTES ARE " + length);

            this.mesh = decode(bytes);
            this.mesh.init();
            Timber.d("MESH INITED ");

        } catch (JSONException | IOException e) {
            Timber.e("GetActorDataForFrame "+e);
            e.printStackTrace();
        }
    }


    @Override
    public void onPrepared(MediaPlayer player) {
        Timber.d("onPrepared");
        isPrepared = true;
        Play();

    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public void Play(){
        Timber.d("Play");
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
    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Timber.d("onFrameAvailable");
        this.setCurrentFrameFromTime();
        if(lastFrame != currentFrame){
            updateSurface = true;
       }
    }


    // Call native decode function

    public native Mesh decode(byte[] bytes);
}
