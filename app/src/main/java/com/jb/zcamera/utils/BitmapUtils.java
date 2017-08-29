
package com.jb.zcamera.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import com.gomo.minivideo.CameraApp;
import com.jb.zcamera.camera.SettingsManager;
import com.jb.zcamera.exif.Exif;
import com.jb.zcamera.image.ImageHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Bitmap 处理工具类
 * 
 * @author oujingwen
 */
public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    private static final float WATER_MARK_REF_SIZE = 1080 * 1350;

    private static final float DATE_MARK_REF_SIZE = 978 * 1302;

    private static final float DEFAULT_DATE_MARK_TEXT_SIZE = 30;

    private static final int DEFAULT_DATE_MARK_TEXT_COLOR = 0xFFFFFFFF;

    private static final int DEFAULT_DATE_MARK_SHADER_COLOR = 0x66000000;

    private static final int DEFAULT_DATE_MARK_SHADER_RADIUS = 3;

    /**
     * 把图片裁剪为正方形
     * @param bitmap
     *
     * @return
     */
    public static Bitmap cropSquareBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == height) {
            return bitmap;
        }
        int length = Math.min(width, height);
        int x = (width - length) / 2;
        int y = (height - length) / 2;
        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, x, y, length, length);
        if (squareBitmap != null) {
            bitmap.recycle();
            bitmap = squareBitmap;
        }
        return bitmap;
    }

    /**
     * 裁剪图片
     *
     * @param bitmap
     * @param ratio
     * @return
     */
    public static Bitmap cropBitmap(Bitmap bitmap, float ratio) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Rect rect = cropRect(new Rect(0, 0, width, height), ratio);

        if (rect.width() == width && rect.height() == height) {
            return bitmap;
        }

        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
        if (squareBitmap != null) {
            bitmap.recycle();
            bitmap = squareBitmap;
        }
        return bitmap;
    }

    /**
     * 把矩形按比例最大裁剪
     * 
     * @param rect
     * @return
     */
    public static Rect cropRect(Rect rect, float ratio) {
        if (rect == null) {
            return null;
        }
        float width = rect.width();
        float height = rect.height();
        float originRatio = (float)width / height;
        if (originRatio == ratio) {
            return rect;
        }
        if (originRatio < ratio) {
            int maskHeight = (int) ((height - width / ratio) / 2f);
            return new Rect(rect.left, rect.top + maskHeight, rect.right, rect.bottom - maskHeight);
        } else {
            int maskWidth = (int) ((width - height * ratio) / 2f);
            return new Rect(rect.left + maskWidth, rect.top, rect.right - maskWidth, rect.bottom);
        }
    }

    /**
     * 解析Jpeg图片
     * 
     * @param data
     * @return
     */
    public static Bitmap decodeJpegData(byte[] data) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
            if ((!options.mCancel) && (options.outWidth > 0) && options.outHeight > 0) {

                int insamplesize = 1;
                float scale = ImageHelper.getFitSampleSizeLarger(options.outWidth, options.outHeight);
                scale = ImageHelper.checkCanvasAndTextureSize(options.outWidth, options.outHeight, scale);

                int i = 1;
                while (scale / Math.pow(i, 2) > 1.0f) {
                    i *= 2;
                }
                if (i != 1) {
                    i = i / 2;
                }
                insamplesize = i;

                if(scale != 1.0f) {
                    int targetDensity = CameraApp.getApplication().getResources().getDisplayMetrics().densityDpi;
                    options.inScaled = true;
                    options.inDensity = (int) (targetDensity * Math.sqrt(scale / Math.pow(i, 2)) + 1);
                    options.inTargetDensity = targetDensity;
                }

                options.inSampleSize = insamplesize;
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inMutable = true;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
            }
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        return bitmap;
    }

    public static Bitmap decodeJpegDataBig(byte[] data, int rotation) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
            if ((!options.mCancel) && (options.outWidth > 0) && options.outHeight > 0) {
                int width = options.outWidth;
                int height = options.outHeight;
                if (rotation == 90 || rotation == 360) {
                    width = options.outHeight;
                    height = options.outWidth;
                }
                options.inSampleSize = ImageHelper.getFitSampleSizeNew(width, height);
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inMutable = true;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
            }
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        return bitmap;
    }
    
    /**
     * 解析Jpeg图片
     * 
     * @param data
     * @return
     */
    public static Bitmap decodeJpegDataInBeauty(byte[] data) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
            if ((!options.mCancel) && (options.outWidth > 0) && options.outHeight > 0) {
                options.inSampleSize = ImageHelper.getFitSampleSize(options.outWidth, options.outHeight, true);
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inMutable = true;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
            }
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        return bitmap;
    }
    
    /**
     * 解析拍照图片，已经处理裁剪和旋转
     * 
     * @param context
     * @param data
     * @return
     */
    public static Bitmap decodeJpegDataWithCutAndRotate(Context context, byte[] data,
                                                        boolean flipHorizontal, boolean flipVertical) {
        int rotation = Exif.getOrientation(data);
        Bitmap bitmap = BitmapUtils.decodeJpegDataInBeauty(data);
        
        /**
         * 1:1裁剪
         */
        if (SettingsManager.getPreferenceSquare()) {
            if (bitmap != null) {
                bitmap = BitmapUtils.cropSquareBitmap(bitmap);
            }
        }
        
        /**
         * 图片旋转
         */
        if (bitmap != null && (rotation != 0 || flipHorizontal || flipVertical)) {
            Bitmap tempBp = ImageHelper.rotating(bitmap, rotation, flipHorizontal, flipVertical);
            if (tempBp != null) {
                if (bitmap != null && bitmap != tempBp) {
                    bitmap.recycle();
                }
                bitmap = tempBp;
            }
        }

        return bitmap;
    }
    
    public static int getExifOrientation(File file) {
        int exif_orientation = 0;
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            String exif_orientation_s = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            // from http://jpegclub.org/exif_orientation.html
            if( exif_orientation_s.equals("0") || exif_orientation_s.equals("1") ) {
                // leave at 0
            }
            else if( exif_orientation_s.equals("3") ) {
                exif_orientation = 180;
            }
            else if( exif_orientation_s.equals("6") ) {
                exif_orientation = 90;
            }
            else if( exif_orientation_s.equals("8") ) {
                exif_orientation = 270;
            }
            else {
                // just leave at 0
                Log.e(TAG, "    unsupported exif orientation: " + exif_orientation_s);
            }
            Log.d(TAG, "    exif orientation: " + exif_orientation);
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        return exif_orientation;
    }
    
    /**
	 * 把Bitmap转换成byteArray
	 * @param bmp
	 * @param needRecycle
	 * @return
	 */
	public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.JPEG, 100, output);
		
		
		if (needRecycle) {
			bmp.recycle();
		}
		
		byte[] result = output.toByteArray();
		
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * 把Bitmap转换成byteArray
	 * @param bmp
	 * @param needRecycle
	 * @return
	 */
	public static byte[] bmpToPNGByteArray(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 100, output);
		
		
		if (needRecycle) {
			bmp.recycle();
		}
		
		byte[] result = output.toByteArray();
		
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * 把Bitmap转换成byteArray
	 * @param bmp
	 * @param needRecycle
	 * @return
	 */
	public static byte[] bmpToJPGByteArray(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.JPEG, 100, output);
		
		
		if (needRecycle) {
			bmp.recycle();
		}
		
		byte[] result = output.toByteArray();
		
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

    /**
     * 把Bitmap转换成byteArray
     * @param bmp
     * @param image_quality
     * @param needRecycle
     * @return
     */
    public static byte[] bmpToJPGByteArray(final Bitmap bmp, int image_quality, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(CompressFormat.JPEG, image_quality, output);

        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();

        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    /**
     * 获取水印位置
     *
     * @param context
     * @param rect
     * @return
     */
    public static RectF getWaterMarkRect(Context context, RectF rect, int width, int height) {
        float scale = (float) Math.sqrt(rect.width() * rect.height() / WATER_MARK_REF_SIZE);
        float markHeight = height * scale;
        float markWidth = width * scale;
        float marginX = scale * 40;
        float marginY = scale * 40;
        RectF dst = new RectF(rect.left + rect.width() - markWidth - marginX,
                rect.top + rect.height() - markHeight - marginY,
                rect.left + rect.width() - marginX,
                rect.top +rect.height() - marginY);
        return dst;
    }

    
    /**
     * 对图片添加水印
     *
     * @param context
     * @param bitmap
     * @return
     */
    public static Bitmap getWaterMarkBitmap(Context context, Bitmap bitmap, int waterMarkId, int width, int height) {
    	Bitmap newBitmap = null;
    	Bitmap waterMarkBitmap = null;
        try {
        	waterMarkBitmap = BitmapFactory.decodeResource(context.getResources(), waterMarkId).copy(Bitmap.Config.ARGB_8888, true);
            if (waterMarkBitmap != null) {
            	newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setFlags(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
                Rect rect = canvas.getClipBounds();
                float scale = (float) Math.sqrt(rect.width() * rect.height() / WATER_MARK_REF_SIZE);
                float markHeight = height * scale;
                float markWidth = width * scale;
                float marginX = scale * 40;
                float marginY = scale * 40;
                RectF dst = new RectF(rect.width() - markWidth - marginX,
                        rect.height() - markHeight - marginY,
                        rect.width() - marginX,
                        rect.height() - marginY);
                canvas.drawBitmap(bitmap, 0, 0, paint);
                canvas.drawBitmap(waterMarkBitmap, null, dst, paint);
            }
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        return newBitmap;
    }

    /**
     * 对图片添加文字水印
     * (文字可以是日期)
     *
     * @param bitmap
     * @param text
     * @param text
     * @return
     */
    public static Bitmap getWaterMarkBitmap(Bitmap bitmap, String text, int rotation) {
        if(TextUtils.isEmpty(text)) return null;
        Bitmap newBitmap = null;
        try {
            rotation = (rotation + 360) % 360;
            newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(newBitmap);
            Paint paint = new Paint();
            paint.setFlags(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            Rect rect = canvas.getClipBounds();
            float scale = (float) Math.sqrt(rect.width() * rect.height() / DATE_MARK_REF_SIZE);

            TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(DEFAULT_DATE_MARK_TEXT_SIZE * scale);
            textPaint.setColor(DEFAULT_DATE_MARK_TEXT_COLOR);
            textPaint.setShadowLayer(DEFAULT_DATE_MARK_SHADER_RADIUS * scale, 0, 0, DEFAULT_DATE_MARK_SHADER_COLOR);
            float textWidth = StaticLayout.getDesiredWidth(text, textPaint);
            StaticLayout textLayout = new StaticLayout(text, 0, text.length(), textPaint, (int)(textWidth + 1), Layout.Alignment.ALIGN_NORMAL, 1f, 0.0f,
                    false, TextUtils.TruncateAt.END, (int)(textWidth + 1));
            int textHeight = textLayout.getHeight();

            float distance = /*(textHeight - DEFAULT_DATE_MARK_TEXT_SIZE * scale) / 2*/0;

            float marginX = scale * 30;
            float marginY = scale * 30;
            RectF dst = new RectF();
            if(rotation == 90){
                dst.set(rect.width() - textWidth - marginX,
                        marginY - distance,
                        rect.width() - marginX,
                        textHeight + marginY - distance);
            } else if(rotation == 180){
                dst.set(marginX,
                        marginY - distance,
                        textWidth + marginX,
                        textHeight + marginY - distance);
            } else if(rotation == 270){
                dst.set(marginX,
                        rect.height() - textHeight - marginY + distance,
                        textWidth + marginX,
                        rect.height() - marginY + distance);
            } else{//0
                dst.set(rect.width() - textWidth - marginX,
                        rect.height() - textHeight - marginY + distance,
                        rect.width() - marginX,
                        rect.height() - marginY + distance);
            }

            canvas.drawBitmap(bitmap, 0, 0, paint);
            canvas.save();
            if(rotation == 90) {
                canvas.rotate(-rotation, dst.left + dst.width() - dst.height() / 2, dst.centerY());
                canvas.translate(dst.left, dst.top);
            } else if(rotation == 180){
                canvas.rotate(-rotation, dst.centerX(), dst.centerY());
                canvas.translate(dst.left, dst.top);
            } else if(rotation == 270){
                canvas.rotate(-90, dst.left + dst.height() / 2, dst.centerY());
                canvas.rotate(-180, dst.centerX(), dst.centerY());
                canvas.translate(dst.left, dst.top);
            } else{//0
                canvas.translate(dst.left, dst.top);
            }
            textLayout.draw(canvas);
            canvas.restore();
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        return newBitmap;
    }
    
    /**
     * 把矩形按比例最大裁剪
     * 
     * @param rect
     * @return
     */
    public static void cropSetSquare(Rect rect, float ratio) {
        if (rect == null) {
            return;
        }
        float width = rect.width();
        float height = rect.height();
        float originRatio = (float)width / height;
        if (originRatio == ratio) {
            return;
        }
        if (originRatio < ratio) {
            int maskHeight = (int)(height - width / ratio) / 2;
            rect.set(rect.left, rect.top + maskHeight, rect.right, rect.bottom - maskHeight);
        } else {
            int maskWidth = (int)(width - height * ratio) / 2;
            rect.set(rect.left + maskWidth, rect.top, rect.right - maskWidth, rect.bottom);
        }
    }

    public static void saveOrginCurveTexture(String filePath) {
        byte[] toneCurveByteArray = new byte[256 * 4];
        for (int currentCurveIndex = 0; currentCurveIndex < 256; currentCurveIndex++) {
            // BGRA for upload to texture
            toneCurveByteArray[currentCurveIndex * 4 + 2] = (byte) ((int) Math.min(Math.max(currentCurveIndex, 0), 255) & 0xff);
            toneCurveByteArray[currentCurveIndex * 4 + 1] = (byte) ((int) Math.min(Math.max(currentCurveIndex, 0), 255) & 0xff);
            toneCurveByteArray[currentCurveIndex * 4] = (byte) ((int) Math.min(Math.max(currentCurveIndex, 0), 255) & 0xff);
            toneCurveByteArray[currentCurveIndex * 4 + 3] = (byte) ((int) Math.min(Math.max(currentCurveIndex, 0), 255) & 0xff);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            Bitmap bitmap = Bitmap.createBitmap(256, 1, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(toneCurveByteArray));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Throwable tr) {
            tr.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
