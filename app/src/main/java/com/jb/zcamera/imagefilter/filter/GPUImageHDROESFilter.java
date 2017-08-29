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
import com.jb.zcamera.imagefilter.util.ImageFilterTools;
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
public class GPUImageHDROESFilter extends GPUImageFilter {
	public static final String FRAGMENT_SHADER_V = ImageFilterTools.getDecryptString("bmx7fXdtd3FwPnN7endrc24+eHJxf2olFG5se313bXdxcD5ze3p3a3NuPndwaiUUFGtwd3hxbHM+bX9zbnJ7bCxaPndwbmtqV3N/eXtKe2Zqa2x7JRQUaH9sZ3dweT5oe30sPmp7ZmprbHtdcXFsendwf2p7JRQUa3B3eHFscz54cnF/aj5uf2x/c0FccWZccmtsQWp7ZntyQWl3emp2JRRrcHd4cWxzPnhycX9qPm5/bH9zQVxxZlxya2xBbX1/cnslFBRocXd6PnN/d3A2Nz5lFBQ+PnhycX9qPm13cHlye1F4eG17aj4jPm5/bH9zQVxxZlxya2xBantme3JBaXd6anY+ND5uf2x/c0FccWZccmtsQW19f3J7JRQ+PnhycX9qPm13cHlye11xcWx6PiM+antmamtse11xcWx6d3B/answZj40Pm5/bH9zQVxxZlxya2xBbX1/cnslFD4+eHJxf2o+cHtpUXh4bXtqPiM+LjAlFD4UPj4+aHt9LT54bH95c3twal1xcnFsPiM+antmamtseyxaNndwbmtqV3N/eXtKe2Zqa2x7Mj5oe30sNm13cHlye11xcWx6Mj5qe2Zqa2x7XXFxbHp3cH9qezBnPjQ+bn9sf3NBXHFmXHJrbEFtfX9yezc3MGx5fCUUPj53cGo+dz4jPi4lFD4+eHFsNnclPnc+Ij4pJT53NTU3PmUUPj4+eGx/eXN7cGpdcXJxbD41Iz5qe2Zqa2x7LFo2d3Bua2pXc395e0p7ZmprbHsyPmh7fSw2c39mNm13cHlye11xcWx6PjM+cHtpUXh4bXtqMj4uMDcyPmp7ZmprbHtdcXFsendwf2p7MGc+ND5uf2x/c0FccWZccmtsQW19f3J7NzcwbHl8JRQ+Pj54bH95c3twal1xcnFsPjUjPmp7ZmprbHssWjZ3cG5raldzf3l7SntmamtsezI+aHt9LDZzd3A2bXdweXJ7XXFxbHo+NT5we2lReHhte2oyPi4wJyc3Mj5qe2Zqa2x7XXFxbHp3cH9qezBnPjQ+bn9sf3NBXHFmXHJrbEFtfX9yezc3MGx5fCUUPj4+cHtpUXh4bXtqPjUjPm13cHlye1F4eG17aiUUPj5jFD4+eXJBWGx/eV1xcnFsPiM+aHt9KjZ4bH95c3twal1xcnFsPjE+LyswMj4vMC43JRRj");

// 	  public static final String FRAGMENT_SHADER_V = "precision mediump float;\n" +
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

	public static final String FRAGMENT_SHADER_H = ImageFilterTools.getDecryptString("bmx7fXdtd3FwPnN7endrc24+eHJxf2olFG5se313bXdxcD5ze3p3a3NuPndwaiUUFGtwd3hxbHM+bX9zbnJ7bCxaPndwbmtqV3N/eXtKe2Zqa2x7JRQUaH9sZ3dweT5oe30sPmp7ZmprbHtdcXFsendwf2p7JRQUa3B3eHFscz54cnF/aj5uf2x/c0FccWZccmtsQWp7ZntyQXZ7d3l2aiUUa3B3eHFscz54cnF/aj5uf2x/c0FccWZccmtsQW19f3J7JRQUaHF3ej5zf3dwNjc+ZRQUPj54cnF/aj5td3B5cntReHhte2o+Iz5uf2x/c0FccWZccmtsQWp7ZntyQXZ7d3l2aj40Pm5/bH9zQVxxZlxya2xBbX1/cnslFD4+eHJxf2o+bXdweXJ7XXFxbHo+Iz5qe2Zqa2x7XXFxbHp3cH9qezBnPjQ+bn9sf3NBXHFmXHJrbEFtfX9yeyUUPj54cnF/aj5we2lReHhte2o+Iz4uMCUUPj4+FD4+aHt9LT54bH95c3twal1xcnFsPiM+antmamtseyxaNndwbmtqV3N/eXtKe2Zqa2x7Mj5oe30sNmp7ZmprbHtdcXFsendwf2p7MGY+ND5uf2x/c0FccWZccmtsQW19f3J7Mj5td3B5cntdcXFsejc3MGx5fCUUPndwaj53PiM+LiUUPnhxbDZ3JT53PiI+KSU+dzU1Nz5lFD4+eGx/eXN7cGpdcXJxbD41Iz5qe2Zqa2x7LFo2d3Bua2pXc395e0p7ZmprbHsyPmh7fSw2antmamtse11xcWx6d3B/answZj40Pm5/bH9zQVxxZlxya2xBbX1/cnsyPnN/ZjZtd3B5cntdcXFsej4zPnB7aVF4eG17ajI+LjA3NzcwbHl8JRQ+Pnhsf3lze3BqXXFycWw+NSM+antmamtseyxaNndwbmtqV3N/eXtKe2Zqa2x7Mj5oe30sNmp7ZmprbHtdcXFsendwf2p7MGY+ND5uf2x/c0FccWZccmtsQW19f3J7Mj5zd3A2bXdweXJ7XXFxbHo+NT5we2lReHhte2oyPi4wJyc3NzcwbHl8JRQ+PnB7aVF4eG17aj41Iz5td3B5cntReHhte2olFD5jFD55ckFYbH95XXFycWw+Iz5oe30qNnhsf3lze3BqXXFycWw+MT4vKzAyPi8wLjclFGM=");

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
//            "}";

    public static final String VERTEX_SHADER_T = "attribute vec4 position;\n" +
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
    public static final String FRAGMENT_SHADE_T = ImageFilterTools.getDecryptString("bmx7fXdtd3FwPnN7endrc24+eHJxf2olFBRrcHd4cWxzPm1/c25ye2wsWj53cG5raldzf3l7SntmamtseyUUa3B3eHFscz5tf3NucntsLFo+d3Bua2pXc395e0p7ZmprbHssJRQUaH9sZ3dweT5oe30sPmp7ZmprbHtdcXFsendwf2p7JRRof2xnd3B5Pmh7fSw+antmamtse11xcWx6d3B/anssJRQUa3B3eHFscz54cnF/aj5uf2x/c0FWWkxBe3h4e31qQW1qbHtweWp2JRRrcHd4cWxzPnhycX9qPm5/bH9zQVZaTEF5f3NzfyUUa3B3eHFscz54cnF/aj5uf2x/c0FWWkxBeWx/bnYlFBQ+aHt9LT55f3NzfzZ4cnF/aj5of3JrezI+Pmh7fS0+fXFycWw3PmUUPj4+eHJxf2o+bj4jPi4wJj40PjZof3Jrez4zPi8wNz41Pi8wLiUUPj4+eHJxf2o+fz4jPjYvMD4zPm43PjE+LjArJRQ+Pj54cnF/aj58PiM+Nm4+Mz4uMCs3PjE+LjArJRQ+Pj5se2prbHA+fXJ/c242fz40Pn1xcnFsPjQ+fXFycWw+NT58PjQ+fXFycWw+Mj4uMDI+LzA3JRRjFBR4cnF/aj5/enRrbWp7ejZ4cnF/aj55bH9udk1qbHtweWp2Mj54cnF/aj59cXJxbDc+ZRQ+Pj54cnF/aj5of3J4PiM+fXJ/c242Ni8wPjM+fXFycWw3PjQ+LDAyPi4wPjI+LDA3JRQ+Pj54cnF/aj5za3JqPiM+fXJ/c242bnFpNn98bTZof3J4PjM+LzA3Mj55bH9udk1qbHtweWp2PjQ+Ly4wNzI+LjAyPi8wNyUUPj4+bHtqa2xwPn1yf3NuNjY2aH9yeD4zPi8wNz40PnNrcmo+NT4vMDcyPi4wMj4sMDclFGMUFHhycX9qPnFoe2x6bHdoezZ4cnF/aj5tamx7cHlqdjI+eHJxf2o+fXFycWw3PmUUPj4+bHtqa2xwPn1yf3NuNjZ9cXJxbD4zPi8wNz40Pm1qbHtweWp2PjU+LzAyPi4wMj4sMDclFGMUFHhycX9qPnxsd3l2anB7bW02eHJxf2o+bWpse3B5anYyPnhycX9qPn1xcnFsNz5lFD4+PnhycX9qPmo+Iz5tamx7cHlqdiUUPj4+d3g+Nmo+IiM+LzAuNz5lFD4+Pj4+Pmx7amtscD59cXJxbD40PmolFD4+PmM+e3Jtez5lFD4+Pj4+Pmx7amtscD59cXJxbD40PjYsMD4zPmo3PjU+LzA+ND42aj4zPi8wNyUUPj4+YxRjFBR4cnF/aj5/aHtsf3l7Nmh7fS0+fXFycWw3PmUUPj4+bHtqa2xwPjZ9cXJxbDBmPjU+fXFycWwwZz41Pn1xcnFsMGQ3PjE+LTAlFGMUFGhxd3o+c393cDY3PmUUPj4+FD4+Pj5oe30tPnFsd3ldcXJxbD4jPmp7ZmprbHssWjZ3cG5raldzf3l7SntmamtsezI+antmamtse11xcWx6d3B/ans3MGx5fCUUPj4+Pmh7fS0+fHJrbF1xcnFsPiM+antmamtseyxaNndwbmtqV3N/eXtKe2Zqa2x7LDI+antmamtse11xcWx6d3B/anssNzBseXwlFD4+PhQ+Pj54cnF/aj5/aHk+Iz5/aHtsf3l7Nnxya2xdcXJxbDclFD4+Pn9oeT4jPnxsd3l2anB7bW02bn9sf3NBVlpMQXl/c3N/PjQ+LDAyPn9oeTclFD4+Pn9oeT4jPn96dGttant6Nm5/bH9zQVZaTEF5bH9udjI+f2h5NyUUPj4+f2h5PiM+cWh7bHpsd2h7Nm5/bH9zQVZaTEF7eHh7fWpBbWpse3B5anY+ND4mMDI+f2h5NyUUPj4+eXJBWGx/eV1xcnFsPiM+aHt9KjZ5f3NzfzZ/aHkyPnFsd3ldcXJxbDcyPi8wNyUUYw==");
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

    public GPUImageHDROESFilter() {
        super(VERTEX_SHADER_T, FRAGMENT_SHADE_T);
        mNoFilter = new GPUImageOESFilter();
        mVerticalFilter = new GPUImageFilter(NO_FILTER_VERTEX_SHADER, FRAGMENT_SHADER_V);
        mHorizontalFilter = new GPUImageFilter(NO_FILTER_VERTEX_SHADER, FRAGMENT_SHADER_H);
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
        super.onDraw(mFrameBufferTextures[0], mGLCubeBuffer, mGLTextureFlipBuffer);
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
