package com.jb.zcamera.imagefilter.filter;

import android.graphics.Matrix;
import android.opengl.GLES20;
import android.os.SystemClock;

import com.gomo.minivideo.CameraApp;
import com.gomo.minivideo.R;
import com.jb.zcamera.image.ImageHelper;
import com.jb.zcamera.imagefilter.util.OpenGlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * 下雪滤镜
 *
 * Created by oujingwen on 15-11-11.
 */
public class GPUImagePointDownFilter extends GPUImageFilter implements IDynamicFilter {
    public static final String VERTEX_SHADER = "" +
            "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute float a_pointSize;\n" +
            "attribute float u_Rotation;\n" +
            "uniform mat3 u_mvpMatrix;\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "varying float v_Rotation;\n" +

            "void main()\n" +
            "{\n" +
            "    gl_Position = vec4(a_position.xyz * u_mvpMatrix, 1.0);\n" +
            "    v_color = a_color;\n" +
            "    gl_PointSize = a_pointSize;\n" +
            "    v_Rotation = u_Rotation;\n" +
            "}\n";
    public static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying vec4 v_color;\n" +
            "uniform sampler2D u_texture0;\n" +
            "varying float v_Rotation;\n" +

            "void main()\n" +
            "{\n" +
            "    float mid = 0.5;\n" +
            "    vec2 rotated = vec2(cos(v_Rotation) * (gl_PointCoord.x - mid) + sin(v_Rotation) * (gl_PointCoord.y - mid) + mid,\n" +
            "                        cos(v_Rotation) * (gl_PointCoord.y - mid) - sin(v_Rotation) * (gl_PointCoord.x - mid) + mid);\n" +
            "    gl_FragColor = v_color * texture2D(u_texture0, rotated);\n" +
            "}\n";

    private int mSnowfallProgramId;
    protected int g_a_positionHandle;
    protected int g_a_colorHandle;
    protected int g_a_pointSizeHandle;
    protected int g_u_mvpMatrixHandle;
    protected int g_u_rotationHandle;

    protected int g_u_texture0Handle;

    // The Game's view size or area is 2 units wide and 3 units high.
    private static final float ViewMaxX = 1;
    private static final float ViewMaxY = 1;

    private static final int MaxSnowFlakes = 70;

    // Each snow flake will wait 3 seconds - then turn or change direction.
    private static final float TimeTillTurn = 3.0f;
    private static final float TimeTillTurnNormalizedUnit = 1.0f / TimeTillTurn;

    private long g_nowTime, g_prevTime;

    private float g_pos[] = new float[MaxSnowFlakes * 2];
    private float g_vel[] = new float[MaxSnowFlakes * 2];
    private float g_col[] = new float[MaxSnowFlakes * 4];
    private float g_size[] = new float[MaxSnowFlakes];
    private float g_rotation[] = new float[MaxSnowFlakes];
    private float g_rotation_v[] = new float[MaxSnowFlakes];
    private float g_timeSinceLastTurn[] = new float[MaxSnowFlakes];

    private float mStartTime[] = new float[MaxSnowFlakes];
    private float mLiveTime[] = new float[MaxSnowFlakes];

    protected Matrix mMatrix;
    private float[] mMatrixValues;

    private FloatBuffer mGLPosBuffer;
    private FloatBuffer mGLRotationBuffer;
    private FloatBuffer mGLVelBuffer;
    private FloatBuffer mGLColBuffer;
    private FloatBuffer mGLSize;

    private int mSnowtTextureId = OpenGlUtils.NO_TEXTURE;

    private Random mRandom;

    private float mDetal;

    private boolean mIsUpdate;

    private final static int MAX_SIZE = ImageHelper.dpToPx(CameraApp.getApplication().getResources(), 16);

    private final static int MIN_SIZE = ImageHelper.dpToPx(CameraApp.getApplication().getResources(), 8);

    public GPUImagePointDownFilter() {
        mMatrix = new Matrix();
        mMatrixValues = new float[9];
    }

    @Override
    public void onInit() {
        super.onInit();

        mRandom = new Random();

        mSnowfallProgramId = OpenGlUtils.loadProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        // Vertex shader variables
        g_a_positionHandle = GLES20.glGetAttribLocation(mSnowfallProgramId, "a_position");
        g_a_colorHandle = GLES20.glGetAttribLocation(mSnowfallProgramId, "a_color");
        g_a_pointSizeHandle = GLES20.glGetAttribLocation(mSnowfallProgramId, "a_pointSize");
        g_u_mvpMatrixHandle = GLES20.glGetUniformLocation(mSnowfallProgramId, "u_mvpMatrix");
        g_u_rotationHandle = GLES20.glGetAttribLocation(mSnowfallProgramId, "u_Rotation");

        // Fragment shader variables
        g_u_texture0Handle = GLES20.glGetUniformLocation(mSnowfallProgramId, "u_texture0");

        g_nowTime = SystemClock.uptimeMillis();
        g_prevTime = g_nowTime;


        mGLPosBuffer = ByteBuffer.allocateDirect(g_pos.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLVelBuffer = ByteBuffer.allocateDirect(g_vel.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLColBuffer = ByteBuffer.allocateDirect(g_col.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLSize = ByteBuffer.allocateDirect(g_size.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLRotationBuffer = ByteBuffer.allocateDirect(g_rotation.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

//        mGLPosBuffer.put(g_pos).position(0);
//        mGLVelBuffer.put(g_vel).position(0);
//        mGLColBuffer.put(g_col).position(0);
//        mGLSize.put(g_size).position(0);

        if (mSnowtTextureId == OpenGlUtils.NO_TEXTURE) {
            mSnowtTextureId = OpenGlUtils.loadTexture(CameraApp.getApplication().getResources(),
                    R.raw.snow3,  OpenGlUtils.NO_TEXTURE, false);
        }
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        super.onDraw(textureId, cubeBuffer, textureBuffer);
        update();
        draw();
    }

    private void update() {
        g_nowTime = SystemClock.uptimeMillis();
        float elapsed = (g_nowTime - g_prevTime) / 1000.0f;

        for( int i = 0; i < MaxSnowFlakes; ++i )
        {
            // Keep track of how long it has been since this flake turned
            // or changed direction.
            g_timeSinceLastTurn[i] += elapsed;

//            if( g_timeSinceLastTurn[i] >= TimeTillTurn )
//            {
//                // Change or invert direction!
//                g_vel[i * 2 + 0] = -(g_vel[i * 2 + 0]);
//                g_timeSinceLastTurn[i] = nextFloat(-5.0f, 0.0f);
//            }

            // Speed up the flake up as it leaves the last turn and prepares for next turn.
            float turnVelocityModifier = g_timeSinceLastTurn[i] * TimeTillTurnNormalizedUnit;

            // Apply some velocity to simulate gravity and wind.
            g_pos[i * 2 + 0] += g_vel[i * 2 + 0]; // Side to side
            g_pos[i * 2 + 1] += g_vel[i * 2 + 1]; // Gravity

            g_col[i * 4 + 0] = 1;
            g_col[i * 4 + 1] = 1;
            g_col[i * 4 + 2] = 1;

            g_col[i * 4 + 3] = countAlpha(g_pos[i * 2 + 1], mStartTime[i], mLiveTime[i]);

            g_rotation[i] = (float) Math.toRadians((Math.toDegrees(g_rotation[i]) + g_rotation_v[i]) % 360);

            // But, if the snow flake goes off the bottom or strays too far
            // left or right - respawn it back to the top.
            if( g_pos[i * 2 + 1] < -(ViewMaxY + 0.2f) ||
                    g_pos[i * 2 + 0] < -(ViewMaxX + 0.2f) || g_pos[i * 2 + 0] > (ViewMaxX + 0.2f) )
            {
                float flag = mRandom.nextFloat();
                if(flag >= 0.0f && flag <= 0.4f){
                    g_pos[i * 2 + 0] = nextFloat( 0, ViewMaxX );
                    g_pos[i * 2 + 1] = ViewMaxY + 0.01f;
                } else if(flag > 0.4f && flag <= 0.7f){
                    g_pos[i * 2 + 0] = ViewMaxX + 0.01f;
                    g_pos[i * 2 + 1] = nextFloat( 0, ViewMaxY );
                } else if(flag > 0.7f && flag <= 0.8f){
                    g_pos[i * 2 + 0] = nextFloat( -ViewMaxX / 2, 0 );
                    g_pos[i * 2 + 1] = ViewMaxY + 0.01f;
                } else if(flag > 0.8f && flag <= 0.9f){
                    g_pos[i * 2 + 0] = ViewMaxX + 0.01f;
                    g_pos[i * 2 + 1] = nextFloat( -ViewMaxY / 2, 0 );
                } else if(flag > 0.9f && flag <= 0.95f){
                    g_pos[i * 2 + 0] = nextFloat( -ViewMaxX, -ViewMaxX / 2 );
                    g_pos[i * 2 + 1] = ViewMaxY + 0.01f;
                } else{
                    g_pos[i * 2 + 0] = ViewMaxX + 0.01f;
                    g_pos[i * 2 + 1] = nextFloat( -ViewMaxY, -ViewMaxY / 2 );
                }

                g_col[i * 4 + 0] = 1;
                g_col[i * 4 + 1] = 1;
                g_col[i * 4 + 2] = 1;
                g_col[i * 4 + 3] = countAlpha(g_pos[i * 2 + 1], mStartTime[i], mLiveTime[i]);
            }
        }

        g_prevTime = g_nowTime;

        mGLPosBuffer.clear();
        mGLPosBuffer.put(g_pos).position(0);

        mGLVelBuffer.clear();
        mGLVelBuffer.put(g_vel).position(0);

        mGLColBuffer.clear();
        mGLColBuffer.put(g_col).position(0);

        mGLSize.clear();
        mGLSize.put(g_size).position(0);

        mGLRotationBuffer.clear();
        mGLRotationBuffer.put(g_rotation).position(0);
    }

    private void draw() {

        // Set the viewport
//        GLES20.glViewport ( 0, 0, mOutputWidth, mOutputHeight );

        // Doodle jump sky color (or something like it).
//        GLES20.glClearColor(0.31f, 0.43f, 0.63f, 1.0f);
//        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // We don't care about depth for point sprites.
//        GLES20.glDepthMask(false); // Turn off depth writes

//        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glEnable(GLES20.GL_POINT_SPRITE_OES);
        //glTexEnvi( GL_POINT_SPRITE_OES, GL_COORD_REPLACE_OES, GL_TRUE );

        GLES20.glUseProgram(mSnowfallProgramId);
//        runPendingOnDrawTasks();

        mMatrix.getValues(mMatrixValues);
        GLES20.glUniformMatrix3fv(g_u_mvpMatrixHandle, 1, false, mMatrixValues, 0);

        GLES20.glVertexAttribPointer(g_u_rotationHandle, 1, GLES20.GL_FLOAT, false, 0, mGLRotationBuffer);
        GLES20.glEnableVertexAttribArray(g_u_rotationHandle);
//        setUniformMatrix4f(g_u_mvpMatrixHandle, g_orthographicMatrix.getArray());
//        glUniformMatrix4fv( g_u_mvpMatrixHandle, 1, GL_FALSE, g_orthographicMatrix.m );

        GLES20.glVertexAttribPointer(g_a_positionHandle, 2, GLES20.GL_FLOAT, false, 0, mGLPosBuffer);
        GLES20.glEnableVertexAttribArray(g_a_positionHandle);

        GLES20.glVertexAttribPointer(g_a_colorHandle, 4, GLES20.GL_FLOAT, false, 0, mGLColBuffer);
        GLES20.glEnableVertexAttribArray(g_a_colorHandle);

        GLES20.glVertexAttribPointer(g_a_pointSizeHandle, 1, GLES20.GL_FLOAT, false, 0, mGLSize);
        GLES20.glEnableVertexAttribArray(g_a_pointSizeHandle);

        // Blend particles
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        if (mSnowtTextureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSnowtTextureId);
            GLES20.glUniform1i(g_u_texture0Handle, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, MaxSnowFlakes);

        GLES20.glDisableVertexAttribArray(g_a_positionHandle);
        GLES20.glDisableVertexAttribArray(g_a_colorHandle);
        GLES20.glDisableVertexAttribArray(g_a_pointSizeHandle);
        GLES20.glDisableVertexAttribArray(g_u_rotationHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);

        mDetal = width * 1.0f / ImageHelper.SCREEN_WIDTH;
//        if(mDetal > 1){
            mDetal = (height * 1.0f / ImageHelper.SCREEN_HEIGHT + mDetal) / 2;
//        } else {
//            mDetal = Math.max(height * 1.0f / ImageHelper.SCREEN_HEIGHT, mDetal);
//        }

        int sizeDuration = MAX_SIZE - MIN_SIZE;

        for( int i = 0; i < MaxSnowFlakes; ++i )
        {
            g_pos[i * 2 + 0] = nextFloat( -ViewMaxX, ViewMaxX );
            g_pos[i * 2 + 1] = nextFloat(-ViewMaxY, ViewMaxY);

//            if(i >= MaxSnowFlakes / 2){
//                g_pos[i * 2 + 0] = (i - MaxSnowFlakes / 2) * 0.1f;
//                g_pos[i * 2 + 1] = 0;
//            } else{
//                g_pos[i * 2 + 0] = (i + 1) * -0.1f;
//                g_pos[i * 2 + 1] = 0;
//            }

//            Loger.d("Test", "pos[" + i + "]=(" + g_pos[i * 2 + 0] + ", " + g_pos[i * 2 + 1] + ")");

            g_vel[i * 2 + 0] = nextFloat(-0.005f, -0.0032f); // Flakes move side to side
            g_vel[i * 2 + 1] = nextFloat(-0.008f, -0.006f); // Flakes fall down

            g_col[i * 4 + 0] = 1;
            g_col[i * 4 + 1] = 1;
            g_col[i * 4 + 2] = 1;
            g_col[i * 4 + 3] = 1; //RandomFloat( 0.6f, 1.0f ); // It seems that Doodle Jump snow does not use alpha.

            g_size[i] = nextFloat(MIN_SIZE, MAX_SIZE);

            // It looks strange if the flakes all turn at the same time, so
            // lets vary their turn times with a random negative value.
            g_timeSinceLastTurn[i] = nextFloat(-5.0f, 0.0f);

            mLiveTime[i] = (((g_size[i] - MIN_SIZE) / sizeDuration) * 2 + 1) / 3;

            mStartTime[i] = (3 - (((g_size[i] - MIN_SIZE) / sizeDuration) * 2) - 1) / 3;

            g_size[i] = g_size[i] * mDetal;

            g_rotation[i] = (float) Math.toRadians(nextFloat(0, 360));

            g_rotation_v[i] = nextFloat(-1f, 1f);
        }

        mGLPosBuffer.put(g_pos).position(0);
        mGLVelBuffer.put(g_vel).position(0);
        mGLColBuffer.put(g_col).position(0);
        mGLSize.put(g_size).position(0);
        mGLRotationBuffer.put(g_rotation).position(0);
    }

    @Override
    protected void onDrawArraysPre() {
        super.onDrawArraysPre();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(1, new int[]{
                mSnowtTextureId
        }, 0);
        mSnowtTextureId = OpenGlUtils.NO_TEXTURE;
        GLES20.glDeleteProgram(mSnowfallProgramId);
        mSnowfallProgramId = 0;
    }


    private float nextFloat(float min, float max) {
        float distance = max - min;
        float r = mRandom.nextFloat();
        return min + distance * r;
    }

    @Override
    public void onRotationChanged() {
        super.onRotationChanged();
        updateTextureCoord();
    }

    private void updateTextureCoord() {
        mMatrix.reset();
        if (mFlipHorizontal) {
            mMatrix.postScale(-1, 1, 0.5f, 0.5f);
        }
        if (mFlipVertical) {
            mMatrix.postScale(1, -1, 0.5f, 0.5f);
        }
        if (mRotation.asInt() != 0) {
            mMatrix.postRotate(mRotation.asInt(), 0.5f, 0.5f);
        }
        mMatrix.getValues(mMatrixValues);
    }

    public float countAlpha(float y, float startTime, float liveTime){
        if(y > 0){
            if(y < startTime){
                float index = (startTime - y) / liveTime;
                index = Math.min(index, 1);
                return (1 - index * 1.0f);
            } else{
                return 1;
            }
        } else{//y <= 0
            float index = (startTime - y) / liveTime;
            index = Math.min(index, 1);
            return (1 - index * 1.0f);
        }
    }

    @Override
    public void setUpdateOn(boolean on) {
        mIsUpdate = on;
    }

    @Override
    public boolean isUpdateOn() {
        return mIsUpdate;
    }

}
