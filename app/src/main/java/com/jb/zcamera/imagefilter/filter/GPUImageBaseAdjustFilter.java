package com.jb.zcamera.imagefilter.filter;

import android.opengl.GLES20;

/**
 * 
 * @author chenfangyi
 * 调整的基础Fiter
 * 只包含Contrast  Brightness  Saturation
 *
 */
public class GPUImageBaseAdjustFilter extends GPUImageFilter {
	
	public static final String BASE_ADJUST_FRAGMENT_SHADER = "" +
            " varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform mediump float contrast;\n" + 
            " uniform mediump float brightness;\n" +
            " uniform mediump float saturation;\n" +
            " \n" +
            " const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "    mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            " 	  \n" +
            "    mediump vec4 cColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);\n" + 
            " 	  \n" +
            "    mediump float luminance = dot(cColor.rgb, luminanceWeighting);\n" +
            "    mediump vec3 greyScaleColor = vec3(luminance);\n" +
            "    mediump vec4 sColor = vec4(mix(greyScaleColor, cColor.rgb, saturation), cColor.w);\n" +
            " 	  \n" +
            "    gl_FragColor = vec4((sColor.rgb + vec3(brightness)), sColor.w);\n" +
            "     \n" +
            " }";
	
	private int mContrastLocation;
    private float mContrast;
    
    private int mBrightnessLocation;
    private float mBrightness;
    
    private int mSaturationLocation;
    private float mSaturation;
    
    
    public GPUImageBaseAdjustFilter() {
        this(1.2f, 0.0f, 1.0f);
    }
    
    public GPUImageBaseAdjustFilter(float contrast, float brightness, float saturation) {
        super(NO_FILTER_VERTEX_SHADER, BASE_ADJUST_FRAGMENT_SHADER);
        mContrast = contrast;
        mBrightness = brightness;
        mSaturation = saturation;
    }
    
    @Override
    public void onInit() {
        super.onInit();
        mContrastLocation = GLES20.glGetUniformLocation(getProgram(), "contrast");
        mBrightnessLocation = GLES20.glGetUniformLocation(getProgram(), "brightness");
        mSaturationLocation = GLES20.glGetUniformLocation(getProgram(), "saturation");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setContrast(mContrast);
        setBrightness(mBrightness);
        setSaturation(mSaturation);
    }

    public void setContrast(final float contrast) {
        mContrast = contrast;
        setFloat(mContrastLocation, mContrast);
    }
    
    public void setBrightness(final float brightness) {
        mBrightness = brightness;
        setFloat(mBrightnessLocation, mBrightness);
    }
    
    public void setSaturation(final float saturation) {
        mSaturation = saturation;
        setFloat(mSaturationLocation, mSaturation);
    }
}
