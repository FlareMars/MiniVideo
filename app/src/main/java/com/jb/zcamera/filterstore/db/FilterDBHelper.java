package com.jb.zcamera.filterstore.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.jb.zcamera.filterstore.bo.LocalFilterBO;
import com.jb.zcamera.imagefilter.util.ImageFilterTools;

/**
 * 滤镜主题商店数据库操作
 * DATA_VERSION = 2 版本号为1.44 ， 添加颜色字段，并且更新所有滤镜的默认颜色值
 * DATA_VERSION = 3 版本号为2.04 ， 添加一款默认滤镜，放在第一位置
 * @author wangning
 * 2015年3月3日 下午7:04:17
 */
public class FilterDBHelper extends SQLiteOpenHelper {

	
	/**
	 * 数据库版本 
	 */
    private static final int DATA_VERSION = 4;
    
    /**
     * 数据库名称
     */
    private static final String DATA_NAME = "camera_filter_store_db";

    /**
     * 表名
     */
    static final String TABLE_NAME_FILTER_STORE = "t_filter_store";
    
    static final String FILTER_STORE_ID = "id";
    
    /**
     * 名称
     */
    static final String FILTER_STORE_NAME = "name";
    
    /**
     * 1、内置 ，  2、more ， 3、下载
     */
    static final String FILTER_STORE_TYPE = "type";
    
    /**
     * 是否解锁：  1已解锁 ， 0还没解锁
     */
    static final String FILTER_STORE_LOCK = "lock";
    
    /**
     * 每个滤镜对应的颜色值
     */
    static final String FILTER_STORE_COLOR = "color";
    
    /**
     * 内置滤镜删除后，变成不可用，下载滤镜删除后，直接从sqlite删除
     * 状态：1、可用，  2、不可用
     */
    static final String FILTER_STORE_STATUS = "status";
    
    /**
     * Local排序，上下拖动使用
     */
    static final String FILTER_STORE_NUM = "num";
    
    /**
     * 内置、more使用名称，下载保存url地址
     */
    static final String FILTER_STORE_IMAGE_URL = "imageUrl";
    
    /**
     * 包名
     */
    static final String FILTER_STORE_PACKAGE_NAME = "packageName";
    
    /**
     * mapid
     */
    static final String FILTER_STORE_MAP_ID = "mapId";
    
    /**
     * mapid
     */
    static final String FILTER_STORE_DOWNLOAD_URL = "downloadUrl";
    
    static final String FILTER_STORE_SIZE = "size";
    
    static final String FILTER_STORE_CATEGORY = "category";
    
    static final String FILTER_STORE_STYPE = "stype";
    
    
    /**
     * apk在本地sdcard中的位置
     */
    static final String FILTER_STORE_APK_URL = "apkUrl";
    
	private static final String SQL_PRAISE = "create table if not exists " + TABLE_NAME_FILTER_STORE + " ("
			+ FILTER_STORE_ID + " INTEGER primary key autoincrement,"
			+ FILTER_STORE_NAME + " TEXT, " 
			+ FILTER_STORE_TYPE + " INTEGER, " 
			+ FILTER_STORE_STATUS + " INTEGER, "
			+ FILTER_STORE_NUM + " INTEGER, "
			+ FILTER_STORE_PACKAGE_NAME + " TEXT, "
			+ FILTER_STORE_MAP_ID + " INTEGER, "			
			+ FILTER_STORE_APK_URL + " TEXT, " 
			+ FILTER_STORE_DOWNLOAD_URL + " TEXT, "
			+ FILTER_STORE_SIZE + " TEXT, "
			+ FILTER_STORE_CATEGORY + " TEXT, "
			+ FILTER_STORE_STYPE + " INTEGER, "
			+ FILTER_STORE_IMAGE_URL + " TEXT " + ")";

    
	public FilterDBHelper(Context context) {
		super(context, DATA_NAME, null, DATA_VERSION);		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_PRAISE);
		init(db);
		
		//数据库版本修改为2
		upgrade2(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i("ss", "onUpgrade: " + oldVersion + " , " + newVersion);

		if(oldVersion == 1) {
			upgrade2(db);
			upgrade4(db);
		} else if(oldVersion == 2) {
			upgrade4(db);
		} else if(oldVersion == 3) {
			upgrade4(db);
		}
	}
	
	/**
	 * 初始化默认值
	 * @author wangning
	 * 2015年3月3日 下午7:55:47
	 */
	private void init(SQLiteDatabase db) {
		String[] defaultFilterNames = ImageFilterTools.FILTER_STRING;
		String[] defaultFilterUrls = ImageFilterTools.FILTER_IMAGE_URL;
		String[] defaultPackageNames = ImageFilterTools.FILTER_PACKAGE_NAME;
		int[] mapId = ImageFilterTools.FILTER_MAPID;
		
		for (int i = 0; i < defaultFilterNames.length; i++) {
			ContentValues cv = new ContentValues();
			cv.put(FilterDBHelper.FILTER_STORE_IMAGE_URL, defaultFilterUrls[i]);
			cv.put(FilterDBHelper.FILTER_STORE_NAME, defaultFilterNames[i]);
			cv.put(FilterDBHelper.FILTER_STORE_NUM, 1 + i);
			cv.put(FilterDBHelper.FILTER_STORE_STATUS, LocalFilterBO.STATUS_USE);
			cv.put(FilterDBHelper.FILTER_STORE_MAP_ID, mapId[i]);
			cv.put(FilterDBHelper.FILTER_STORE_TYPE, LocalFilterBO.TYPE_LOCAL_INTERNAL);
			cv.put(FilterDBHelper.FILTER_STORE_PACKAGE_NAME, defaultPackageNames[i]);
			db.insert(FilterDBHelper.TABLE_NAME_FILTER_STORE, null, cv);
		}
	}

	/**
	 * 数据库版本为3，添加一个默认滤镜
	 * @param db
	 */
	private void upgrade3(SQLiteDatabase db){
		ContentValues cv = new ContentValues();
		cv.put(FilterDBHelper.FILTER_STORE_IMAGE_URL, "filter_test10");
		cv.put(FilterDBHelper.FILTER_STORE_NAME, "test10");
		cv.put(FilterDBHelper.FILTER_STORE_NUM, 0);
		cv.put(FilterDBHelper.FILTER_STORE_STATUS, LocalFilterBO.STATUS_USE);
		cv.put(FilterDBHelper.FILTER_STORE_MAP_ID, "102090424");
		cv.put(FilterDBHelper.FILTER_STORE_TYPE, LocalFilterBO.TYPE_LOCAL_INTERNAL);
		cv.put(FilterDBHelper.FILTER_STORE_PACKAGE_NAME, "com.jb.zcamera.imagefilter.plugins.test");
		db.insert(FilterDBHelper.TABLE_NAME_FILTER_STORE, null, cv);
	}

	/**
	 * 因为数据库版本为3时，插入数据错误。重新删除在插入正确的值
	 * @param db
	 */
	private void upgrade4(SQLiteDatabase db) {
		String packageName = "com.jb.zcamera.imagefilter.plugins.test";
		String sql = "delete from " + FilterDBHelper.TABLE_NAME_FILTER_STORE + " where " + FilterDBHelper.FILTER_STORE_PACKAGE_NAME +" = '"  + packageName + "'";
		db.execSQL(sql);

		packageName = "com.jb.zcamera.imagefilter.plugins.snow";
		if(!isExistFilter(db , packageName)) {
			//不存在才插入
			ContentValues cv = new ContentValues();
			cv.put(FilterDBHelper.FILTER_STORE_IMAGE_URL, "filter_snow");
			cv.put(FilterDBHelper.FILTER_STORE_NAME, "Snow");
			cv.put(FilterDBHelper.FILTER_STORE_NUM, 0);
			cv.put(FilterDBHelper.FILTER_STORE_STATUS, LocalFilterBO.STATUS_USE);
			cv.put(FilterDBHelper.FILTER_STORE_MAP_ID, "102090513");
			cv.put(FilterDBHelper.FILTER_STORE_TYPE, LocalFilterBO.TYPE_LOCAL_INTERNAL);
			cv.put(FilterDBHelper.FILTER_STORE_PACKAGE_NAME, "com.jb.zcamera.imagefilter.plugins.snow");
			db.insert(FilterDBHelper.TABLE_NAME_FILTER_STORE, null, cv);
		}
	}

	public boolean isExistFilter(SQLiteDatabase db, String pkName) {
		boolean flag = false;
		try {
			String sql = "select * from " + FilterDBHelper.TABLE_NAME_FILTER_STORE +" where " + FilterDBHelper.FILTER_STORE_PACKAGE_NAME +"='" + pkName+"'";
			Cursor cursor = db.rawQuery(sql, null);
			if (cursor != null) {
				flag = cursor.getCount() > 0 ? true : false;
				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * 数据库为版本2的升级：
	 * 1、添加解锁字段
	 * 2、添加颜色值字段
	 * 3、给默认滤镜重新赋值
	 * @author wangning
	 * 2015年8月20日 下午6:23:20
	 */
	private void upgrade2(SQLiteDatabase db){
		updateTable2(db);
		initDefaultColor2(db);
	}
	
	/**
	 * DATA_VERSION为2时，添加 解锁lock字段
	 * @author wangning
	 * 2015年8月20日 下午5:51:27
	 * @param db
	 */
	private void updateTable2(SQLiteDatabase db){
		//添加解锁字段
		String sql = "ALTER TABLE "+TABLE_NAME_FILTER_STORE+" ADD COLUMN "+ FILTER_STORE_LOCK+" INTEGER;";
		db.execSQL(sql);
		
		//添加颜色值字段
		sql = "ALTER TABLE "+TABLE_NAME_FILTER_STORE+" ADD COLUMN "+ FILTER_STORE_COLOR+" TEXT;";
		db.execSQL(sql);
	}
	
	/**
	 * 数据库为版本2时，添加的默认滤镜值
	 * @author wangning
	 * 2015年8月20日 下午6:27:23
	 */
	private void initDefaultColor2(SQLiteDatabase db){
		try {
			db.beginTransaction(); // 手动设置开始事务
			String[] defaultPackageNames = ImageFilterTools.FILTER_PACKAGE_NAME;
//			String[] defaultColors = ImageFilterTools.FILTER_COLORS;
			//int[] filterMapId = ImageFilterTools.FILTER_MAPID;
			// 批量处理操作
			for(int i = 0; i < defaultPackageNames.length; i++){
				String pkgName = defaultPackageNames[i];
				String sql = "update " + FilterDBHelper.TABLE_NAME_FILTER_STORE
						+ " set " + FilterDBHelper.FILTER_STORE_COLOR + " = '" + ImageFilterTools.OLD_DOWNLOAD_COLOR.get(pkgName) + "' "
						+ " where " + FilterDBHelper.FILTER_STORE_PACKAGE_NAME + " = '" + pkgName + "'";
				db.execSQL(sql);
			}
			//设置事务处理成功，不设置会自动回滚不提交
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (db != null) {
				db.endTransaction(); // 处理完成
			}
		}  
		
	}
}
