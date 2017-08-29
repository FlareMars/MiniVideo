package com.jb.zcamera.camera;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 摄像头尺寸数据库
 * <p/>
 * Created by oujingwen on 16-1-5.
 */
public class CameraSizesDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "camera_sizes_db";
    public static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "sizes";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_CAMERAID = "cameraId";
    public static final String FIELD_WIDTH = "width";
    public static final String FIELD_HEIGHT = "height";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_REMARK = "remark";
    public static final String FIELD_REMARK1 = "remark1";
    public static final String FIELD_REMARK2 = "remark2";
    private static final String TAG = CameraSizesDBHelper.class.getSimpleName();
    private static final String SQL_PRAISE = "create table if not exists " + TABLE_NAME + " ("
            + FIELD_ID + " INTEGER primary key autoincrement,"
            + FIELD_CAMERAID + " INTEGER, "
            + FIELD_TYPE + " INTEGER, "
            + FIELD_WIDTH + " INTEGER, "
            + FIELD_HEIGHT + " INTEGER, "
            + FIELD_VALUE + " TEXT, "
            + FIELD_REMARK + " TEXT, "
            + FIELD_REMARK1 + " TEXT, "
            + FIELD_REMARK2 + " TEXT )";

    public CameraSizesDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_PRAISE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * 查询所有尺寸数据
     *
     * @param cameraIds 用于填充id列表
     * @return
     */
    public synchronized List<CameraSize> queryAll(List<Integer> cameraIds) {
        if (cameraIds != null) {
            cameraIds.clear();
        }
        List<CameraSize> sizes = new ArrayList<>();
        Cursor cursor = null;
        CameraSize size = null;
        try {
            cursor = getReadableDatabase().rawQuery("select * from " + CameraSizesDBHelper.TABLE_NAME, null);
            while (cursor.moveToNext()) {
                size = new CameraSize(cursor);
                sizes.add(size);
                if (cameraIds != null && !cameraIds.contains(size.mCameraId)) {
                    cameraIds.add(size.mCameraId);
                }
            }
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return sizes;
    }

    /**
     * 插入尺寸数据
     *
     * @param sizes
     */
    public synchronized void insert(List<CameraSize> sizes) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues cv = null;
            for (CameraSize size : sizes) {
                cv = new ContentValues();
                cv.put(FIELD_CAMERAID, size.mCameraId);
                cv.put(FIELD_TYPE, size.mType);
                cv.put(FIELD_WIDTH, size.mWidth);
                cv.put(FIELD_HEIGHT, size.mHeight);
                cv.put(FIELD_VALUE, size.mValue);
                db.insert(TABLE_NAME, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 清除所有尺寸数据
     */
    public synchronized boolean clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete(TABLE_NAME, null, null);
            return true;
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
        }
        return false;
    }

    public static class CameraSize {
        public static final int TYPE_PICTURE = 0;
        public static final int TYPE_VIDEO = 1;

        public int mCameraId;
        public int mType;
        public int mWidth;
        public int mHeight;
        public String mValue;

        public CameraSize(Cursor cursor) {
            this.mCameraId = cursor.getInt(1);
            this.mType = cursor.getInt(2);
            this.mWidth = cursor.getInt(3);
            this.mHeight = cursor.getInt(4);
            this.mValue = cursor.getString(5);
        }

        public CameraSize(int cameraId, int type, int width, int height, String value) {
            this.mCameraId = cameraId;
            this.mHeight = height;
            this.mType = type;
            this.mValue = value;
            this.mWidth = width;
        }
    }
}
