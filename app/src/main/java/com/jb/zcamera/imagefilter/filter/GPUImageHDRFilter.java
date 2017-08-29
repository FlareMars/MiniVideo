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

import android.opengl.GLES20;

import com.jb.zcamera.imagefilter.GPUImageRenderer;
import com.jb.zcamera.imagefilter.util.OpenGlUtils;
import com.jb.zcamera.imagefilter.util.Rotation;
import com.jb.zcamera.imagefilter.util.TextureRotationUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**
 * A hardware-accelerated 9-hit box blur of an image
 *
 * scaling: for the size of the applied blur, default of 1.0
 */
public class GPUImageHDRFilter extends GPUImageFilter {

//    public static final String FRAGMENT_SHADER_V = "precision mediump float;\n" +
//            "precision mediump int;\n" +
//            "\n" +
//            "uniform sampler2D inputImageTexture;\n" +
//            "\n" +
//            "varying vec2 textureCoordinate;\n" +
//            "\n" +
//            "uniform float param_BoxBlur_texel_width;\n" +
//            "uniform float param_BoxBlur_scale;\n" +
//            "\n" +
//            "void main() {\n" +
//            "\n" +
//            "  float singleOffset = param_BoxBlur_texel_width * param_BoxBlur_scale;\n" +
//            "  float singleCoord = textureCoordinate.x * param_BoxBlur_scale;\n" +
//            "  float newOffset = 0.;\n" +
//            " \n" +
//            "   vec3 fragmentColor = texture2D(inputImageTexture, vec2(singleCoord, textureCoordinate.y * param_BoxBlur_scale)).rgb;\n" +
//            "  int i = 0;\n" +
//            "  for(i; i < 7; i++) {\n" +
//            "   fragmentColor += texture2D(inputImageTexture, vec2(max(singleCoord - newOffset, 0.), textureCoordinate.y * param_BoxBlur_scale)).rgb;\n" +
//            "   fragmentColor += texture2D(inputImageTexture, vec2(min(singleCoord + newOffset, 0.99), textureCoordinate.y * param_BoxBlur_scale)).rgb;\n" +
//            "   newOffset += singleOffset;\n" +
//            "  }\n" +
//            "  gl_FragColor = vec4(fragmentColor / 15., 1.0);\n" +
//            "}";
//
//    public static final String FRAGMENT_SHADE_H = "precision mediump float;\n" +
//            "precision mediump int;\n" +
//            "\n" +
//            "uniform sampler2D inputImageTexture;\n" +
//            "\n" +
//            "varying vec2 textureCoordinate;\n" +
//            "\n" +
//            "uniform float param_BoxBlur_texel_height;\n" +
//            "uniform float param_BoxBlur_scale;\n" +
//            "\n" +
//            "void main() {\n" +
//            "\n" +
//            "  float singleOffset = param_BoxBlur_texel_height * param_BoxBlur_scale;\n" +
//            "  float singleCoord = textureCoordinate.y * param_BoxBlur_scale;\n" +
//            "  float newOffset = 0.;\n" +
//            "   \n" +
//            "  vec3 fragmentColor = texture2D(inputImageTexture, vec2(textureCoordinate.x * param_BoxBlur_scale, singleCoord)).rgb;\n" +
//            " int i = 0;\n" +
//            " for(i; i < 7; i++) {\n" +
//            "  fragmentColor += texture2D(inputImageTexture, vec2(textureCoordinate.x * param_BoxBlur_scale, max(singleCoord - newOffset, 0.))).rgb;\n" +
//            "  fragmentColor += texture2D(inputImageTexture, vec2(textureCoordinate.x * param_BoxBlur_scale, min(singleCoord + newOffset, 0.99))).rgb;\n" +
//            "  newOffset += singleOffset;\n" +
//            " }\n" +
//            " gl_FragColor = vec4(fragmentColor / 15., 1.0);\n" +
////            " gl_FragColor = texture2D(inputImageTexture, textureCoordinate);" +
//            "}";
//
//    private static final String VERTEX_SHADER_T = "attribute vec4 position;\n" +
//            "attribute vec4 inputTextureCoordinate;\n" +
//            "attribute vec4 inputTextureCoordinate2;\n" +
//            " \n" +
//            "varying vec2 textureCoordinate;\n" +
//            "varying vec2 textureCoordinate2;\n" +
//            " \n" +
//            "void main()\n" +
//            "{\n" +
//            "    gl_Position = position;\n" +
//            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
//            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
//            "}";
//
//    public static final String FRAGMENT_SHADE_T = "precision mediump float;\n" +
//            "\n" +
//            "uniform sampler2D inputImageTexture;\n" +
//            "uniform sampler2D inputImageTexture2;\n" +
//            "\n" +
//            "varying vec2 textureCoordinate;\n" +
//            "varying vec2 textureCoordinate2;\n" +
//            "\n" +
//            "uniform float param_HDR_effect_strength;\n" +
//            "uniform float param_HDR_gamma;\n" +
//            "uniform float param_HDR_graph;\n" +
//            "\n" +
//            " vec3 gamma(float value,  vec3 color) {\n" +
//            "   float p = 0.8 * (value - 1.) + 1.0;\n" +
//            "   float a = (1. - p) / 0.5;\n" +
//            "   float b = (p - 0.5) / 0.5;\n" +
//            "   return clamp(a * color * color + b * color , 0., 1.);\n" +
//            "}\n" +
//            "\n" +
//            "float adjusted(float graphStrength, float color) {\n" +
//            "   float valf = clamp((1. - color) * 2., 0. , 2.);\n" +
//            "   float mult = clamp(pow(abs(valf - 1.), graphStrength * 10.), 0., 1.);\n" +
//            "   return clamp(((valf - 1.) * mult + 1.), 0., 2.);\n" +
//            "}\n" +
//            "\n" +
//            "float overdrive(float strength, float color) {\n" +
//            "   return clamp((color - 1.) * strength + 1., 0., 2.);\n" +
//            "}\n" +
//            "\n" +
//            "float brightness(float strength, float color) {\n" +
//            "   float t = strength;\n" +
//            "   if (t <= 1.0) {\n" +
//            "      return color * t;\n" +
//            "   } else {\n" +
//            "      return color * (2. - t) + 1. * (t - 1.);\n" +
//            "   }\n" +
//            "}\n" +
//            "\n" +
//            "float average(vec3 color) {\n" +
//            "   return (color.x + color.y + color.z) / 3.;\n" +
//            "}\n" +
//            "\n" +
//            "void main() {\n" +
//            "   \n" +
//            "    vec3 origColor = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
//            "    vec3 blurColor = texture2D(inputImageTexture2, textureCoordinate2).rgb;\n" +
//            "   \n" +
//            "   float avg = average(blurColor);\n" +
//            "   avg = brightness(param_HDR_gamma * 2., avg);\n" +
//            "   avg = adjusted(param_HDR_graph, avg);\n" +
//            "   avg = overdrive(param_HDR_effect_strength * 8., avg);\n" +
//            "   gl_FragColor = vec4(gamma(avg, origColor), 1.);\n" +
////            "   gl_FragColor = texture2D(inputImageTexture2, textureCoordinate2);\n" +
//            "}";

    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;
    private final FloatBuffer mGLTextureFlipBuffer;
    private FloatBuffer mGLTextureScaleBuffer;

    public int mFilterSecondTextureCoordinateAttribute;
    public int mFilterInputTextureUniform2;
    public int mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;

    private GPUImageFilter mNoFilter;
    private GPUImageFilter mVerticalFilter;
    private GPUImageFilter mHorizontalFilter;

    private float mBlurScale = 2.5f;

    private float mEffectStrength = 0.2f;
    private float mGamma = 0.54f;
    private float mGraph = 0.2f;

    public GPUImageHDRFilter() {
        super(GPUImageHDROESFilter.VERTEX_SHADER_T, GPUImageHDROESFilter.FRAGMENT_SHADE_T);
        mNoFilter = new GPUImageFilter(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        mVerticalFilter = new GPUImageFilter(NO_FILTER_VERTEX_SHADER, GPUImageHDROESFilter.FRAGMENT_SHADER_V);
        mHorizontalFilter = new GPUImageFilter(NO_FILTER_VERTEX_SHADER, GPUImageHDROESFilter.FRAGMENT_SHADER_H);
        mGLCubeBuffer = ByteBuffer.allocateDirect(GPUImageRenderer.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(GPUImageRenderer.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        float[] flipTexture = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);
        mGLTextureFlipBuffer = ByteBuffer.allocateDirect(flipTexture.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureFlipBuffer.put(flipTexture).position(0);
    }

    @Override
    public void onInit() {
        super.onInit();
        mFilterSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
        mFilterInputTextureUniform2 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);

        mNoFilter.onInit();
        mVerticalFilter.onInit();
        mHorizontalFilter.onInit();
    }

    protected void initTexelOffsets() {
        int texelWidthOffsetLocation = GLES20.glGetUniformLocation(mVerticalFilter.getProgram(), "param_BoxBlur_texel_width");
        int blurSacleLocation = GLES20.glGetUniformLocation(mVerticalFilter.getProgram(), "param_BoxBlur_scale");
        mVerticalFilter.setFloat(texelWidthOffsetLocation, 1 / mOutputWidth);
        mVerticalFilter.setFloat(blurSacleLocation, mBlurScale);

        int texelHeightOffsetLocation = GLES20.glGetUniformLocation(mHorizontalFilter.getProgram(), "param_BoxBlur_texel_height");
        blurSacleLocation = GLES20.glGetUniformLocation(mHorizontalFilter.getProgram(), "param_BoxBlur_scale");
        mHorizontalFilter.setFloat(texelHeightOffsetLocation, 1 / mOutputHeight);
        mHorizontalFilter.setFloat(blurSacleLocation, mBlurScale);

        int effectStrengthLocation = GLES20.glGetUniformLocation(getProgram(), "param_HDR_effect_strength");
        int gammaLocation = GLES20.glGetUniformLocation(getProgram(), "param_HDR_gamma");
        int graphLocation = GLES20.glGetUniformLocation(getProgram(), "param_HDR_graph");
        setFloat(effectStrengthLocation, mEffectStrength);
        setFloat(gammaLocation, mGamma);
        setFloat(graphLocation, mGraph);
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        initTexelOffsets();
        destroyFramebuffers();
        mNoFilter.onOutputSizeChanged(width, height);
        mVerticalFilter.onOutputSizeChanged(width, height);
        mHorizontalFilter.onOutputSizeChanged(width, height);

        mFrameBuffers = new int[4];
        mFrameBufferTextures = new int[4];

        // FrameBuffer1
        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, (int) (width), (int) (height), 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        // FrameBuffer2
        GLES20.glGenFramebuffers(1, mFrameBuffers, 1);
        GLES20.glGenTextures(1, mFrameBufferTextures, 1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[1]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, (int) (width), (int) (height), 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[1], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        // FrameBuffer3
        GLES20.glGenFramebuffers(1, mFrameBuffers, 2);
        GLES20.glGenTextures(1, mFrameBufferTextures, 2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[2]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, (int) (width), (int) (height), 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[2]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[2], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        // FrameBuffer4
        GLES20.glGenFramebuffers(1, mFrameBuffers, 3);
        GLES20.glGenTextures(1, mFrameBufferTextures, 3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[3]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, (int) (width), (int) (height), 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[3]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[3], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        float[] t = {
                0f, 1f - 1f / mBlurScale,
                1f / mBlurScale / mBlurScale, 1f - 1f / mBlurScale,
                0f, 1f - 1f / mBlurScale + 1f / mBlurScale / mBlurScale,
                1f / mBlurScale / mBlurScale, 1f - 1f / mBlurScale + 1f / mBlurScale / mBlurScale,
        };

        mGLTextureScaleBuffer = ByteBuffer.allocateDirect(t.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureScaleBuffer.put(t).position(0);
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterSourceTexture2);
        GLES20.glUniform1i(mFilterInputTextureUniform2, 3);

        GLES20.glVertexAttribPointer(mFilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mGLTextureScaleBuffer);
    }

    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return;
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glClearColor(0, 0, 0, 0);
        mNoFilter.onDraw(textureId, cubeBuffer, textureBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glClearColor(0, 0, 0, 0);
        mVerticalFilter.onDraw(mFrameBufferTextures[0], mGLCubeBuffer, mGLTextureBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[2]);
        GLES20.glClearColor(0, 0, 0, 0);
        mHorizontalFilter.onDraw(mFrameBufferTextures[1], mGLCubeBuffer, mGLTextureBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        mFilterSourceTexture2 = mFrameBufferTextures[2];
        super.onDraw(textureId, cubeBuffer, textureBuffer);
    }

    @Override
    public void onDestroy() {
        destroyFramebuffers();
        super.onDestroy();
    }

    private void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }
}
