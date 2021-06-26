package com.example.corto;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11Ext;

import timber.log.Timber;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

public class MeshView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private SceneShader sceneShader;
    private final float[] mMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private CameraPerspective cameraPerspective;
    private static final Vector3f CAMERA_EYE = new Vector3f(3,2,3);
    private static final Vector3f CAMERA_CENTER = new Vector3f(0,0,0);
    private static final Vector3f CAMERA_UP = new Vector3f(0,1,0);

    public Actor actor;

    public MeshView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Timber.d("onSurfaceCreated");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);
        GLES20.glClearDepthf(1.0f);
        GLES20.glFrontFace(GLES20.GL_CCW);

        cameraPerspective = new CameraPerspective(CAMERA_EYE, CAMERA_CENTER, CAMERA_UP, 1, 1000);
        // actor.mesh = new Mesh(getContext(),"monkey.obj");
        actor.onSurfaceCreated();
        actor.currentFrame = 0;
        actor.GetActorDataForFrame();

        sceneShader = new SceneShader(getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Timber.d("onSurfaceChanged");
        GLES20.glViewport(0, 0, width, height);
        cameraPerspective.setWidth(width).setHeight(height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //Timber.d("onDrawFrame");
        GLES20.glClearColor(1, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        cameraPerspective.loadVpMatrix();
        sceneShader.setViewPos(cameraPerspective.getEye());
        Matrix.setIdentityM(mMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, cameraPerspective.getVpMatrix(), 0, mMatrix, 0);
        sceneShader.setMMatrix(mMatrix);
        sceneShader.setMvpMatrix(mvpMatrix);
        if(actor != null){
            if(actor.isPrepared){
                actor.isPrepared = false;
//
//                final int[] textures = new int[1];
//                //Generate a texture to textures, and return a non-zero value if the generation is successful
//                GLES20.glGenTextures(1, textures, 0);
//
//
//                if (textures[0]==0){
//                    Log.d("ERROR","Failed to generate texture object");
//
//                }
//                //Bind the texture we just generated to OpenGL texture
//                GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
//
//                actor.surfaceTexture.attachToGLContext(textures[0]);


                //Used to set texture filtering method, GL_TEXTURE_MIN_FILTER is the filtering method when zooming out, GL_TEXTURE_MAG_FILTER is zooming in
//                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
//                        GLES20.GL_NEAREST);
//                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
//                        GLES20.GL_LINEAR);

//                int textureId = textures[0];
//
//                //SurfaceTexture is to get data of a new frame from the video stream and the camera data stream. Use updateTexImage to get the new data.
//                //Use textureId to create a SurfaceTexture
//                actor.surfaceTexture = new SurfaceTexture(textureId);
//                //Listening for a new frame data
//                actor.surfaceTexture.setOnFrameAvailableListener(actor);
//                //Use surfaceTexture to create a Surface
//                Surface surface = new Surface(actor.surfaceTexture);
//                //Set the surface as the output surface of the mediaPlayer
//                actor.mediaPlayer.setSurface(surface);
//                surface.release();

                Timber.d("Actor onCreate END");
            }

        actor.updateFrame();

        if(actor.mesh != null) {
            sceneShader.draw(actor.mesh, actor.mSTMatrix);
//            Log.v("WORKS", "actor != null && actor.mesh != null ");
//            Log.v("actor != null", actor.toString());
//            Log.v("actor.mesh", actor.mesh.toString());
        }
        } else {
            actor.currentFrame = 0;
            Log.v("DOESNT WORK", "actor != null && actor.mesh != null ");
        }

    }
}
