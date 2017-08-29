/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jb.zcamera.imagefilter.filter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;

import com.gomo.minivideo.CameraApp;
import com.jb.zcamera.imagefilter.util.OpenGlUtils;
import com.jb.zcamera.imagefilter.util.Rotation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GPUImageAnimationFilter extends GPUImageFilter {
    private static final String VERTEX_SHADER = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            "}";

    public static final String SCREEN_BLEND_FRAGMENT_SHADER = "varying highp vec2 textureCoordinate;\n" +
            " varying highp vec2 textureCoordinate2;\n" +
            "\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform sampler2D inputImageTexture2;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     mediump vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "     mediump vec4 whiteColor = vec4(1.0);\n" +
            "     gl_FragColor = whiteColor - ((whiteColor - textureColor2) * (whiteColor - textureColor));\n" +
            " }";

    private static final int BASE_WITH = 1080;
    private static final int BASE_HEIGHT = 1920;

    public int mFilterSecondTextureCoordinateAttribute;
    public int mFilterInputTextureUniform2;
    public int mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    private FloatBuffer mTexture2CoordinatesBuffer;

    private Rect mStickerRect = new Rect(680, 340, 1040, 40);

    private int mIndex;
    private long mPreTime;

    private int[] mStickerIds;
    private Bitmap[] mBitmaps;
    private int mFrameCount;
    private int mFrameDelay;

    public GPUImageAnimationFilter(int[] stickerIds, int frameDelay, Rect stickerRect) {
        super(VERTEX_SHADER, SCREEN_BLEND_FRAGMENT_SHADER);
        mStickerIds = stickerIds;
        mFrameCount = mStickerIds.length;
        mFrameDelay = frameDelay;
        mStickerRect = stickerRect;
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        updateTextureCoord();
    }

    @Override
    public void onInit() {
        super.onInit();

        mFilterSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
        mFilterInputTextureUniform2 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);

        mBitmaps = new Bitmap[mFrameCount];

        for(int i = 0; i < mFrameCount; i ++) {
            mBitmaps[i] = BitmapFactory.decodeResource(CameraApp.getApplication().getResources(), mStickerIds[i]);
        }
        mIndex = 0;
        mPreTime = 0;
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        update();
        super.onDraw(textureId, cubeBuffer, textureBuffer);
    }

    private void update() {
        long nowTime = System.currentTimeMillis();
        if (mPreTime == 0) {
            mIndex = 0;
            mFilterSourceTexture2 = OpenGlUtils.loadTexture(mBitmaps[mIndex], mFilterSourceTexture2, false);
            mPreTime = nowTime;
        } else {
            int dsIn = (int) ((nowTime - mPreTime) / mFrameDelay);
            if (dsIn > 0) {
                mIndex = (mIndex + dsIn) % mFrameCount;
                mFilterSourceTexture2 = OpenGlUtils.loadTexture(mBitmaps[mIndex], mFilterSourceTexture2, false);
                mPreTime = nowTime;
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(1, new int[]{
                mFilterSourceTexture2
        }, 0);
        mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
        for (Bitmap bitmap : mBitmaps) {
            bitmap.recycle();
        }
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterSourceTexture2);
        GLES20.glUniform1i(mFilterInputTextureUniform2, 3);

        mTexture2CoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(mFilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexture2CoordinatesBuffer);
    }

    public void setRotation(final Rotation rotation, final boolean flipHorizontal, final boolean flipVertical) {
        boolean changed = mRotation != rotation || flipHorizontal != mFlipHorizontal || flipVertical != mFlipVertical;
        super.setRotation(rotation, flipHorizontal, flipVertical);
        if (changed) {
            updateTextureCoord();
        }
    }

    @Override
    public void setFlipVertical(boolean flipVertical) {
        boolean changed = flipVertical != mFlipVertical;
        super.setFlipVertical(flipVertical);
        if (changed) {
            updateTextureCoord();
        }
    }

    @Override
    public void setFlipHorizontal(boolean flipHorizontal) {
        boolean changed = flipHorizontal != mFlipHorizontal;
        super.setFlipHorizontal(flipHorizontal);
        if (changed) {
            updateTextureCoord();
        }
    }

    @Override
    public void setRotation(Rotation rotation) {
        boolean changed = mRotation != rotation;
        super.setRotation(rotation);
        if (changed) {
            updateTextureCoord();
        }
    }

    private void updateTextureCoord() {
        int outputWidth = mOutputWidth;
        int outputHeight = mOutputHeight;
        if (mRotation == Rotation.ROTATION_90 || mRotation == Rotation.ROTATION_270) {
            outputWidth = mOutputHeight;
            outputHeight = mOutputWidth;
        }
        float scale = (float)outputWidth / BASE_WITH;
        RectF scaledStickerRect = new RectF(mStickerRect.left * scale, mStickerRect.top * scale,
                mStickerRect.right * scale, mStickerRect.bottom * scale);
        RectF invertStickerRect = new RectF(scaledStickerRect.right, outputHeight - scaledStickerRect.bottom,
                scaledStickerRect.left, outputHeight - scaledStickerRect.top);
        RectF standStickerRect = new RectF(invertStickerRect.left / outputWidth, invertStickerRect.top / outputHeight,
                invertStickerRect.right / outputWidth, invertStickerRect.bottom / outputHeight);
        float[] coords = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };
        Matrix matrix = new Matrix();
        if (mFlipHorizontal) {
            matrix.postScale(-1, 1, 0.5f, 0.5f);
        }
        if (mFlipVertical) {
            matrix.postScale(1, -1, 0.5f, 0.5f);
        }
        if (mRotation.asInt() != 0) {
            matrix.postRotate(mRotation.asInt(), 0.5f, 0.5f);
        }
        matrix.postTranslate(-standStickerRect.right, -standStickerRect.bottom);
        float scaleX = 1 / Math.abs(standStickerRect.width());
        float scaleY = 1 / Math.abs(standStickerRect.height());
        matrix.postScale(scaleX, scaleY, 0 , 0);
        matrix.mapPoints(coords, coords);
        FloatBuffer fBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fBuffer.put(coords);
        fBuffer.flip();
        mTexture2CoordinatesBuffer = fBuffer;
    }
}
