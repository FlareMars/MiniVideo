package com.jb.zcamera.imagefilter.filter;

import android.opengl.GLES20;

public class GPUImageBilateralFilter extends GPUImageTwoPassTextureSamplingFilter {
	public static final String VERTEX_SHADER =
			"attribute vec4 position;\n"+
					"attribute vec4 inputTextureCoordinate;\n"+
					"const int GAUSSIAN_SAMPLES = 9;\n"+
					"uniform float texelWidthOffset;\n"+
					"uniform float texelHeightOffset;\n"+
					"varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n"+
					"void main()\n"+
					"{\n"+
					"gl_Position = position;\n"+
					"// Calculate the positions for the blur\n"+
					"int multiplier = 0;\n"+
					"vec2 blurStep;\n"+
					"vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n"+
					"for (int i = 0; i < GAUSSIAN_SAMPLES; i++)\n"+
					"{\n"+
					"multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));\n"+
					"// Blur in x (horizontal)\n"+
					"blurStep = float(multiplier) * singleStepOffset;\n"+
					"blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n"+
					"}\n"+
					"}\n";



    public static final String FRAGMENT_SHADER =
                    "#ifdef GL_FRAMGMENT_PRECISION_HIGH\n"+
                    "precision highp float;\n"+
                    "#else\n"+
                    "precision mediump float;\n"+
                    "#endif\n"+
                    "uniform sampler2D inputImageTexture;\n"+
    				"  \n"+
    				" const highp int GAUSSIAN_SAMPLES = 9;\n"+
    				"  \n"+
    				" varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n"+
    				"  \n"+
    				" uniform highp float distanceNormalizationFactor;\n"+
    				"  \n"+
    				" void main()\n"+
    				" {\n"+
    				" \n"+
    				"     vec4 centralColor;\n"+
    				"     float gaussianWeightTotal;\n"+
    				"     vec4 sum;\n"+
    				"     vec4 sampleColor;\n"+
    				"     float distanceFromCentralColor;\n"+
    				"     float gaussianWeight;\n"+
    				"      \n"+
    				"     centralColor = texture2D(inputImageTexture, blurCoordinates[4]);\n"+
    				"     gaussianWeightTotal = 0.18;\n"+
    				"     sum = centralColor * 0.18;\n"+
    				"      \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[0]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				" \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[1]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				" \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[2]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				" \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[3]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				" \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[5]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.15 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				" \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[6]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.12 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				" \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[7]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.09 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				" \n"+
    				"     sampleColor = texture2D(inputImageTexture, blurCoordinates[8]);\n"+
    				"     distanceFromCentralColor = min(distance(centralColor, sampleColor) * distanceNormalizationFactor, 1.0);\n"+
    				"     gaussianWeight = 0.05 * (1.0 - distanceFromCentralColor);\n"+
    				"     gaussianWeightTotal += gaussianWeight;\n"+
    				"     sum += sampleColor * gaussianWeight;\n"+
    				"      \n"+
    				"     gl_FragColor = vec4((sum / gaussianWeightTotal).rgb, centralColor.a);\n"+
    				"  \n"+
    				"}\n";


    protected float mBlurSize = 1.0f;
    protected float mDistanceNormalizationFactor = 8.0f;

    public GPUImageBilateralFilter() {
        this(1f);
    }

    public GPUImageBilateralFilter(float blurSize) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
//        super(VERTEX_SHADER, getFragmentShader(), VERTEX_SHADER, getFragmentShader());
        mBlurSize = blurSize;
    }
    
    @Override
    protected void initTexelOffsets() {
        super.initTexelOffsets();
        GPUImageFilter filter = mFilters.get(0);
        int distanceNormalizationFactorLocation = GLES20.glGetUniformLocation(filter.getProgram(),
                "distanceNormalizationFactor");
        filter.setFloat(distanceNormalizationFactorLocation, mDistanceNormalizationFactor);

        filter = mFilters.get(1);
        distanceNormalizationFactorLocation = GLES20.glGetUniformLocation(filter.getProgram(),
                "distanceNormalizationFactor");
        filter.setFloat(distanceNormalizationFactorLocation, mDistanceNormalizationFactor);
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
    
    public void setDistanceNormalizationFactor(float factor) {
        mDistanceNormalizationFactor = factor;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }
    
    private static String getFragmentShader() {
        String shader = 
                "#ifdef GL_FRAMGMENT_PRECISION_HIGH\n"+
                "precision highp float;\n"+
                "#else\n"+
                "precision mediump float;\n"+
                "#endif\n"+
                "uniform sampler2D inputImageTexture;\n" +
    "const float ds = 3.0;\n" +
    "const float rs = 10.0;\n" +
    "const float R = 1.0;\n" +
    "const int GAUSSIAN_SAMPLES = 9;\n" +
    "const float factor = -0.5;\n" +
    "varying vec2 textureCoordinate;\n" +
    "varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
    "uniform float distanceNormalizationFactor;\n" +
    "void main()\n" +
    "{\n" +
    "    vec4 centralColor;\n" +
    "    float gaussianWeightTotal;\n" +
    "    vec4 sum;\n" +
    "    vec4 sampleColor;\n" +
    "    float distanceFromCentralColor;\n" +
    "    float gaussianWeight;\n" +
    "    float delta;\n" +
    "    float distanceWeight;\n" +
    "    float x;\n" +
    "    float y;\n" +
    "    centralColor = texture2D(inputImageTexture, blurCoordinates[int(GAUSSIAN_SAMPLES/2)]);\n" +
    "    gaussianWeightTotal = 0.0;\n" +
    "    sum = vec4(0.0);\n" +
    "    for(int i = 0; i < GAUSSIAN_SAMPLES; i++) {\n" +
    "        sampleColor = texture2D(inputImageTexture, blurCoordinates[i]);\n" +
    "        delta = distance(centralColor, sampleColor) / rs;\n" +
    "        x =floor(mod(float(i), 2.0 * R)) - R;\n" +
    "        y = floor(float(i) / (2.0 * R)) - R;\n" +
    "        distanceWeight = sqrt(x * x + y * y)/ds;\n" +
    "        gaussianWeight = exp(distanceWeight * distanceWeight * factor) * exp(delta * delta * factor);\n" +
    "        gaussianWeightTotal += gaussianWeight;\n" +
    "        sum += sampleColor * gaussianWeight;\n" +
    "    }\n" +
    "    vec4 result = sum / gaussianWeightTotal;\n" +
    "    gl_FragColor = result;\n" +
    "}\n";
        return shader;
    }
}
