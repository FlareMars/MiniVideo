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


public class GPUImageLookupFilter extends GPUImageTwoInputFilter {

    private float mIntentsity;
    private int mIntentsityLocation;

    public static final String LOOKUP_FRAGMENT_SHADER = "    precision mediump float;\n" +
            "    varying highp vec2 textureCoordinate;\n" +
            "    varying highp vec2 textureCoordinate2; // TODO: This is not used\n" +
            "\n" +
            "    uniform sampler2D inputImageTexture;\n" +
            "    uniform sampler2D inputImageTexture2; // lookup texture\n" +
            "    uniform lowp float intensity;\n" +
            "    void main()\n" +
            "    {\n" +
            "        mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "\n" +
            "        mediump float blueColor = textureColor.b * 63.0;\n" +
            "\n" +
            "        mediump vec2 quad1;\n" +
            "        quad1.y = floor(floor(blueColor) / 8.0);\n" +
            "        quad1.x = floor(blueColor) - (quad1.y * 8.0);\n" +
            "\n" +
            "        mediump vec2 quad2;\n" +
            "        quad2.y = floor(ceil(blueColor) / 8.0);\n" +
            "        quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n" +
            "        quad1 = clamp(quad1, vec2(0.0), vec2(7.0));\n" +
            "        quad2 = clamp(quad2, vec2(0.0), vec2(7.0));\n" +
            "\n" +
            "        highp vec2 texPos1;\n" +
            "        texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "        texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "\n" +
            "        highp vec2 texPos2;\n" +
            "        texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "        texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "\n" +
            "        mediump vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n" +
            "        mediump vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n" +
            "\n" +
            "        mediump vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n" +
            "        gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.a), intensity);\n" +
            "    }";

//    private static final String SHADER_STRING = "Pm5se313bXdxcD5ze3p3a3NuPnhycX9qJRQ+aH9sZ3dweT52d3l2bj5oe30sPmp7ZmprbHtdcXFsendwf2p7JRQ+aH9sZ3dweT52d3l2bj5oe30sPmp7ZmprbHtdcXFsendwf2p7LCU+MTE+SlFaUSQ+SnZ3bT53bT5wcWo+a217ehQ+FD5rcHd4cWxzPm1/c25ye2wsWj53cG5raldzf3l7SntmamtseyUUPmtwd3hxbHM+bX9zbnJ7bCxaPndwbmtqV3N/eXtKe2Zqa2x7LCU+MTE+cnFxdWtuPmp7ZmprbHsUPmtwd3hxbHM+cnFpbj54cnF/aj53cGp7cG13amclPhQ+aHF3ej5zf3dwNjcUPmUUPj4+Pj5ze3p3a3NuPmh7fSo+antmamtse11xcnFsPiM+antmamtseyxaNndwbmtqV3N/eXtKe2Zqa2x7Mj5qe2Zqa2x7XXFxbHp3cH9qezclFD4+Pj4+FD4+Pj4+c3t6d2tzbj54cnF/aj58cmt7XXFycWw+Iz5qe2Zqa2x7XXFycWwwfD40PigtMC4lFD4+Pj4+FD4+Pj4+c3t6d2tzbj5oe30sPm9rf3ovJRQ+Pj4+Pm9rf3ovMGc+Iz54cnFxbDZ4cnFxbDZ8cmt7XXFycWw3PjE+JjAuNyUUPj4+Pj5va396LzBmPiM+eHJxcWw2fHJre11xcnFsNz4zPjZva396LzBnPjQ+JjAuNyUUPj4+Pj4UPj4+Pj5ze3p3a3NuPmh7fSw+b2t/eiwlFD4+Pj4+b2t/eiwwZz4jPnhycXFsNn17d3I2fHJre11xcnFsNz4xPiYwLjclFD4+Pj4+b2t/eiwwZj4jPn17d3I2fHJre11xcnFsNz4zPjZva396LDBnPjQ+JjAuNyUUFz4+b2t/ei8+Iz59cn9zbjZva396LzI+aHt9LDYuMC43Mj5oe30sNikwLjc3JRQXPj5va396LD4jPn1yf3NuNm9rf3osMj5oe30sNi4wLjcyPmh7fSw2KTAuNzclFD4+Pj4+FD4+Pj4+dnd5dm4+aHt9LD5qe2ZOcW0vJRQ+Pj4+Pmp7Zk5xbS8wZj4jPjZva396LzBmPjQ+LjAvLCs3PjU+LjArMSsvLDAuPjU+NjYuMC8sKz4zPi8wLjErLywwLjc+ND5qe2Zqa2x7XXFycWwwbDclFD4+Pj4+antmTnFtLzBnPiM+Nm9rf3ovMGc+ND4uMC8sKzc+NT4uMCsxKy8sMC4+NT42Ni4wLywrPjM+LzAuMSsvLDAuNz40Pmp7ZmprbHtdcXJxbDB5NyUUPj4+Pj4UPj4+Pj52d3l2bj5oe30sPmp7Zk5xbSwlFD4+Pj4+antmTnFtLDBmPiM+Nm9rf3osMGY+ND4uMC8sKzc+NT4uMCsxKy8sMC4+NT42Ni4wLywrPjM+LzAuMSsvLDAuNz40Pmp7ZmprbHtdcXJxbDBsNyUUPj4+Pj5qe2ZOcW0sMGc+Iz42b2t/eiwwZz40Pi4wLywrNz41Pi4wKzErLywwLj41PjY2LjAvLCs+Mz4vMC4xKy8sMC43PjQ+antmamtse11xcnFsMHk3JRQ+Pj4+PhQ+Pj4+PnN7endrc24+aHt9Kj5we2ldcXJxbC8+Iz5qe2Zqa2x7LFo2d3Bua2pXc395e0p7ZmprbHssMj5qe2ZOcW0vNyUUPj4+Pj5ze3p3a3NuPmh7fSo+cHtpXXFycWwsPiM+antmamtseyxaNndwbmtqV3N/eXtKe2Zqa2x7LDI+antmTnFtLDclFD4+Pj4+FD4+Pj4+c3t6d2tzbj5oe30qPnB7aV1xcnFsPiM+c3dmNnB7aV1xcnFsLzI+cHtpXXFycWwsMj54bH99ajZ8cmt7XXFycWw3NyUUPj4+Pj55ckFYbH95XXFycWw+Iz5zd2Y2antmamtse11xcnFsMj5oe30qNnB7aV1xcnFsMGx5fDI+antmamtse11xcnFsMH83Mj53cGp7cG13amc3JRQ+Yw==";
//
//    public static final String LOOKUP_FRAGMENT_SHADER = ImageFilterTools.getDecryptString(SHADER_STRING);

    public GPUImageLookupFilter() {
        super(LOOKUP_FRAGMENT_SHADER);
        mIntentsity = 1;
    }
    
    @Override
    public void onInit() {
        super.onInit();
        mIntentsityLocation = GLES20.glGetUniformLocation(getProgram(), "intensity");
    }
    
    @Override
    public void onInitialized() {
        super.onInitialized();
        setIntensity(mIntentsity);
    }

    @Override
    public boolean isSupportIntensity() {
        return true;
    }

    @Override
    public float getIntensity() {
        return mIntentsity;
    }

    public void setIntensity(float intensity) {
        mIntentsity = intensity;
    	setFloat(mIntentsityLocation, intensity);
    }
}
