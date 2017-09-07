
package com.jb.zcamera.folder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.text.TextUtils;
import android.util.Log;

import com.jb.zcamera.camera.SettingsManager;
import com.jb.zcamera.utils.DateMaskUtil;
import com.jb.zcamera.utils.PhoneInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件夹帮助类
 * 
 * @author oujingwen
 *
 */
public class FolderHelper {
    private static final String TAG = "FolderHelper";

    public static final int MEDIA_TYPE_IMAGE = 1;

    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final int MEDIA_TYPE_DYNAMIC = 3;

    public static final int MEDIA_TYPE_GIF = 4;

    private static final String VIDEO_BASE_URI = "content://media/external/video/media";

    public static final String MEDIA_TEMP_FOLDER_PATH = Environment.getExternalStorageDirectory() + File.separator + "MiniVideo" + File.separator + "MediaTemp";

    public static final String  DICM_ROOT_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();

    public static String getSaveLocation() {
        String folder_name = SettingsManager.getPreferenceSaveLocation();
        return folder_name;
    }

    public static File getBaseFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    public static File getImageFolder(String folder_name) {
        File file = null;
        if (folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length() - 1) {
            // ignore final '/' character
            folder_name = folder_name.substring(0, folder_name.length() - 1);
        }
        // if( folder_name.contains("/") ) {
        if (folder_name.startsWith("/")) {
            file = new File(folder_name);
        } else {
            file = new File(getBaseFolder(), folder_name);
        }
        /*
         * if( Loger.isD() ) { Log.d(TAG, "folder_name: " + folder_name);
         * Log.d(TAG, "full path: " + file); }
         */
        return file;
    }

    public static File getImageFolder() {
        String folder_name = getSaveLocation();
        return getImageFolder(folder_name);
    }

    public static File getTempFolder(Context context) {
        return new File(MEDIA_TEMP_FOLDER_PATH);
    }

    public static File getTempFolder() {
        return new File(MEDIA_TEMP_FOLDER_PATH);
    }

    /** Create a File for saving an image or video */
    @SuppressLint("SimpleDateFormat")
    public static File getOutputMediaFile(Context context, int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(context.getFilesDir(), "videos");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(new Date());
        String index = "";
        File mediaFile = null;
        boolean hasDateMask = DateMaskUtil.getDataMarkOpen();
        for (int count = 1; count <= 100; count++) {
            if (type == MEDIA_TYPE_IMAGE) {
                String name = mediaStorageDir.getPath() + File.separator + "IMG_"
                        + timeStamp + index + ".jpg";
                if(hasDateMask) {
                    name = DateMaskUtil.addDateMark(name);
                }
                mediaFile = new File(name);
            } else if (type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"
                        + timeStamp + index + ".mp4");
            } else if (type == MEDIA_TYPE_DYNAMIC) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "ZDYNAMIC_"
                        + timeStamp + index + ".mp4");
            } else if (type == MEDIA_TYPE_GIF) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"
                        + timeStamp + index + ".gif");
            } else {
                return null;
            }
            if (!mediaFile.exists()) {
                break;
            }
            index = "_" + count; // try to find a unique filename
        }

        return mediaFile;
    }

//    /**
//     * 如果是外置 切版本大于5.0使用这个去获取文件
//     * @param context
//     * @param type
//     * @return
//     */
//    public static File getExtSaveFile(Context context, int type){
//        File mediaStorageDir = getImageFolder();
//        // Create a media file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(new Date());
//        String index = "";
//        File mediaFile = null;
//        for (int count = 1; count <= 100; count++) {
//            if (type == MEDIA_TYPE_IMAGE) {
//                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"
//                        + timeStamp + index + ".jpg");
//            } else if (type == MEDIA_TYPE_VIDEO) {
//                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"
//                        + timeStamp + index + ".mp4");
//            } else if (type == MEDIA_TYPE_DYNAMIC) {
//                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "ZDYNAMIC_"
//                        + timeStamp + index + ".mp4");
//            } else if (type == MEDIA_TYPE_GIF) {
//                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"
//                        + timeStamp + index + ".gif");
//            } else {
//                return null;
//            }
//            if (!mediaFile.exists()) {
//                break;
//            }
//            index = "_" + count; // try to find a unique filename
//        }
//
//        if (Loger.isD()) {
//            Log.d(TAG, "getOutputMediaFile returns: " + mediaFile);
//        }
//        return mediaFile;
//    }
    
    public static void broadcastFile(final Context context, final File file, final boolean is_new_picture,
                                     final boolean is_new_video, final int orientation, final OnScanCompletedListener listener) {
        // note that the new method means that the new folder shows up as a file
        // when connected to a PC via MTP (at least tested on Windows 8)
        if (file.isDirectory()) {
            // this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
            // Uri.fromFile(file)));
            // ACTION_MEDIA_MOUNTED no longer allowed on Android 4.4! Gives:
            // SecurityException: Permission Denial: not allowed to send
            // broadcast android.intent.action.MEDIA_MOUNTED
            // note that we don't actually need to broadcast anything, the
            // folder and contents appear straight away (both in Gallery on
            // device, and on a PC when connecting via MTP)
            // also note that we definitely don't want to broadcast
            // ACTION_MEDIA_SCANNER_SCAN_FILE or use scanFile() for folders, as
            // this means the folder shows up as a file on a PC via MTP (and
            // isn't fixed by rebooting!)
            if (listener != null) {
                listener.onScanCompleted(file.getAbsolutePath(), null, orientation);
            }
        } else {
            // both of these work fine, but using
            // MediaScannerConnection.scanFile() seems to be preferred over
            // sending an intent
            // this.sendBroadcast(new
            // Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
            // Uri.fromFile(file)));
            MediaScannerConnection.scanFile(context, new String[]{
                    file.getAbsolutePath()
            }, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    if (is_new_picture) {
                        // note, we reference the string directly rather than
                        // via Camera.ACTION_NEW_PICTURE, as the latter class is
                        // now deprecated - but we still need to broadcase the
                        // string for other apps
                        context.sendBroadcast(new Intent("android.hardware.action.NEW_PICTURE", uri));
                        // for compatibility with some apps - apparently this is
                        // what used to be broadcast on Android?
                        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
                    } else if (is_new_video) {
                        context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));
                    }
                    try {
                        ContentResolver cr = context.getContentResolver();
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
                        cr.update(uri, cv, null, null);
                    } catch (Throwable e) {
                    }
                    if (listener != null) {
                        listener.onScanCompleted(path, uri, orientation);
                    }
                }
            });
        }
    }

    public static void broadcastVideoFile(Context context, File fileSaved, int width, int height,
                                          int rotation, long duration, String[] location,
                                          FolderHelper.OnScanCompletedListener listener) {
        try {
            String title = fileSaved.getName();
            if (title != null) {
                int index = title.lastIndexOf(".");
                if (index > 0) {
                    title = title.substring(0, index);
                }
            }
            long dateTaken = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put(Video.Media.TITLE, title);
            values.put(Video.Media.DISPLAY_NAME, fileSaved.getName());
            values.put(Video.Media.DATE_TAKEN, dateTaken);
            values.put(Video.Media.DATE_MODIFIED, dateTaken / 1000);
            values.put(Video.Media.MIME_TYPE, "video/mp4");
            values.put(Video.Media.DATA, fileSaved.getAbsolutePath());
            if ("90".equals(rotation) || "270".equals(rotation)) {
                values.put(Video.Media.RESOLUTION, height + "x" + width);
                setVideoSize(values, height, width);
            } else {
                values.put(Video.Media.RESOLUTION, width + "x" + height);
                setVideoSize(values, width, height);
            }
            if (location != null) {
                values.put(Video.Media.LONGITUDE, location[0]);
                values.put(Video.Media.LATITUDE, location[1]);
            }
            values.put(Video.Media.SIZE, fileSaved.length());
            values.put(Video.Media.DURATION, duration);
            Uri uri = context.getContentResolver()
                    .insert(Uri.parse(VIDEO_BASE_URI), values);
            if (uri != null) {
                if (listener != null) {
                    listener.onScanCompleted(fileSaved.getAbsolutePath(), uri, 0);
                }
            } else {
                throw new RuntimeException();
            }
        } catch (Throwable tr) {
            // need to scan when finished, so we update for the
            // completed file
            FolderHelper.broadcastFile(context, fileSaved, false,
                    true, 0, listener);
        }
    }

    public static void broadcastVideoFile(Context context, File fileSaved, Location location,
                                          FolderHelper.OnScanCompletedListener listener) {
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(fileSaved.getAbsolutePath());
            String duration = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            String width = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String rotation = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            mmr.release();
            String title = fileSaved.getName();
            if (title != null) {
                int index = title.lastIndexOf(".");
                if (index > 0) {
                    title = title.substring(0, index);
                }
            }
            long dateTaken = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put(Video.Media.TITLE, title);
            values.put(Video.Media.DISPLAY_NAME, fileSaved.getName());
            values.put(Video.Media.DATE_TAKEN, dateTaken);
            values.put(Video.Media.DATE_MODIFIED, dateTaken / 1000);
            values.put(Video.Media.MIME_TYPE, "video/mp4");
            values.put(Video.Media.DATA, fileSaved.getAbsolutePath());
            if ("90".equals(rotation) || "270".equals(rotation)) {
                values.put(Video.Media.RESOLUTION, height + "x" + width);
                setVideoSize(values, height, width);
            } else {
                values.put(Video.Media.RESOLUTION, width + "x" + height);
                setVideoSize(values, width, height);
            }
            if (location != null) {
                values.put(Video.Media.LATITUDE, location.getLatitude());
                values.put(Video.Media.LONGITUDE, location.getLongitude());
            }
            values.put(Video.Media.SIZE, fileSaved.length());
            values.put(Video.Media.DURATION, duration);
            Uri uri = context.getContentResolver()
                    .insert(Uri.parse(VIDEO_BASE_URI), values);
            if (uri != null) {
                if (listener != null) {
                    listener.onScanCompleted(fileSaved.getAbsolutePath(), uri, 0);
                }
            } else {
                throw new RuntimeException();
            }
        } catch (Throwable tr) {
            // need to scan when finished, so we update for the
            // completed file
            FolderHelper.broadcastFile(context, fileSaved, false,
                    true, 0, listener);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setVideoSize(ContentValues values, String width, String height) {
        // The two fields are available since ICS but got published in JB
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            values.put(Video.Media.WIDTH, width);
            values.put(Video.Media.HEIGHT, height);
        }
    }

    public static void asynAddImage(final Context context, final String fileName, final String mimeType,
                                    final long date, final Location location, final int orientation, final int jpegLength,
                                    final String path, final int width, final int height,
                                    final OnScanCompletedListener listener) {
        new Thread() {

            @Override
            public void run() {
                super.run();
                Uri uri = addImage(context, fileName, mimeType, date, location, orientation,
                        jpegLength, path, width, height);
                if (listener != null) {
                    listener.onScanCompleted(path, uri, orientation);
                }
            }

        }.start();
    }

    // Add the image to media store.
    public static Uri addImage(Context context, String fileName, String mimeType,
                               long date, Location location, int orientation, int jpegLength,
                               String path, int width, int height) {
        String title = fileName;
        if (title != null) {
            int index = title.lastIndexOf(".");
            if (index > 0) {
                title = title.substring(0, index);
            }
        }
        // Insert into MediaStore.
        ContentValues values = new ContentValues(9);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, fileName);
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, mimeType);
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, jpegLength);

        setImageSize(values, width, height);

        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }

        Uri uri = null;
        try {
            uri = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setImageSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            values.put(MediaColumns.WIDTH, width);
            values.put(MediaColumns.HEIGHT, height);
        }
    }

    @SuppressWarnings("deprecation")
    public static long freeMemory() { // return free memory in MB
        try {
            File image_folder = getImageFolder();
            StatFs statFs = new StatFs(image_folder.getAbsolutePath());
            // cast to long to avoid overflow!
            long blocks = statFs.getAvailableBlocks();
            long size = statFs.getBlockSize();
            long free  = (blocks*size) / 1048576;
            /*if( MyDebug.LOG ) {
                Log.d(TAG, "freeMemory blocks: " + blocks + " size: " + size + " free: " + free);
            }*/
            return free;
        }
        catch(IllegalArgumentException e) {
            // can fail on emulator, at least!
            return -1;
        }
    }

    public interface OnScanCompletedListener {
        void onScanCompleted(String path, Uri uri, int orientation);
    }

    //
    public static void asynAddImage(final Context context, final String fileName, final String mimeType,
                                    final long date, final String latitude, final String longitude, final int orientation, final int jpegLength,
                                    final String path, final int width, final int height,
                                    final OnScanCompletedListener listener) {
        new Thread() {

            @Override
            public void run() {
                super.run();
                Uri uri = addImage(context, fileName, mimeType, date, latitude, longitude, orientation,
                        jpegLength, path, width, height);
                if (listener != null && uri != null) {
                    listener.onScanCompleted(path, uri, orientation);
                }
            }

        }.start();
    }

 // Add the image to media store.
    public static Uri addImage(Context context, String fileName, String mimeType,
                               long date, String latitude, String longitude, int orientation, int jpegLength,
                               String path, int width, int height) {
        String title = fileName;
        if (title != null) {
            int index = title.lastIndexOf(".");
            if (index > 0) {
                title = title.substring(0, index);
            }
        }
        // Insert into MediaStore.
        ContentValues values = new ContentValues(9);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, fileName);
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, mimeType);
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, jpegLength);

        setImageSize(values, width, height);

        values.put(ImageColumns.LATITUDE, latitude);
        values.put(ImageColumns.LONGITUDE, longitude);

        Uri uri = null;
        try {
            uri = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }

 // Add the video to media store.
    public static Uri addVideo(Context context, String fileName, String mimeType,
                               long date, long dataModify, String latitude, String longitude, long videoLength, String resolution,
                               String path, int width, int height, long duration) {
        String title = fileName;
        if (title != null) {
            int index = title.lastIndexOf(".");
            if (index > 0) {
                title = title.substring(0, index);
            }
        }

        ContentValues values = new ContentValues();
        values.put(Video.Media.TITLE, title);
        values.put(Video.Media.DISPLAY_NAME, fileName);
        values.put(Video.Media.DATE_TAKEN, date);
        values.put(Video.Media.DATE_MODIFIED, dataModify);
        values.put(Video.Media.MIME_TYPE, mimeType);
        values.put(Video.Media.DATA, path);
        values.put(Video.Media.RESOLUTION, resolution);
        values.put(Video.Media.LATITUDE, latitude);
        values.put(Video.Media.LONGITUDE, longitude);
        values.put(Video.Media.SIZE, videoLength);
        values.put(Video.Media.DURATION, duration);
        setVideoSize(values, width, height);
        Uri uri = null;
        try {
			uri = context.getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI, values);
		} catch (Throwable e) {
			 Log.e(TAG, "Failed to write MediaStore" + e.getMessage());
		}
        return uri;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setVideoSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            values.put(Video.Media.WIDTH, width);
            values.put(Video.Media.HEIGHT, height);
        }
    }

    public static String getOrCreateSaveLocation() {
    	String location = null;
		try {
			location = SettingsManager.getPreferenceSaveLocation();
            if(PhoneInfo.isSupportWriteExtSdCard() && ExtSdcardUtils.isExtSdcardPath(location)){
//                ExtSdcardUtils.mkdirExtSdcardPath(CameraApp.getApplication(), location, ExtSdcardUtils.getSavedExtSdcardUri());
            } else{
                File f = new File(location);
                if(!f.exists()){
                    f.mkdirs();
                }
            }
		} catch (Throwable e) {
		}
		return location;
	}

    public static File getOrCreateEditCachePath(Context context) {
        File path = new File(getCachePath(context) + "/.edit_temp");
        if (!path.exists()) {
            if (path.mkdirs()) {
                try {
                    new File(path.getAbsolutePath(), ".nomedia").createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return path;
    }

    public static File getCachePath(Context context) {
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null && externalCacheDir.exists()) {
            return externalCacheDir;
        }
        externalCacheDir = context.getCacheDir();
        if (externalCacheDir != null && externalCacheDir.exists()) {
            return externalCacheDir;
        }
        return new File(Environment.getExternalStorageDirectory().getPath() + ("/Android/data/" + context.getPackageName() + "/cache/"));
    }

    /**
     * 是否是我们的图片文件
     *
     * @param fileName
     * @return
     */
    public static boolean isOurPictureFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        if (fileName.length() != "IMG_yyyyMMdd_HHmmssSSS.jpg".length()
                && fileName.length() != "IMG_yyyyMMdd_HHmmss.jpg".length()
                && !fileName.contains("SPhotoEditor")) {
            return false;
        }
        if (fileName.startsWith("IMG_") && fileName.endsWith(".jpg")) {
            return true;
        }
        return false;
    }

    /**
     * 是否是我们的视频文件
     *
     * @param fileName
     * @return
     */
    public static boolean isOurVideoFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        if (fileName.length() != "VID_yyyyMMdd_HHmmssSSS.mp4".length()
                && fileName.length() != "VID_yyyyMMdd_HHmmss.mp4".length()) {
            return false;
        }
        if (fileName.startsWith("VID_") && fileName.endsWith(".mp4")) {
            return true;
        }
        return isMotionFile(fileName);
    }

    public static boolean isMotionFile(String fileName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        if (fileName.startsWith("ZDYNAMIC_") && (fileName.endsWith(".mp4") || fileName.endsWith(".sphotoeditorv"))) {
            return true;
        }
        return false;
    }

    public static boolean isMotionFilePath(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        int index = filePath.lastIndexOf('/');
        if (index >= 0 && index < filePath.length() - 1) {
            filePath = filePath.substring(index + 1);
        }
        return isMotionFile(filePath);
    }

    /**
     * 保存图片Exif信息
     *
     * @param imgFile
     * @param width
     * @param height
     * @param datetime
     * @param location
     */
    public static void setExif(File imgFile, int width, int height, int orientation, long datetime, Location location) {
        if (imgFile == null) {
            return;
        }
        try {
            android.media.ExifInterface exif = new android.media.ExifInterface(imgFile.getCanonicalPath());

            if (location != null) {
                // String latitudeStr = "90/1,12/1,30/1";
                double lat = location.getLatitude();
                double alat = Math.abs(lat);
                String dms = Location.convert(alat, Location.FORMAT_SECONDS);
                String[] splits = dms.split(":");
                String[] secnds = (splits[2]).split("\\.");
                String seconds;
                if (secnds.length == 0) {
                    seconds = splits[2];
                } else {
                    seconds = secnds[0];
                }

                String latitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";
                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE, latitudeStr);

                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE_REF, lat > 0 ? "N" : "S");

                double lon = location.getLongitude();
                double alon = Math.abs(lon);

                dms = Location.convert(alon, Location.FORMAT_SECONDS);
                splits = dms.split(":");
                secnds = (splits[2]).split("\\.");

                if (secnds.length == 0) {
                    seconds = splits[2];
                } else {
                    seconds = secnds[0];
                }
                String longitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";

                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE, longitudeStr);
                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE_REF, lon > 0 ? "E" : "W");
            }
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(getOrientationValueForRotation(orientation)));
            exif.setAttribute(android.media.ExifInterface.TAG_IMAGE_WIDTH, String.valueOf(width));
            exif.setAttribute(android.media.ExifInterface.TAG_IMAGE_LENGTH, String.valueOf(height));

            if (datetime > 0) {
                String timeStamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(new Date(datetime));
                exif.setAttribute(android.media.ExifInterface.TAG_DATETIME, timeStamp);
            }
            exif.setAttribute(android.media.ExifInterface.TAG_MAKE, "Z Camera");
            exif.saveAttributes();
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
    }

    public static short getOrientationValueForRotation(int degrees) {
        degrees %= 360;
        if(degrees < 0) {
            degrees += 360;
        }

        return (short)(degrees < 90?1:(degrees < 180?6:(degrees < 270?3:8)));
    }

    /**
     * 保存图片Exif信息
     *
     * @param imgFile
     * @param datetime
     */
    public static void setExif(File imgFile, long datetime) {
        if (imgFile == null) {
            return;
        }
        try {
            android.media.ExifInterface exif = new android.media.ExifInterface(imgFile.getCanonicalPath());
            if (datetime > 0) {
                String timeStamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(new Date(datetime));
                exif.setAttribute(android.media.ExifInterface.TAG_DATETIME, timeStamp);
            }
            exif.setAttribute(android.media.ExifInterface.TAG_MAKE, "Z Camera");
            exif.saveAttributes();
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
    }

    /**
     * 检查是否需要移动到位置存储卡
     *
     * @param context
     * @param fileSaved
     * @param mimeType
     * @return 返回新的文件位置，如果不需要移动则返回原位置
     */
    public static File checkMoveToExtSdcard(Context context, File fileSaved, String mimeType) {
        if (fileSaved == null) {
            return null;
        }
        if (MEDIA_TEMP_FOLDER_PATH.equals(fileSaved.getParent())) {
            String fileName = fileSaved.getName();
            String moveToFilePath = getImageFolder().getAbsolutePath() + File.separator + fileName;
            OutputStream os = ExtSdcardUtils.getExtCardOutputStream(context, moveToFilePath, mimeType);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileSaved);
                byte[] buffer = new byte[1024];
                int count = 0;
                while((count = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, count);
                }
                os.flush();
                fileSaved.delete();
                return new File(moveToFilePath);
            } catch (Throwable tr) {
                Log.e(TAG, "", tr);
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                    }
                }
            }
        }
        return fileSaved;
    }

    public static boolean isExtSdcardImagePath() {
        return PhoneInfo.isSupportWriteExtSdCard()
                && ExtSdcardUtils.isExtSdcardPath(FolderHelper.getImageFolder().getAbsolutePath());
    }

    /**
     * 保存图片Exif信息
     * 去除其他相机的Exif
     * @param imgFile
     */
    public static void removeModelExif(File imgFile) {
        if (imgFile == null) {
            return;
        }
        try {
            android.media.ExifInterface exif = new android.media.ExifInterface(imgFile.getCanonicalPath());
            exif.setAttribute(android.media.ExifInterface.TAG_MAKE, "Z Camera");
            exif.setAttribute(android.media.ExifInterface.TAG_MODEL, "Z Camera");
            exif.saveAttributes();
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
    }
}
