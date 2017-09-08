package com.jb.zcamera.filterstore.bo;

import android.graphics.Color;
import android.text.TextUtils;

import com.gomo.minivideo.CameraApp;
import com.pixelslab.stickerpe.R;

import java.io.Serializable;

/**
 * 本地滤镜bo
 * @author wangning
 * 2015年3月2日 下午5:53:08
 */
public class LocalFilterBO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 本地内置
	 */
	public static int TYPE_LOCAL_INTERNAL = 1;
	
	/**
	 * more
	 */
	public static int TYPE_MORE = 2;
	
	
	/**
	 * 网络下载
	 */
	public static int TYPE_DOWNLOAD = 3;
	
	/**
	 * Original
	 */
	public static int TYPE_ORIGINAL = 4;
	
	/**
	 * 状态可用
	 */
	public static int STATUS_USE = 1;
	
	/**
	 * 状态不可用
	 */
	public static int STATUS_NO = 2;
	
	/**
	 * 还没解锁
	 */
	public static int LOCK = 0;
	
	/**
	 * 已解锁
	 */
	public static int UN_LOCK = 1;

	/**
	 * 分类：0或者空是滤镜类型
	 */
	public static String CATEGORY_FILTER = "0";

	/**
	 * 分类：1是画中画类型
	 */
	public static String CATEGORY_PIP = "1";

	
	private int id;
	
	/**
	 * 名称
	 */
	private String name;
	
	/**
	 * （内置、more使用名称，下载保存url地址）
	 */
	private String imageUrl;
	
	/**
	 * （1、内置 ，  2、more ， 3、下载）
	 */
	private int type;
	
	/**
	 * 内置滤镜删除后，变成不可用，下载滤镜删除后，直接从sqlite删除
	 * （状态：1、可用，  2、不可用）
	 */
	private int status;
	
	/**
	 * Local排序，上下拖动使用
	 */
	private int num;
	
	/**
	 * 包名
	 */
	private String packageName;
	
	/**
	 * 内置滤镜使用mapid查询详情图片
	 */
	private int mapId;
	
	/**
	 * apk下载到本地sdcard地址
	 */
	private String apkUrl;
	
	/**
	 * 下载地址
	 */
	private String downloadUrl;
	
	
	/**
	 * 安装包大小
	 */
	private String size;
	
	/**
	 * 分类：   0或者空是滤镜类型、  1是画中画类型
	 */
	private String category;
	
	/**
	 * 是否有最新滤镜标识 ， 0不是， 1是new标识
	 */
	private int stype;
	
	/**
	 * 滤镜颜色
	 */
	private String color;
	
	/**
	 * 是否已经解锁，true已解锁、false还没解锁
	 */
	private boolean isUnlock;
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getApkUrl() {
		return apkUrl;
	}

	public void setApkUrl(String apkUrl) {
		this.apkUrl = apkUrl;
	}

	public int getMapId() {
		return mapId;
	}

	public void setMapId(int mapId) {
		this.mapId = mapId;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getStype() {
		return stype;
	}

	public void setStype(int stype) {
		this.stype = stype;
	}	

	public String getColor() {
		return color;
	}
	
	/**
	 * 把颜色的字符串转换int类型
	 * @author wangning
	 * 2015年9月2日 下午3:51:37
	 * @return
	 */
	public int getColorInt() {
		int resultColor = 0;
		if(!TextUtils.isEmpty(color)) {
			resultColor = Color.parseColor(color);
        } else {
        	resultColor = CameraApp.getApplication().getResources().getColor(R.color.filter_store_default_color);
        }
		return resultColor;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public boolean isUnlock() {
		return isUnlock;
	}

	public void setUnlock(boolean isUnlock) {
		this.isUnlock = isUnlock;
	}

}
