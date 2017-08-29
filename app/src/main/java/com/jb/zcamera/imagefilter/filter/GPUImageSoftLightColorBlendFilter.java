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

/**
 * float[] rgba
 * length为4
 * 里面的值都是0-1之间
 */
public class GPUImageSoftLightColorBlendFilter extends GPUImageFilter {
    public static final String SOFT_LIGHT_BLEND_FRAGMENT_SHADER = "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform mediump vec4 overlay;\n" +
//            " const highp vec4 overlay = vec4(1.0, 0, 0, 1.0);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     mediump vec4 base = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     \n" +
            "     gl_FragColor = base * (overlay.a * (base / base.a) + (2.0 * overlay * (1.0 - (base / base.a)))) + overlay * (1.0 - base.a) + base * (1.0 - overlay.a);\n" +
            " }";

    private float[] mOverlay;
    private int mOverlayLocation;

    public GPUImageSoftLightColorBlendFilter() {
        this(new float[]{1.0f, 0f, 0f, 1f});
    }

    public GPUImageSoftLightColorBlendFilter(float[] rgba) {
        super(NO_FILTER_VERTEX_SHADER, SOFT_LIGHT_BLEND_FRAGMENT_SHADER);
        this.mOverlay = rgba;
    }

    @Override
    public void onInit() {
        super.onInit();
        mOverlayLocation = GLES20.glGetUniformLocation(getProgram(), "overlay");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setOverlay(mOverlay);
    }

    public void setOverlay(float[] overlay) {
        this.mOverlay = overlay;
        setFloatVec4(mOverlayLocation, overlay);
    }
}
