package com.sid.esleep.snapsid;

import android.opengl.GLSurfaceView;



import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameGLRenderer implements GLSurfaceView.Renderer {

    private Triangle mTriangle;
    private Rectangle mRectangle;
    private Cube mCube;
    float x=0,y=0,z=1;
    float[] scratch = new float[16];
    float[] scratch1 = new float[16];
    private final float[] mMVPMatrix = new float[16];
    private final float[] mMVPMatrix1 = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mProjectionMatrix1 = new float[16];
    private final float[] mViewMatrix1 = new float[16];
    private float[] mRotationMatrix = new float[16];
    private float[] mRotationMatrix1 = new float[16];
    private  float[] mModelMatrix = new float[16];
    private float [] mModelMatrix1  = new float[16];
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.5f);
        mTriangle = new Triangle();
        mRectangle = new Rectangle();
        mCube = new Cube();

    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        // Calculate the projection and view transformation
        long time = SystemClock.uptimeMillis() % 4000L;
        float angle = 0.090f * ((int) time);
        Matrix.setRotateM(mRotationMatrix, 0, angle, 0, 0, -1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        // Draw triangle
        mTriangle.draw(scratch);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 100);
        Matrix.frustumM(mProjectionMatrix1, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public static int loadShader(int type, String shaderCode){


        int shader = GLES20.glCreateShader(type);


        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}