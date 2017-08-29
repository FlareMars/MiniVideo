package com.jb.zcamera.imagefilter.filter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;

import com.gomo.minivideo.R;
import com.jb.zcamera.imagefilter.util.ImageFilterTools;

public class DawnFilter extends GPUImageMultiInputFilter {

	public DawnFilter(Context context) {
		super(VERTEX_SHADER, FRAG, "a", "b", "c", "d", "e");
		Resources resources = context.getResources();
		setBitmap(1, BitmapFactory.decodeResource(resources, R.drawable.dawn3));
		setBitmap(2, BitmapFactory.decodeResource(resources, R.drawable.dawn2));
		setBitmap(3, BitmapFactory.decodeResource(resources, R.drawable.dawn5));
		setBitmap(4, BitmapFactory.decodeResource(resources, R.drawable.dawn4));
		setBitmap(5, BitmapFactory.decodeResource(resources, R.drawable.dawn1));
	}

	private final static String FRAG =
			"precision highp float;\n" +
					"\n" +
					"varying vec2 textureCoordinate;\n" +
					"varying vec2 textureCoordinate2;\n" +
					"\n" +
					"uniform sampler2D inputImageTexture;\n" +
					"uniform float intensity;\n" +
					"\n" +
					"\n" +
					"uniform sampler2D a;\n" +
					"uniform sampler2D b;\n" +
					"uniform sampler2D c;\n" +
					"uniform sampler2D d;\n" +
					"uniform sampler2D e;\n" +
					"\n" +
					"vec4 filter(vec4 color)\n" +
					"{\n" +
					"    vec3 tl = color.rgb;\n" +
					"\n" +
					"      vec3 ee = texture2D(c, textureCoordinate2).rgb;\n" +
					"      tl = tl * ee;\n" +
					"\n" +
					"    tl = vec3(\n" +
					"        texture2D(a, vec2(tl.r, .16666)).r,\n" +
					"        texture2D(a, vec2(tl.g, .5)).g,\n" +
					"        texture2D(a, vec2(tl.b, .83333)).b);\n" +
					"\n" +
					"    vec3 la = vec3(.30, .59, .11);\n" +
					"    vec3 ge = texture2D(d, vec2(dot(la, tl), .5)).rgb;\n" +
					"    vec3 fl = vec3(\n" +
					"        texture2D(e, vec2(ge.r, tl.r)).r,\n" +
					"        texture2D(e, vec2(ge.g, tl.g)).g,\n" +
					"        texture2D(e, vec2(ge.b, tl.b)).b\n" +
					"    );\n" +
					"\n" +
					"    vec3 be = texture2D(b, textureCoordinate2).rgb;\n" +
					"    vec3 bed = vec3(\n" +
					"        texture2D(e, vec2(be.r, fl.r)).r,\n" +
					"        texture2D(e, vec2(be.g, fl.g)).g,\n" +
					"        texture2D(e, vec2(be.b, fl.b)).b\n" +
					"        );\n" +
					"\n" +
					"    return vec4(bed, 1.0);\n" +
					"}\n" +
					"void main()\n" +
					"{\n" +
					"    vec4 color = texture2D(inputImageTexture, textureCoordinate);\n" +
					"    gl_FragColor = mix(color, filter(color), intensity);\n" +
					"}\n";
	private final static String FRAG_STRING = "PXp7eHdwez5IV1pbUUFTUVpbPnh/cm17FG5se313bXdxcD52d3l2bj54cnF/aiUUFGh/bGd3cHk+aHt9LD5qe2Zqa2x7XXFxbHp3cH9qeyUUaH9sZ3dweT5oe30sPmp7ZmprbHtdcXFsendwf2p7LCUUFGtwd3hxbHM+bX9zbnJ7bCxaPndwbmtqV3N/eXtKe2Zqa2x7JRRrcHd4cWxzPnhycX9qPndwantwbXdqZyUUFGtwd3hxbHM+bX9zbnJ7bCxaPn8lFGtwd3hxbHM+bX9zbnJ7bCxaPnwlFGtwd3hxbHM+bX9zbnJ7bCxaPn0lFGtwd3hxbHM+bX9zbnJ7bCxaPnolFGtwd3hxbHM+bX9zbnJ7bCxaPnslFBR9cXBtaj5zf2otPm17PiM+c39qLTYUPj4+Pj4vMCwvLi0uLjI+My4wLiYnKS4uMj4zLjAuJy8uLi4yFD4+Pj4zLjAvKSgvLi4yPj4vMC8sLScuLjI+My4wLykpKi4uMhQ+Pj4+My4wLi0qLC4uMj4zLjAuLSosLi4yPj4vMCwoKyYuLjclFH1xcG1qPmh7fS0+ens+Iz5oe30tNjAtMj4wKycyPjAvLzclFBRoe30qPnh3cmp7bDZoe30qPn1xcnFsNz5lFD4+Pj59cXBtaj58cXFyPmh3entxU3F6ez4jPkhXWltRQVNRWlslFD4+Pj5oe30tPmpyPiM+fXFycWwwbHl8JRQ+Pj4+aHt9LD5taD4jPmh7fSw2antmamtse11xcWx6d3B/anssMGYyPi8wLj4zPmp7ZmprbHtdcXFsendwf2p7LDBnNyUUPj4+Pmh7fSw+cm4lFD4+Pj5ybjBnPiM+LjArJRQ+Pj4+cm4wZj4jPmpyMGwlFD4+Pj5qcjBsPiM+antmamtseyxaNnwyPnJuNzBsJRQ+Pj4+cm4wZj4jPmpyMHklFD4+Pj5qcjB5PiM+antmamtseyxaNnwyPnJuNzB5JRQ+Pj4+cm4wZj4jPmpyMHwlFD4+Pj5qcjB8PiM+antmamtseyxaNnwyPnJuNzB8JRQ+Pj4+aHt9LT5saj4jPmp7ZmprbHssWjZ6Mj5oe30sNnpxajZ6ezI+anI3Mj4uMCs3NzBseXwlFD4+Pj5qcj4jPm17PjQ+c3dmNmpyMj5sajI+MCs3JRQ+Pj4+aHt9LD5qfT4jPjYsMC4+ND5taDc+Mz4vMC4lFD4+Pj54cnF/aj56PiM+aHd6e3FTcXp7PiE+LjAqPiQ+enFqNmp9Mj5qfTclFD4+Pj5oe30tPm16JRQ+Pj4+cm4wZj4jPmpyMGwlFD4+Pj5tejBsPiM+antmamtseyxaNn0yPnJuNzBsJRQ+Pj4+cm4wZj4jPmpyMHklFD4+Pj5tejB5PiM+antmamtseyxaNn0yPnJuNzB5JRQ+Pj4+cm4wZj4jPmpyMHwlFD4+Pj5tejB8PiM+antmamtseyxaNn0yPnJuNzB8JRQ+Pj4+eHJxf2o+aHs+Iz5tc3FxanZtantuNi4wLjI+LzAsKzI+bnFpNnoyPi8wLSs3MS8wKCs3JRQ+Pj4+anI+Iz5zd2Y2anIyPm16Mj5oezclFD4+Pj5ybjBmPiM+anIwbCUUPj4+Pm16MGw+Iz5qe2Zqa2x7LFo2ezI+cm43MGwlFD4+Pj5ybjBmPiM+anIweSUUPj4+Pm16MHk+Iz5qe2Zqa2x7LFo2ezI+cm43MHklFD4+Pj5ybjBmPiM+anIwfCUUPj4+Pm16MHw+Iz5qe2Zqa2x7LFo2ezI+cm43MHwlFD4+Pj5qcj4jPnN3ZjZtejI+anIyPmh7NyUUPj4+PnJuMGY+Iz5qcjBsJRQ+Pj4+anIwbD4jPmp7ZmprbHssWjZ/Mj5ybjcwbCUUPj4+PnJuMGY+Iz5qcjB5JRQ+Pj4+anIweT4jPmp7ZmprbHssWjZ/Mj5ybjcweSUUPj4+PnJuMGY+Iz5qcjB8JRQ+Pj4+anIwfD4jPmp7ZmprbHssWjZ/Mj5ybjcwfCUUPj4+Pmh7fSo+eHI+Iz5oe30qNmpyMGx5fDI+LzAuNyUUPj4+Pmx7amtscD54ciUUYxRocXd6PnN/d3A2NxRlFD4+Pj5oe30qPn1xcnFsPiM+antmamtseyxaNndwbmtqV3N/eXtKe2Zqa2x7Mj5qe2Zqa2x7XXFxbHp3cH9qezclFD4+Pj55ckFYbH95XXFycWw+Iz5zd2Y2fXFycWwyPnh3cmp7bDZ9cXJxbDcyPndwantwbXdqZzclFGMU";
	
//	private final static String FRAG = ImageFilterTools.getDecryptString(FRAG_STRING);




}
