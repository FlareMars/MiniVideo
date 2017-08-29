
package com.jb.zcamera.camera;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * 获取触摸区域帮助类
 * 
 * @author oujingwen
 *
 */
public class TapAreaUtil {

    public static Rect calculateTapArea(int x, int y, float areaMultiple, int previewWidth,
                                        int previewHeight, Matrix matrix) {
        int areaSize = (int)(getAreaSize(previewWidth, previewHeight) * areaMultiple);
        int left = clamp(x - areaSize / 2, 0, previewWidth - areaSize);
        int top = clamp(y - areaSize / 2, 0, previewHeight - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        Rect rect = new Rect();
        matrix.mapRect(rectF);
        rectFToRect(rectF, rect);
        return rect;
    }

    public static int clamp(int x, int min, int max) {
        if (x > max)
            return max;
        if (x < min)
            return min;
        return x;
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    private static int getAreaSize(int previewWidth, int previewHeight) {
        // Recommended focus area size from the manufacture is 1/8 of the image
        // width (i.e. longer edge of the image)
        return Math.max(previewWidth, previewHeight) / 8;
    }
}
