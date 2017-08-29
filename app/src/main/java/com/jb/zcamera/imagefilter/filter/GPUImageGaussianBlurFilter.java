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

import java.util.Locale;

/**
 * A more generalized 9x9 Gaussian blur filter
 * blurSize value ranging from 0.0 on up, with a default of 1.0
 */
public class GPUImageGaussianBlurFilter extends GPUImageTwoPassTextureSamplingFilter {
    public static final String VERTEX_SHADER = vertexShaderForOptimizedBlurOfRadius(4, 2.0f);

    public static final String FRAGMENT_SHADER = fragmentShaderForOptimizedBlurOfRadius(4, 2.0f);

    protected float mBlurSize = 1f;

    public GPUImageGaussianBlurFilter() {
        this(1f);
    }

    public GPUImageGaussianBlurFilter(float blurSize) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
        mBlurSize = blurSize;
    }

    @Override
    public float getVerticalTexelOffsetRatio() {
        return mBlurSize;
    }

    @Override
    public float getHorizontalTexelOffsetRatio() {
        return mBlurSize;
    }

    /**
     * A multiplier for the blur size, ranging from 0.0 on up, with a default of 1.0
     *
     * @param blurSize from 0.0 on up, default 1.0
     */
    public void setBlurSize(float blurSize) {
        mBlurSize = blurSize;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }

    private static String vertexShaderForOptimizedBlurOfRadius(int blurRadius, float sigma) {
        // First, generate the normal Gaussian weights for a given sigma
        float[] standardGaussianWeights = new float[blurRadius + 1];
        float sumOfWeights = 0.0f;
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = (float)((1.0 / Math.sqrt(2.0 * Math.PI * Math.pow(sigma, 2.0))) * Math.exp(-Math.pow(currentGaussianWeightIndex, 2.0) / (2.0 * Math.pow(sigma, 2.0))));

            if (currentGaussianWeightIndex == 0)
            {
                sumOfWeights += standardGaussianWeights[currentGaussianWeightIndex];
            }
            else
            {
                sumOfWeights += 2.0 * standardGaussianWeights[currentGaussianWeightIndex];
            }
        }

        // Next, normalize these weights to prevent the clipping of the Gaussian curve at the end of the discrete samples from reducing luminance
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = standardGaussianWeights[currentGaussianWeightIndex] / sumOfWeights;
        }

        // From these weights we calculate the offsets to read interpolated values from
        int numberOfOptimizedOffsets = Math.min(blurRadius / 2 + (blurRadius % 2), 7);
        float[] optimizedGaussianOffsets = new float[numberOfOptimizedOffsets];

        for (int currentOptimizedOffset = 0; currentOptimizedOffset < numberOfOptimizedOffsets; currentOptimizedOffset++)
        {
            float firstWeight = standardGaussianWeights[currentOptimizedOffset*2 + 1];
            float secondWeight = standardGaussianWeights[currentOptimizedOffset*2 + 2];

            float optimizedWeight = firstWeight + secondWeight;

            optimizedGaussianOffsets[currentOptimizedOffset] = (firstWeight * (currentOptimizedOffset*2 + 1) + secondWeight * (currentOptimizedOffset*2 + 2)) / optimizedWeight;
        }

        StringBuilder shaderString = new StringBuilder();
        // Header
        shaderString.append(String.format(Locale.US,
                "     attribute vec4 position;\n" +
                        "     attribute vec4 inputTextureCoordinate;\n" +
                        "     \n" +
                        "     uniform float texelWidthOffset;\n" +
                        "     uniform float texelHeightOffset;\n" +
                        "     \n" +
                        "     varying vec2 blurCoordinates[%1$d];\n" +
                        "     \n" +
                        "     void main()\n" +
                        "     {\n" +
                        "        gl_Position = position;\n" +
                        "        \n" +
                        "        vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n", (1 + (numberOfOptimizedOffsets * 2))));

        // Inner offset loop
        shaderString.append("blurCoordinates[0] = inputTextureCoordinate.xy;\n");
        for (int currentOptimizedOffset = 0; currentOptimizedOffset < numberOfOptimizedOffsets; currentOptimizedOffset++)
        {
            shaderString.append(String.format(Locale.US, "" +
                    "blurCoordinates[%1$d] = inputTextureCoordinate.xy + singleStepOffset * %2$f;\n" +
                    "blurCoordinates[%3$d] = inputTextureCoordinate.xy - singleStepOffset * %4$f;\n", ((currentOptimizedOffset * 2) + 1), optimizedGaussianOffsets[currentOptimizedOffset], ((currentOptimizedOffset * 2) + 2), optimizedGaussianOffsets[currentOptimizedOffset]));
        }

        // Footer
        shaderString.append(String.format(Locale.US, "}\n"));
        return shaderString.toString();
    }

    private static String fragmentShaderForOptimizedBlurOfRadius(int blurRadius, float sigma) {
        // First, generate the normal Gaussian weights for a given sigma
        float[] standardGaussianWeights = new float[blurRadius + 1];
        float sumOfWeights = 0.0f;
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = (float)((1.0 / Math.sqrt(2.0 * Math.PI * Math.pow(sigma, 2.0))) * Math.exp(-Math.pow(currentGaussianWeightIndex, 2.0) / (2.0 * Math.pow(sigma, 2.0))));

            if (currentGaussianWeightIndex == 0)
            {
                sumOfWeights += standardGaussianWeights[currentGaussianWeightIndex];
            }
            else
            {
                sumOfWeights += 2.0f * standardGaussianWeights[currentGaussianWeightIndex];
            }
        }

        // Next, normalize these weights to prevent the clipping of the Gaussian curve at the end of the discrete samples from reducing luminance
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = standardGaussianWeights[currentGaussianWeightIndex] / sumOfWeights;
        }

        // From these weights we calculate the offsets to read interpolated values from
        int numberOfOptimizedOffsets = Math.min(blurRadius / 2 + (blurRadius % 2), 7);
        int trueNumberOfOptimizedOffsets = blurRadius / 2 + (blurRadius % 2);

        StringBuilder shaderString = new StringBuilder();
        shaderString.append(String.format(Locale.US, "" +
                "     uniform sampler2D inputImageTexture;\n" +
                "     uniform highp float texelWidthOffset;\n" +
                "     uniform highp float texelHeightOffset;\n" +
                "     \n" +
                "     varying highp vec2 blurCoordinates[%1$d];\n" +
                "     \n" +
                "     void main()\n" +
                "     {\n" +
                "        lowp vec4 sum = vec4(0.0);\n", (1 + (numberOfOptimizedOffsets * 2))));
        // Inner texture loop
        shaderString.append(String.format(Locale.US, "sum += texture2D(inputImageTexture, blurCoordinates[0]) * %1$f;\n", standardGaussianWeights[0]));

        for (int currentBlurCoordinateIndex = 0; currentBlurCoordinateIndex < numberOfOptimizedOffsets; currentBlurCoordinateIndex++)
        {
            float firstWeight = standardGaussianWeights[currentBlurCoordinateIndex * 2 + 1];
            float secondWeight = standardGaussianWeights[currentBlurCoordinateIndex * 2 + 2];
            float optimizedWeight = firstWeight + secondWeight;

            shaderString.append(String.format(Locale.US, "sum += texture2D(inputImageTexture, blurCoordinates[%1$d]) * %2$f;\n", ((currentBlurCoordinateIndex * 2) + 1), optimizedWeight));
            shaderString.append(String.format(Locale.US, "sum += texture2D(inputImageTexture, blurCoordinates[%1$d]) * %2$f;\n", ((currentBlurCoordinateIndex * 2) + 2), optimizedWeight));
        }

        // If the number of required samples exceeds the amount we can pass in via varyings, we have to do dependent texture reads in the fragment shader
        if (trueNumberOfOptimizedOffsets > numberOfOptimizedOffsets)
        {
            shaderString.append(String.format(Locale.US, "highp vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n"));

            for (int currentOverlowTextureRead = numberOfOptimizedOffsets; currentOverlowTextureRead < trueNumberOfOptimizedOffsets; currentOverlowTextureRead++)
            {
                float firstWeight = standardGaussianWeights[currentOverlowTextureRead * 2 + 1];
                float secondWeight = standardGaussianWeights[currentOverlowTextureRead * 2 + 2];

                float optimizedWeight = firstWeight + secondWeight;
                float optimizedOffset = (firstWeight * (currentOverlowTextureRead * 2 + 1) + secondWeight * (currentOverlowTextureRead * 2 + 2)) / optimizedWeight;

                shaderString.append(String.format(Locale.US, "sum += texture2D(inputImageTexture, blurCoordinates[0] + singleStepOffset * %1$f) * %2$f;\n", optimizedOffset, optimizedWeight));
                shaderString.append(String.format(Locale.US, "sum += texture2D(inputImageTexture, blurCoordinates[0] - singleStepOffset * %1$f) * %2$f;\n", optimizedOffset, optimizedWeight));
            }
        }

        // Footer
        shaderString.append(String.format(Locale.US, "gl_FragColor = sum;\n}\n"));
        return shaderString.toString();
    }
}
