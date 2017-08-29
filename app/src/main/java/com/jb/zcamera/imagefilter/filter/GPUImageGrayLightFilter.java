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

public class GPUImageGrayLightFilter extends GPUImageFilter {
    public static final String SOFT_LIGHT_BLEND_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "\n" +
            " uniform sampler2D inputImageTexture;\n" +
            " const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "     mediump float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
            "     mediump vec3 greyScaleColor = vec3(luminance);\n" +
            "     \n" +
            "     mediump vec4 greyColor = vec4(mix(greyScaleColor, textureColor.rgb, 0.0), textureColor.w);\n" +
            "     highp float red = floor(((greyColor.r * 255.0) * 235.0 / 255.0 + 10.0) + 0.5) / 255.0;\n" +
//            "     highp float red1 = red * 0.92156863 + 10.0;\n" +
//            "     highp float red2 = round(red1);\n" +
//            "     if(red >= 0.0 && red <= 6.0){\n" +
//            "           red += 10.0;\n" +
//            "     } else if(red > 6.0 && red <= 19.0){\n" +
//            "           red += 9.0;\n" +
//            "     } else if(red > 19.0 && red <= 31.0){\n" +
//            "           red += 8.0;\n" +
//            "     } else if(red > 31.0 && red <= 44.0){\n" +
//            "           red += 7.0;\n" +
//            "     } else if(red > 44.0 && red <= 57.0){\n" +
//            "           red += 6.0;\n" +
//            "     } else if(red > 57.0 && red <= 70.0){\n" +
//            "           red += 5.0;\n" +
//            "     } else if(red > 70.0 && red <= 82.0){\n" +
//            "           red += 4.0;\n" +
//            "     } else if(red > 82.0 && red <= 95.0){\n" +
//            "           red += 3.0;\n" +
//            "     } else if(red > 95.0 && red <= 108.0){\n" +
//            "           red += 2.0;\n" +
//            "     } else if(red > 108.0 && red <= 121.0){\n" +
//            "           red += 1.0;\n" +
//            "     } else if(red > 121.0 && red <= 133.0){\n" +
//            "           red += 0.0;\n" +
//            "     } else if(red > 133.0 && red <= 146.0){\n" +
//            "           red -= 1.0;\n" +
//            "     } else if(red > 146.0 && red <= 159.0){\n" +
//            "           red -= 2.0;\n" +
//            "     } else if(red > 159.0 && red <= 172.0){\n" +
//            "           red -= 3.0;\n" +
//            "     } else if(red > 172.0 && red <= 184.0){\n" +
//            "           red -= 4.0;\n" +
//            "     } else if(red > 184.0 && red <= 197.0){\n" +
//            "           red -= 5.0;\n" +
//            "     } else if(red > 197.0 && red <= 210.0){\n" +
//            "           red -= 6.0;\n" +
//            "     } else if(red > 210.0 && red <= 223.0){\n" +
//            "           red -= 7.0;\n" +
//            "     } else if(red > 223.0 && red <= 235.0){\n" +
//            "           red -= 8.0;\n" +
//            "     } else if(red > 235.0 && red <= 248.0){\n" +
//            "           red -= 9.0;\n" +
//            "     } else if(red > 248.0 && red <= 255.0){\n" +
//            "           red -= 10.0;\n" +
//            "     } else{\n" +
//            "     }\n" +
//            "     red = red / 255.0;\n" +
            "     gl_FragColor = vec4(red, red, red, textureColor.a);\n" +
            " }";

    public GPUImageGrayLightFilter() {
        super(NO_FILTER_VERTEX_SHADER, SOFT_LIGHT_BLEND_FRAGMENT_SHADER);
    }
}
