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
import android.opengl.GLES20;
import android.util.Log;

import com.jb.zcamera.imagefilter.util.ImageFilterTools;
import com.jb.zcamera.imagefilter.util.OpenGlUtils;
import com.jb.zcamera.imagefilter.util.Rotation;
import com.jb.zcamera.imagefilter.util.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GPUImageMultiInputFilter extends GPUImageFilter {
	protected static final String SHADER_STRING = "f2pqbHd8a2p7Pmh7fSo+bnFtd2p3cXAlFH9qamx3fGtqez5oe30qPndwbmtqSntmamtse11xcWx6d3B/anslFH9qamx3fGtqez5oe30qPndwbmtqSntmamtse11xcWx6d3B/anssJRQ+FGh/bGd3cHk+aHt9LD5qe2Zqa2x7XXFxbHp3cH9qeyUUaH9sZ3dweT5oe30sPmp7ZmprbHtdcXFsendwf2p7LCUUPhRocXd6PnN/d3A2NxRlFD4+Pj55ckFOcW13andxcD4jPm5xbXdqd3FwJRQ+Pj4+antmamtse11xcWx6d3B/ans+Iz53cG5rakp7ZmprbHtdcXFsendwf2p7MGZnJRQ+Pj4+antmamtse11xcWx6d3B/anssPiM+d3Bua2pKe2Zqa2x7XXFxbHp3cH9qeywwZmclFGM=";

    protected static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
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
    private ByteBuffer mTexture2CoordinatesBuffer;
    protected Bitmap[] mBitmaps;
    protected String[] mSamplers;
    protected Integer[] mSamplerUniformLocation;
    public Integer[] mFilterSourceTexture;
    protected int mSamplerCount;
    
    private float mIntentsity = 1;
    private int mIntentsityLocation;

    public GPUImageMultiInputFilter(String vertexShader, String fragmentShader, String... samplers) {
        super(vertexShader, fragmentShader);
        setRotation(Rotation.NORMAL, false, false);
        mSamplers = samplers;
        mSamplerCount = samplers.length;
        mSamplerUniformLocation = new Integer[mSamplerCount];
        mBitmaps = new Bitmap[mSamplerCount];
        mFilterSourceTexture = new Integer[mSamplerCount];
        for (int i = 0; i < mSamplerCount; ++i) {
        	mFilterSourceTexture[i] = OpenGlUtils.NO_TEXTURE;
        }
    }

    @Override
    public void onInit() {
        super.onInit();

        int program = getProgram();
        mFilterSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(program, "inputTextureCoordinate2");
        checkGLError("1");
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        checkGLError("2");
        for (int i = 0; i < mSamplerCount; ++i) {
        	mSamplerUniformLocation[i] = GLES20.glGetUniformLocation(program, mSamplers[i]);
        	checkGLError("3 / " + i);
        }
        
        mIntentsityLocation = GLES20.glGetUniformLocation(program, "intensity");
        
        for (int i = 0; i < mSamplerCount; ++i) {
        	if (mBitmaps[i] != null && !mBitmaps[i].isRecycled()) {
        		setBitmap(i + 1, mBitmaps[i]);
        	}
        }
        
    }
    
    @Override
    public void onInitialized() {
    	super.onInitialized();
        setIntensity(mIntentsity);
    }
    
    public void setIntensity(float intensity) {
        mIntentsity = intensity;
        setFloat(mIntentsityLocation, intensity);
    }

    @Override
    public float getIntensity() {
        return mIntentsity;
    }

    @Override
    public boolean isSupportIntensity() {
        return true;
    }

    public void setBitmap(final int i, final Bitmap bitmap) {
    	if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
    	mBitmaps[i - 1] = bitmap;
        runOnDraw(new Runnable() {
            public void run() {
                if (mFilterSourceTexture[i - 1] == OpenGlUtils.NO_TEXTURE) {
                    if (bitmap == null || bitmap.isRecycled()) {
                        return;
                    }
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE3 + i - 1);
                    mFilterSourceTexture[i - 1] = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false);
                }
            }
        });
    }

    public Bitmap getBitmap(int i) {
        return mBitmaps[i];
    }
    
    public void recycleBitmap() {
    	for (int i = 0; i < mSamplerCount; ++i) {
    		if (mBitmaps[i] != null && !mBitmaps[i].isRecycled()) {
    			mBitmaps[i].recycle();
    			mBitmaps[i] = null;
    		}
    	}
    }

    public void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < mSamplerCount; ++i) {
        	GLES20.glDeleteTextures(1, new int[]{
        			mFilterSourceTexture[i]
        	}, 0);
        	mFilterSourceTexture[i] = OpenGlUtils.NO_TEXTURE;
        }
        
    }
    
	public static boolean checkGLError(String op) {
		int error;
		boolean foundError = false;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e("DWM", op + ": glError " + error);
			foundError = true;
		}
		return foundError;
	}

    @Override
    protected void onDrawArraysPre() {
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        for (int i = 0; i < mSamplerCount; ++i) {
            if(mFilterSourceTexture[i] != OpenGlUtils.NO_TEXTURE){
                GLES20.glActiveTexture(GLES20.GL_TEXTURE3 + i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterSourceTexture[i]);
                GLES20.glUniform1i(mSamplerUniformLocation[i], 3 + i);
            }
        }

        mTexture2CoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(mFilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexture2CoordinatesBuffer);
        
    }

    public void setRotation(final Rotation rotation, final boolean flipHorizontal, final boolean flipVertical) {
        float[] buffer = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);

        ByteBuffer bBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
        FloatBuffer fBuffer = bBuffer.asFloatBuffer();
        fBuffer.put(buffer);
        fBuffer.flip();

        mTexture2CoordinatesBuffer = bBuffer;
        
    }
}
