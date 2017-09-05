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
import android.opengl.GLES20;

import com.gomo.minivideo.CameraApp;
import com.jb.zcamera.imagefilter.util.OpenGlUtils;
import com.jb.zcamera.imagefilter.util.Rotation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

public class GPUImageTwoInputFilter extends GPUImageFilter {
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

    public int mFilterSecondTextureCoordinateAttribute;
    public int mFilterInputTextureUniform2;
    public int mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    private ByteBuffer mTexture2CoordinatesBuffer;
    private Bitmap mBitmap;
    private int mCurrentIndex = 0;
    private int mMaxIndex = 0;
    private List<String> mBitmapList;

    public GPUImageTwoInputFilter(String fragmentShader) {
        this(VERTEX_SHADER, fragmentShader);
    }

    public GPUImageTwoInputFilter(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader);
        setRotation(Rotation.NORMAL, false, false);
    }

    @Override
    public void onInit() {
        super.onInit();

        mFilterSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
        mFilterInputTextureUniform2 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);

        updateTextureCoord();

        if (mBitmap != null&&!mBitmap.isRecycled()) {
            setBitmap(mBitmap);
        }
    }

    public void setBitmapList(List<String> bitmapList) {
        this.mBitmapList = bitmapList;
        mCurrentIndex = 0;
        mMaxIndex = mBitmapList.size() - 1;
        if (bitmapList.size() > 0) {
            setBitmap(BitmapFactory.decodeFile(mBitmapList.get(mCurrentIndex)));
        }
    }
    
    public void setBitmap(final Bitmap bitmap) {
        if (bitmap != null && bitmap.isRecycled()) {
            return;
        }
        mBitmap = bitmap;
        if (mBitmap == null) {
            return;
        }
        runOnDraw(new Runnable() {
            public void run() {
                if (bitmap.isRecycled()) {
                    return;
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                boolean recycle = mBitmapList != null && mBitmapList.size() > 1;
                mFilterSourceTexture2 = OpenGlUtils.loadTexture(bitmap, mFilterSourceTexture2, recycle);
            }
        });
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void recycleBitmap() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(1, new int[]{
                mFilterSourceTexture2
        }, 0);
        mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterSourceTexture2);
        GLES20.glUniform1i(mFilterInputTextureUniform2, 3);

        mTexture2CoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(mFilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexture2CoordinatesBuffer);

        if (mBitmapList != null && mBitmapList.size() > 1) {
            mCurrentIndex++;
            if (mCurrentIndex > mMaxIndex) {
                mCurrentIndex = 0;
            }
            setBitmap(BitmapFactory.decodeFile(mBitmapList.get(mCurrentIndex)));
        }
    }

    public void setRotation(final Rotation rotation, final boolean flipHorizontal, final boolean flipVertical) {
        super.setRotation(rotation, flipHorizontal, flipVertical);
    }

    public void setTexture2(int textureId, final Rotation rotation, final boolean flipHorizontal, final boolean flipVertical) {
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            mFilterSourceTexture2 = textureId;
            if (mRotation != rotation || mFlipHorizontal != flipHorizontal
                    || mFlipVertical != flipVertical) {
                boolean flag = mFlipVertical != flipVertical;
                setRotation(rotation, flipHorizontal, flipVertical);
                if (flag) {
                    onFlipVerticalChanged();
                }
            }
        }
    }

    public void onFlipVerticalChanged() {
    }

    @Override
    public void onRotationChanged() {
        super.onRotationChanged();
        updateTextureCoord();
    }

    public void updateTextureCoord() {
        float[] buffer = {
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
        matrix.mapPoints(buffer, buffer);
        ByteBuffer bBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
        FloatBuffer fBuffer = bBuffer.asFloatBuffer();
        fBuffer.put(buffer);
        fBuffer.flip();
        mTexture2CoordinatesBuffer = bBuffer;
    }
}
