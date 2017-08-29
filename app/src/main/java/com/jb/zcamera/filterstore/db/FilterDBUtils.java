package com.jb.zcamera.filterstore.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.gomo.minivideo.CameraApp;
import com.jb.zcamera.filterstore.bo.LocalFilterBO;
import com.jb.zcamera.imagefilter.util.ImageFilterTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 内置、下载滤镜操作
 * @author wangning
 * 2015年3月3日 下午7:00:59
 */
public class FilterDBUtils {

	private static final String TAG = "DBUtils";
	
	SQLiteDatabase db;
	
	Context mContext;
	
	private static FilterDBUtils instance;
	
	private FilterDBUtils() {
		this.mContext = CameraApp.getApplication();
		openDB();
	}
	
	public static FilterDBUtils getInstance() {
		if (instance == null) {
			instance = new FilterDBUtils();
		}
		return instance;
	}
	
	/**
	 * 从服务端下载滤镜插入到sqlite数据库
	 * @author wangning
	 * 2015年3月3日 下午7:12:29
	 */
	public void insertFilter(LocalFilterBO localFilterBO) {
		try {
			openDB();	
			if (db != null && localFilterBO != null) {
				//不存在才需要添加
				if (!isExistFilter(localFilterBO.getName())) {
					ContentValues cv = new ContentValues();
					cv.put(FilterDBHelper.FILTER_STORE_IMAGE_URL, localFilterBO.getImageUrl());
					cv.put(FilterDBHelper.FILTER_STORE_NAME, localFilterBO.getName());
					cv.put(FilterDBHelper.FILTER_STORE_NUM, getMaxNum() + 1);
					cv.put(FilterDBHelper.FILTER_STORE_STATUS, LocalFilterBO.STATUS_USE);
					cv.put(FilterDBHelper.FILTER_STORE_TYPE, LocalFilterBO.TYPE_DOWNLOAD);
					cv.put(FilterDBHelper.FILTER_STORE_PACKAGE_NAME, localFilterBO.getPackageName());
					cv.put(FilterDBHelper.FILTER_STORE_APK_URL, localFilterBO.getApkUrl());
					cv.put(FilterDBHelper.FILTER_STORE_MAP_ID, localFilterBO.getMapId());
					cv.put(FilterDBHelper.FILTER_STORE_DOWNLOAD_URL, localFilterBO.getDownloadUrl());
					cv.put(FilterDBHelper.FILTER_STORE_SIZE, localFilterBO.getSize());
					cv.put(FilterDBHelper.FILTER_STORE_CATEGORY, localFilterBO.getCategory());
					cv.put(FilterDBHelper.FILTER_STORE_STYPE, localFilterBO.getStype());
					cv.put(FilterDBHelper.FILTER_STORE_COLOR, localFilterBO.getColor());
					cv.put(FilterDBHelper.FILTER_STORE_LOCK, localFilterBO.isUnlock() ? 1 : 0);
					db.insert(FilterDBHelper.TABLE_NAME_FILTER_STORE, null, cv);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * 是否已经存在滤镜
	 * @author wangning
	 * 2015年3月3日 下午7:15:59
	 * @param name 滤镜名称
	 * @return
	 */
	public boolean isExistFilter(String name) {
		try {
			openDB();
			
			String sql = "select * from " + FilterDBHelper.TABLE_NAME_FILTER_STORE +" where " + FilterDBHelper.FILTER_STORE_NAME +"='" + name+"'";
	        Cursor cursor = db.rawQuery(sql, null);
	        if (cursor != null) {
	        	boolean flag = cursor.getCount() > 0 ? true : false;
	        	cursor.close();		
	        	return flag;
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        return false;
	}

	
	/**
	 * local界面 ， 删除内置滤镜，其实是更新状态为不可用
	 * @author wangning
	 * 2015年3月3日 下午7:43:55
	 */
	public void deleteInternalFilterById(int id) {
		try {
			openDB();
			
			//最大排序号
			String sql = "update " + FilterDBHelper.TABLE_NAME_FILTER_STORE
					+ " set " + FilterDBHelper.FILTER_STORE_STATUS + " = " + LocalFilterBO.STATUS_NO
					+ " where " + FilterDBHelper.FILTER_STORE_ID +" = "  + id;
			db.execSQL(sql);	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 在filter列表点击下载内置滤镜， 把该滤镜状态改为可用
	 * @author wangning
	 * 2015年3月6日 下午3:20:42
	 * @param name
	 */
	public void updateInternalFilterByName(String name) {
		try {
			openDB();
			//最大排序号
			int maxNum = getMaxNum();
			String sql = "update " + FilterDBHelper.TABLE_NAME_FILTER_STORE
					+ " set " + FilterDBHelper.FILTER_STORE_STATUS + " = " + LocalFilterBO.STATUS_USE
					+ " , " + FilterDBHelper.FILTER_STORE_NUM + " = " + maxNum
					+ " where " + FilterDBHelper.FILTER_STORE_NAME +" = '"  + name +"'";
			db.execSQL(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * local界面 ， 删除下载滤镜
	 * @author wangning
	 * 2015年3月3日 下午7:43:02
	 * @param id
	 */
	public void deleteLocalFilterById(int id) {
		try {
			openDB();
			String sql = "delete from " + FilterDBHelper.TABLE_NAME_FILTER_STORE + " where " + FilterDBHelper.FILTER_STORE_ID +" = "  + id;
			db.execSQL(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	
	/**
	 * 根据包名删除滤镜 (sdcard滤镜文件被删除， 点击)
	 * @author wangning
	 * 2015年6月8日 下午5:31:04
	 */
	public void deleteFilterByPackageNames(ArrayList<String> packageNameList) {
		
		try {
			openDB();
			
			String sql = "";
			if (packageNameList == null || packageNameList.size() < 1) {
				//为空， 则删除下载的
				sql = "delete from  " + FilterDBHelper.TABLE_NAME_FILTER_STORE +" where " + FilterDBHelper.FILTER_STORE_TYPE +" = " + LocalFilterBO.TYPE_DOWNLOAD;
			} else {
				//delete from  t_packagename where package_name not in ('a' , 'c');
				
				//拼接sql 一次删除多条下载的滤镜
				sql = "delete from  " + FilterDBHelper.TABLE_NAME_FILTER_STORE +  " where " 
						+ FilterDBHelper.FILTER_STORE_TYPE +" = " + LocalFilterBO.TYPE_DOWNLOAD
						+" and "+ FilterDBHelper.FILTER_STORE_NAME + " not in (";
				if (packageNameList.size() > 0) {
					for (String pkName : packageNameList) {
						sql += "'" + pkName +"',";
					}
				}
				
				if (sql.endsWith(",")) {
					sql = sql.substring(0, sql.length() -1);
				}
				sql += ")";
			}
			
			db.execSQL(sql);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 上下拖动loca，改变顺序后，更新数据库状态。
	 * @author wangning
	 * 2015年3月4日 下午7:12:01
	 * @param listNew
	 */
	public void updateNum(List<LocalFilterBO> listNew) {
		try {
			openDB();
			db.beginTransaction(); // 手动设置开始事务
		
			// 批量处理操作
			for (int i = 0; i < listNew.size(); i++) {
				LocalFilterBO localFilterBO = listNew.get(i);
				String sql = "update " + FilterDBHelper.TABLE_NAME_FILTER_STORE
						+ " set " + FilterDBHelper.FILTER_STORE_NUM + " = " + (i+1) 
						+ " where " + FilterDBHelper.FILTER_STORE_ID + " = " + localFilterBO.getId();
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
	
	/**
	 * 根据包名解锁
	 * @author wangning
	 * 2015年8月20日 下午5:29:08
	 * @param packageName
	 */
	public void updateLockByPackageName(String packageName) {
		try {
			openDB();
			String sql = "update " + FilterDBHelper.TABLE_NAME_FILTER_STORE
					+ " set " + FilterDBHelper.FILTER_STORE_LOCK + " =  " + LocalFilterBO.UN_LOCK
					+ " where " + FilterDBHelper.FILTER_STORE_PACKAGE_NAME + " = '" + packageName + "'";
			db.execSQL(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取最大num ， 插入时使用
	 * @author wangning
	 * 2015年3月3日 下午7:21:36
	 * @return
	 */
	public int getMaxNum() {
		int num = 0;
		
		try {
			openDB();
			
			String sql = "select max(num) as num from " + FilterDBHelper.TABLE_NAME_FILTER_STORE;
	        Cursor cursor = db.rawQuery(sql, null);
	        
	        while (cursor.moveToNext()) {
	            num = cursor.getInt(cursor.getColumnIndex("num"));  
	            break;
	        }  
	        cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return num;
	}
	
	/**
	 * 根据包名获取滤镜
	 * @author wangning
	 * 2015年3月12日 下午7:13:25
	 * @return
	 */
	public LocalFilterBO getLocalFilterByPackageName(String packageName) {
		String sql = "select * from " + FilterDBHelper.TABLE_NAME_FILTER_STORE +" where " + FilterDBHelper.FILTER_STORE_PACKAGE_NAME +" = '" + packageName +"'";
		ArrayList<LocalFilterBO> list = getLocalFilterList(sql);
		if (list !=null && list.size() > 0) {
			return list.get(0);
		}
        return null;
	}
	
	
	/**
	 * 获取所有可用的滤镜
	 * @author wangning
	 * 2015年3月12日 下午7:13:00
	 * @return
	 */
	public ArrayList<LocalFilterBO> getLocalFilterListAll() {
		String sql = "select * from " + FilterDBHelper.TABLE_NAME_FILTER_STORE
				+ " order by " + FilterDBHelper.FILTER_STORE_NUM;
        return getLocalFilterList(sql);
	}
	
	
	/**
	 * 查询所有滤镜 , 过滤掉more , 并且状态可用 , Local界面使用 , num 顺序排列
	 * @author wangning
	 * 2015年3月3日 下午7:39:44
	 * @return
	 */
	public ArrayList<LocalFilterBO> getLocalFilterListStatusUse() {
		String sql = "select * from " + FilterDBHelper.TABLE_NAME_FILTER_STORE
				+ " where " + FilterDBHelper.FILTER_STORE_STATUS +" = " + LocalFilterBO.STATUS_USE
				+ " order by " + FilterDBHelper.FILTER_STORE_NUM;
		return getLocalFilterList(sql);
	}

	/**
	 * 本地内置不可用(删除)状态,
	 * @author wangning
	 * 2015年3月6日 下午3:33:19
	 * @return
	 */
	public ArrayList<LocalFilterBO> getLocalFilterListStatusNo() {
		String sql = "select * from " + FilterDBHelper.TABLE_NAME_FILTER_STORE
				+ " where " + FilterDBHelper.FILTER_STORE_TYPE +" = "  + LocalFilterBO.TYPE_LOCAL_INTERNAL
				+ " and " + FilterDBHelper.FILTER_STORE_STATUS +" = " + LocalFilterBO.STATUS_NO
				+ " order by " + FilterDBHelper.FILTER_STORE_NUM;
        return getLocalFilterList(sql);
	}
	
	/**
	 * 查询所有滤镜
	 * @author wangning
	 * 2015年3月3日 下午7:39:44
	 * @return
	 */
	private ArrayList<LocalFilterBO> getLocalFilterList(String sql) {
		ArrayList<LocalFilterBO> list = new ArrayList<LocalFilterBO>();
		try {
			openDB();
			HashMap<String, Integer> oldMapIds = ImageFilterTools.OLD_DOWNLOAD_MAP_ID;
			HashMap<String, String> oldMapColors = ImageFilterTools.OLD_DOWNLOAD_COLOR;
	        Cursor cursor = db.rawQuery(sql, null);
	        while (cursor.moveToNext()) {
	        	LocalFilterBO localFilterBO = new LocalFilterBO();
	            localFilterBO.setId(cursor.getInt(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_ID)));
	            localFilterBO.setImageUrl(cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_IMAGE_URL)));  
	            localFilterBO.setName(cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_NAME)));  
	            localFilterBO.setNum(cursor.getInt(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_NUM)));  
	            localFilterBO.setStatus(cursor.getInt(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_STATUS)));
	            localFilterBO.setType(cursor.getInt(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_TYPE)));
				String packageName = cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_PACKAGE_NAME));
	            localFilterBO.setPackageName(packageName);
	            localFilterBO.setApkUrl(cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_APK_URL)));

				Integer mapid = oldMapIds.get(packageName);
				if (mapid != null && mapid != 0) {
					//适配旧滤镜包mapid
					localFilterBO.setMapId(mapid);
				} else {
					localFilterBO.setMapId(cursor.getInt(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_MAP_ID)));
				}

	            localFilterBO.setDownloadUrl(cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_DOWNLOAD_URL)));
	            localFilterBO.setSize(cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_SIZE)));
	            localFilterBO.setCategory(cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_CATEGORY)));
	            localFilterBO.setStype(cursor.getInt(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_STYPE)));

				String oldColor = oldMapColors.get(packageName);
				if(TextUtils.isEmpty(oldColor)) {
					localFilterBO.setColor(cursor.getString(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_COLOR)));
				} else {
					localFilterBO.setColor(oldColor);
				}

	            
	            //1解锁、 0没解锁
	            localFilterBO.setUnlock(cursor.getInt(cursor.getColumnIndex(FilterDBHelper.FILTER_STORE_LOCK)) == 1 ? true : false);
	            
	            list.add(localFilterBO);
	        }  
	        cursor.close();	
		} catch (Exception e) {
			e.printStackTrace();
		}
        return list;
	}
	
	private void openDB() {
		if (db == null) {
			try {
				FilterDBHelper dbHelper = new FilterDBHelper(mContext);
				/*
				 *getWriteableDataBase()其实是相当于getReadableDatabase()的一个子方法，getWriteableDataBase()是只能返回一个以读写方式打开的SQLiteDatabase的引用，
				 *如果此时数据库不可写时就会抛出异常，比如数据库的磁盘空间满了的情况。而getReadableDatabase()一般默认是调用getWriteableDataBase()方法，
				 *如果数据库不可写时就会返回一个以只读方式打开的SQLiteDatabase的引用，这就是二者最明显的区别。
				 */			
				db = dbHelper.getReadableDatabase();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
}
