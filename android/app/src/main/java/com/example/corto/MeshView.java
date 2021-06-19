package com.example.corto;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MeshView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = "CORTO_OPENGLES";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("libcorto");
    }
    // Call native decode function

    private SceneShader sceneShader;
    private ArrayList<Mesh> meshes;
    private final float[] mMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private CameraPerspective cameraPerspective;
    private static final Vector3f CAMERA_EYE = new Vector3f(3,2,3);
    private static final Vector3f CAMERA_CENTER = new Vector3f(0,0,0);
    private static final Vector3f CAMERA_UP = new Vector3f(0,1,0);

    public static int currentFrame = 0;
    public static int lastFrame = -1;
    public static Mesh meshObj;
    private byte[] bytes;

    public MeshView(Context context) {
        super(context);
        setSystemUiVisibility(SYSTEM_UI_FLAG_IMMERSIVE | SYSTEM_UI_FLAG_FULLSCREEN);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);
        GLES20.glClearDepthf(1.0f);
        GLES20.glFrontFace(GLES20.GL_CCW);

        cameraPerspective = new CameraPerspective(CAMERA_EYE, CAMERA_CENTER, CAMERA_UP, 1, 1000);

        meshObj = new Mesh(getContext(),"monkey.obj");
        sceneShader = new SceneShader(getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        cameraPerspective.setWidth(width).setHeight(height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(currentFrame != lastFrame){
                lastFrame = currentFrame;
                try {
                    InputStream is = getResources().openRawResource(R.raw.mesh);
                    int size = is.available();
                    bytes = new byte[size];

                    is.read(bytes, 0, bytes.length);
                    Log.v(TAG, "BYTES ARE " + size);
                    is.close();

                    // TODO: When object returns successfully, decode to the existing mesh
                    // For now we are returning dummy
                    Mesh returnedGeometryObject = decode(bytes);

                    Log.v(TAG, "BYTE[0] IS " + bytes[0]);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        cameraPerspective.loadVpMatrix();
        sceneShader.setViewPos(cameraPerspective.getEye());

        Matrix.setIdentityM(mMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, cameraPerspective.getVpMatrix(), 0, mMatrix, 0);

        sceneShader.setMesh(meshObj);
        sceneShader.setMMatrix(mMatrix);
        sceneShader.setMvpMatrix(mvpMatrix);
        sceneShader.bindData();
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, meshObj.getIndicesBuffer().capacity(), GLES20.GL_UNSIGNED_INT, meshObj.getIndicesBuffer());
        sceneShader.unbindData();
    }
    public native Mesh decode(byte[] bytes);

}
