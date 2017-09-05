package com.jb.zcamera.gallery.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 
 * @author chenfangyi
 *
 */

public class FileUtil {
    
    private static final String LOG_TAG = "FileUtil";

    public static final String APP_DIR = "ZEROSMS";
    
    public static final String CACHE_DIR = "dir";

    public static final String ANIMATION_BG_DIR = "anim_bg";
    
	/**
	 * 获取压缩包中的单个文件InputStream
	 * 
	 * @param zipFilePath
	 *            压缩文件的完整路径
	 * @param singleFileName
	 *            压缩包中要解压的文件名 <B>（该目录下的路径)</B>
	 * @return InputStream
	 * @throws Exception
	 */
	public static InputStream unzipSingleFile(String zipFilePath,
                                              String singleFileName) throws Exception {
		ZipFile zipFile = new ZipFile(zipFilePath);
		ZipEntry zipEntry = zipFile.getEntry(singleFileName);

		return zipFile.getInputStream(zipEntry);
	}
	
	
    /**
     * 导出ZIP文件的注释
     * @param filename 指定的ZIP文件的完全路径
     * @return ZIP文件的注释，如果找不到任何注释将返回null
     */
    public static String extractZipComment(String filename) {
        if(filename == null || filename.equals("")) return null;
        
        String retStr = null;
        try {
            File file = new File(filename);
            int fileLen = (int) file.length();
            
            FileInputStream in = new FileInputStream(file);
            
            byte[] buffer = new byte[Math.min(fileLen, 2048)];
            int len;
            
            in.skip(fileLen - buffer.length);
            
            if ((len = in.read(buffer)) > 0)
            {
                retStr = getZipCommentFromBuffer(buffer, len);
                if(retStr != null)
                {
                    retStr = retStr.trim();
                }
            }
            
            in.close();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception on reading ZIP comment!", e);
        }
        return retStr;
    }
    
    private static String getZipCommentFromBuffer(byte[] buffer, int len) {
        byte[] magicDirEnd = { 0x50, 0x4b, 0x05, 0x06 };
        int buffLen = Math.min(buffer.length, len);
        
        // Check the buffer from the end
        for (int i = buffLen - magicDirEnd.length - 22; i >= 0; i--) {
            boolean isMagicStart = true;
            for (int k = 0; k < magicDirEnd.length; k++) {
                if (buffer[i + k] != magicDirEnd[k]) {
                    isMagicStart = false;
                    break;
                }
            }
            
            if (isMagicStart) {
                // Magic Start found!
                int commentLen = buffer[i + 20] + buffer[i + 21] * 256;
                int realLen = buffLen - i - 22;
                if (commentLen != realLen)
                {
                    Log.w(LOG_TAG, "ZIP comment size mismatch!");
                }
                String comment = new String(
                        buffer, i + 22, Math.min(commentLen, realLen));
                return comment;
            }
        }

        Log.w(LOG_TAG, "ZIP comment NOT found!");
        return null;
    }
    
	
	/**  
     * 解压一个压缩文档到指定位置  
     * @param zipFilePath 压缩包的路径
     * @param outputPath 指定的路径  
     * @throws Exception 
     */  
    public static boolean unzipFolder(String zipFilePath, String outputPath)
            throws Exception
    {
		File folder = DirectoryUtil.getOrMakeDir(outputPath);
		if (!folder.exists()){
			return false;
		}

        InputStream is = new FileInputStream(zipFilePath);
        unzipSteam(is, outputPath);
        return true;
    }
    
    
    /**
     * 解压一个输入流中的文件到指定位置  
     * @param is 输入流
     * @param outputPath 指定的路径  
     * @throws Exception
     */
    public static void unzipSteam(InputStream is, String outputPath) 
        throws Exception {
        ZipInputStream inputZip = new ZipInputStream(is);
        ZipEntry zipEntry;
        String tempPathName = "";
        
        while ((zipEntry = inputZip.getNextEntry()) != null) {
            tempPathName = zipEntry.getName();
            
            if (zipEntry.isDirectory()) {
                // 获取文件夹名
                tempPathName = tempPathName.substring(0,tempPathName.length() - 1);
                
                DirectoryUtil.getOrMakeNomediaDir(outputPath + File.separator+ tempPathName);
            } else {
                File file = new File(outputPath + File.separator + tempPathName);
                if(!file.exists()){
                    File fileParentDir=file.getParentFile();{
                        if(!fileParentDir.exists()){
                             fileParentDir.mkdirs();  
                        }
                    }
                    file.createNewFile();
                }
                
                // 获取文件输出流
                FileOutputStream fos = new FileOutputStream(file);
                int len;
                
                // 缓冲数组
                byte[] buffer = new byte[1024];
                
                // 读取1024字节进缓冲数组
                while ((len = inputZip.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                    fos.flush();
                }
                fos.close();
            }
        }
        inputZip.close();
    }
    
    /**
     * 删除指定文件夹中的所有文件
     * 
     * @param folderPath 目标文件夹的路径
     * @return 是否成功删除
     */
    public static boolean deleteFilesInFolder(String folderPath)
    {
        File folder = new File(folderPath);
        try
        {
            // 如果图片存放的路径存在并且是目录，则循环删除里面的所有文件
            if (folder.exists() && folder.isDirectory())
            {
                File[] files = folder.listFiles();
                for (File file : files)
                {
                    file.delete();
                }
                return true;
            } else
            {
            }
        } catch (Exception e)
        {
            
        }
        
        return false;
    }
    
    /**
     * 检查文件是否存在
     * @param path 文件的路径
     * @return 文件是否存在
     */
    public static boolean checkExist(String path) {
        if(path == null || path.equals("")) {
            return false;
        }
        
        try {
            File file = new File(path); 
            if(file.exists()) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * 检查并建立指定的文件夹
     * @param folderPath 文件夹的路径
     * @return 是否建立了文件夹
     */
    public static boolean buildFolderIfNotFound(String folderPath) {
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if(folder.mkdirs() == false) {
                    Log.i(LOG_TAG, "The folder is already exist: " + folderPath);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch(Exception ex) {
            Log.w(LOG_TAG,"Fail to build folder: " + folderPath +
                    ", " + ex.getMessage());
            return false;
        }
    }
    
    
    public static void copyFile(File sourceFile, File targetFile) throws IOException
    {
        // 新建文件输入流并对它进行缓冲
        FileInputStream input = new FileInputStream(sourceFile);
        BufferedInputStream inBuff = new BufferedInputStream(input);
        
        // 新建文件输出流并对它进行缓冲
        FileOutputStream output = new FileOutputStream(targetFile);
        BufferedOutputStream outBuff = new BufferedOutputStream(output);
        
        // 缓冲数组
        byte[] b = new byte[1024 * 5];
        int len;
        while ((len = inBuff.read(b)) != -1)
        {
            outBuff.write(b, 0, len);
        }
        
        // 刷新此缓冲的输出流
        outBuff.flush();
        
        // 关闭流
        inBuff.close();
        outBuff.close();
        output.close();
        input.close();
    }
    
    /**
     * 移动文件
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @return 是否成功移动文件
     */
    public static boolean moveFile(File sourceFile, File targetFile) {
        if (sourceFile == null) {
            Log.w(LOG_TAG, "Argument 'sourceFile' is null.");
            return false;
        }
        if (targetFile == null) {
            Log.w(LOG_TAG, "Argument 'targetFile' is null.");
            return false;
        }
        
        try {
            copyFile(sourceFile, targetFile);
            deleteFile(sourceFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.w(LOG_TAG,"Exception on moveFile(): " + e.getMessage());
            return false;
        }
    }
    
    
    /**
     * 删除指定的路径的文件
     * @param filePath 指定文件的完全路径
     */
    public static void deleteFile(String filePath) {
    	try {
			File file = new File(filePath);
			file.delete();
		} catch (Exception e) {
		    Log.w(LOG_TAG,"Exception on deleting file: " + filePath + 
		            ", " + e.getMessage());
		}
    }
    
    /**
     * 删除一个文件夹
     * @param folderPath
     */
    public static void deleteFolder(String folderPath) {
    	try {
    		File file = new File(folderPath);
    		if (!file.exists()) {
    			return;
    		}
    		deleteAllFiles(folderPath);
    		file.delete();
    	} catch (Exception e) {
			// TODO: handle exception
		}
    }
    
    private static void deleteAllFiles(String folderPath) {
    	try {
    		File file = new File(folderPath);
    		File[] listFiles = file.listFiles();
    		if (listFiles != null) {
    			for (File item: listFiles) {
    				if (item.isFile()) {
    					item.delete();
    				} else {
    					deleteFolder(item.getAbsolutePath());
    				}
    			}
    		}
    	} catch (Exception e) {
			// TODO: handle exception
		}
    }
    
    
    /**
     * 判断SD卡是否可用
     * @return SD卡是否可用
     */
    public static boolean isSDCardMounted() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }
    
    
    /**
     * 调用媒体扫描服务扫描整个外部存储器</br>
     * 建议采用{@link #callToScanMediaFile(Context, String)}来扫描指定文件以提高效率
     * @param context 上下文对象
     */
    public static void callToScanMediaFile(Context context) {
        if(context == null) {
            Log.w(LOG_TAG, "Argument 'context' is null.");
            return;
        }
        
        context.sendBroadcast(new Intent(
                Intent.ACTION_MEDIA_MOUNTED, 
                Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    }
    
    /**
     * 调用媒体扫描服务扫描指定的图像文件
     * @param context 上下文对象
     * @param filePath 指定的图像文件的完全路径，不需要包含"<code>file://</code>"的前缀
     */
    public static void callToScanMediaFile(Context context, String filePath) {
        if(context == null) {
            Log.w(LOG_TAG, "Argument 'context' is null.");
            return;
        }
        if(filePath == null || filePath.length() == 0) {
            Log.w(LOG_TAG, "Argument 'filePath' is null or empty.");
            return;
        }
        
        Uri uri = Uri.parse("file://" + filePath);
        if(uri == null) {
            Log.w(LOG_TAG, "Error on parsing file path to URI, filePath: " + filePath);
            return;
        }
        
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    } 
    
    
    private static final String SCHEME_FILE = "file";
    private static final String[] IMAGE_FILE_EXT = { "jpg", "jpeg", "gif", "png", "bmp" };
    
    /**
     * 判断某URI地址是不是非图像文件的地址
     * @param uri 要判断的URI地址
     * @return 如果该地址是非文件地址，或其指向的是图像文件，则返回false；如果是其他非图像文件则返回true
     */
    public static boolean isNonImageFileUri(Uri uri) {
        if (uri == null) {
            Log.w(LOG_TAG, "Argument 'uri' is null.");
            return false;
        }
        if (!uri.isHierarchical()) {
            return false;
        }
        
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(SCHEME_FILE)) {
            return false;
        }
        
        String path = uri.getPath();
        int extStartIndex = path.lastIndexOf('.');
        if (extStartIndex <= 0 || extStartIndex >= path.length() - 1) {
            return true;
        }
        
        String ext = uri.getPath().substring(extStartIndex + 1);
        for (int i = 0; i < IMAGE_FILE_EXT.length; i++) {
            if (ext.equalsIgnoreCase(IMAGE_FILE_EXT[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取GO短信在sd卡上的目录
     * @return 目录全路径，例如：/mnt/sdcard/GOSMS/
     */
    public static String getGOSMSDir() {
    	return Environment.getExternalStorageDirectory().getAbsolutePath() + "/ZEROSMS/";
    }
    
    
    
    /****************Properties操作*************************/
    public static final String GOSMS_FILE_PATH = Environment.getExternalStorageDirectory()+"/ZEROSMS/";
    public static final String PROPERTIES_PATH = GOSMS_FILE_PATH+"properties.cfg";
    
    /**
     * 加载配置文件
     * @return
     */
	public static Properties loadConfig() {
		Properties properties = new Properties();
		try {
			DirectoryUtil.getOrMakeNomediaDir(GOSMS_FILE_PATH);
			FileInputStream s = new FileInputStream(PROPERTIES_PATH);
			properties.load(s);
			s.close();
		} catch (Exception e) {
			
		}
		return properties;
	}
	
    /**
     * 获取Properties值
     * @return
     */
	public static String getPropertiesValue(String key) {
		Properties properties = loadConfig();
		try {
			return properties.get(key).toString();
		} 
		catch (Exception e) {

		}
		return null;
	}

	/**
	 * 保存设置
	 * @param key
	 * @param value
	 */
	public static void saveConfig(Properties properties) {
		try {
			FileOutputStream s = new FileOutputStream(PROPERTIES_PATH, false);
			properties.store(s, "");
			s.flush();
			s.close();
		} catch (Exception e) {

		}
	}
	
	/**
	 * 
	 * @author huyong
	 * @param byteData
	 * @param fileName
	 * @return
	 */
	public static boolean saveByteToCommonIconSDFile(final byte[] byteData, final String fileName) {
		String filePathName = GOSMS_FILE_PATH + "download/";
		filePathName += fileName;
		return saveByteToSDFile(byteData, filePathName);
	}
	
	/**
	 * 保存数据到指定文件
	 * @author huyong
	 * @param byteData
	 * @param filePathName
	 * @return true for save successful, false for save failed.
	 */
	public static boolean saveByteToSDFile(final byte[] byteData, final String filePathName) {
		boolean result = false;
		try {
			File newFile = createNewFile(filePathName, false);
			FileOutputStream fileOutputStream = new FileOutputStream(newFile);
			fileOutputStream.write(byteData);
			fileOutputStream.flush();
			fileOutputStream.close();
			result = true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return result;
	}
	
	public static boolean saveStringToFile(final String str, final String filePathName) {
		boolean result = false;
		FileOutputStream fileOutputStream = null;
		OutputStreamWriter writer = null;
		try {
			File newFile = createNewFile(filePathName, false);
			fileOutputStream = new FileOutputStream(newFile);
			writer = new OutputStreamWriter(fileOutputStream);
			writer.append(str);
			writer.flush();
			result = true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
				if (fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (Exception e1) {
				
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @author huyong
	 * @param path：文件路径
	 * @param append：若存在是否插入原文件
	 * @return
	 */
	public static File createNewFile(String path, boolean append) {
		File newFile = new File(path);
		if ( !append )
		{
			if (newFile.exists())
			{
				newFile.delete();
			}
		}
		if( !newFile.exists() )
        {
        	try
        	{
        		File parent = newFile.getParentFile();
        		if (parent != null && !parent.exists())
        		{
        			parent.mkdirs();
        		}
        		newFile.createNewFile();
        	}
        	catch (Exception e) 
        	{
        		e.printStackTrace();
			}
        }
        return newFile;
	}
	
	/**
	 * 获取媒体文件路径
	 * 
	 * @param context
	 * @param uri
	 * @param c
	 * @return
	 */
    public static String getMediaFilePath(Context context, Uri uri, Cursor c) {
        String filePath = null;
        try {
            filePath = c.getString(c.getColumnIndexOrThrow(Images.Media.DATA));
        } catch (IllegalArgumentException e) {
            try {
                filePath = c.getString(c.getColumnIndexOrThrow("_data"));
            } catch (IllegalArgumentException ex) {
                filePath = uri.getPath();
            }
        }
        return filePath;
    }
    
    /**
     * 获取媒体文件类型
     * 
     * @param context
     * @param uri
     * @param c
     * @return
     */
    public static String getMediaFileMimeType(Context context, Uri uri, Cursor c) {
        String contentType = null;
        try {
            contentType = c.getString(
                    c.getColumnIndexOrThrow(Images.Media.MIME_TYPE)); // mime_type
        } catch (IllegalArgumentException e) {
            try {
                contentType = c.getString(c.getColumnIndexOrThrow("mimetype"));
            } catch (IllegalArgumentException ex) {
                contentType = context.getContentResolver().getType(uri);
            }
        }
        return contentType;
    }
    
    public static void saveMediaFile(Context context, Uri uri, String filePath) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            os = new FileOutputStream(filePath);
            byte[] buffer = new byte[8192];
            for (int len = 0; (len = is.read(buffer)) != -1;) {
                os.write(buffer, 0, len);
            }
        } catch (Throwable e) {
            Log.e("", "", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Throwable e) {
                }
            }
        }
    }
    
    public static void takePersistableUriPermission(Context context, Uri uri, int takeFlags) {
        try {
            Method takePersistableUriPermissionMethod = ContentResolver.class.getMethod(
                    "takePersistableUriPermission", new Class<?>[] {
                            Uri.class, int.class
                    });
            takePersistableUriPermissionMethod.invoke(context.getContentResolver(), uri, takeFlags);
        } catch (Throwable tr) {
            Log.e("", "", tr);
        }
    }
	
	/**
	 * 检查文件路径是否缺少SD卡路径，如果缺少返回完整的路径
	 * 
	 * @param path
	 * @return
	 */
    public static String checkLackESD(String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }

        try {
            File file = new File(path);
            if (file != null && !file.canRead()) {
                try {
                    Log.e("FileUtil", "Path Error:" + path);
                } catch(Throwable t) {
                }
                if (!path.startsWith("/mnt/sdcard")
                        && !path.startsWith(Environment.getExternalStorageDirectory().getPath())) {
                    try {
                    } catch(Throwable t) {
                    }
                    String fixedPath = Environment.getExternalStorageDirectory().getPath() + path;
                    File fixedFile = new File(fixedPath);
                    if (fixedFile.canRead() && fixedFile.isFile()) {
                        try {
                        } catch(Throwable t) {
                        }
                        return fixedPath;
                    }
                }
            }
        } catch (Throwable tr) {
            Log.e("FileUtil", "", tr);
        }

        return path;
    }
    
    /**
     * 通过文件路径获取文件大小
     */
    public static long getFileSize(String path) {
        try {
            Log.e("FileUtil", "FileSize Error:" + path);
        } catch(Throwable t) {
        }
        
        if(path == null) {
            return 0;
        }
        
        try {
            File file = new File(path);
            return file.length();
        } catch (Throwable tr) {
            Log.e("FileUtil", "", tr);
        }
        
        return 0;
    }
    
    /**
     * 保存数据到文件
     * @param filePath 要保存到的文件路径
     * @param files 要保存的二进制数据
     */
    public static void savePicToFile(String filePath, byte[] files) {
		FileOutputStream out = null;
		try {
			File newFile = new File(filePath);
			if (!newFile.exists()) {
				newFile.createNewFile();
			}
			out = new FileOutputStream(newFile);
			out.write(files);

		} catch (IOException e) {

		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
	}
    
    /**
     * 彩信附件文件名处理
     * 
     * @param src
     * @return
     */
    public static String getMmsPartSrc(String src) {
        if(TextUtils.isEmpty(src)) {
            return src;
        }
        
        src = src.replace(' ', '_');
        src = src.replace('=', '_');
        
        if(src.length() > 20) {
            src = src.substring(src.length() - 20);
        }
        
        return src;
    }
    
	public static final String GOSHARE_MEDIA_FILE_SAVE_DIR = Environment.getExternalStorageDirectory()+"/ZEROSMS/.goshare/";

    public static String getMediaFilePath(String fileName) {
        DirectoryUtil.getOrMakeNomediaDir(GOSHARE_MEDIA_FILE_SAVE_DIR);
        Random random = new Random(System.currentTimeMillis());
        return GOSHARE_MEDIA_FILE_SAVE_DIR + random.nextInt(10000) + "_" + fileName;
    }
    
    public static boolean isExistsFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        File file = new File(fileName);
        return file.exists();
    }

    /**
	 * 判断文件内容是否为空
	 * 
	 * @param file
	 * @return
	 */
	public static boolean isFileEmpty(String file) {
		FileReader fr = null;
		try {
			fr = new FileReader(file);
			if (fr.read() == -1) {
				fr.close();
				return true;
			} else {
				fr.close();
				return false;
			}
		} catch (Exception e) {
			return true;
		}
	}
	
	
	// String.format("%.2f", Float.valueOf(size)/1024/1024);
	public static String formetFileSize(long fileS) {// 转换文件大小
		DecimalFormat df = new DecimalFormat("#.#");// #代表数字
		String fileSizeString = "";
		if (fileS < 1024) {
			fileSizeString = df.format((float) fileS) + "B";
		} else if (fileS < 1048576) {
			fileSizeString = df.format((float) fileS / 1024) + "KB";
		} else if (fileS < 1073741824) {
			fileSizeString = df.format((float) fileS / 1048576) + "MB";
		} else {
			fileSizeString = df.format((float) fileS / 1073741824) + "GB";
		}
		return fileSizeString;
	}

    /**
     * 获取文件的名称
     * @param pathName
     * @return
     */
    public static String getFileName(String pathName){
        if(TextUtils.isEmpty(pathName)) return null;
        String name = pathName.substring(pathName.lastIndexOf(File.separator) + 1, pathName.length());
        return name;
    }

    /**
     * 获取父文件的路径
     * @param pathName
     * @return
     */
    public static String getParentFilePath(String pathName){
        if(TextUtils.isEmpty(pathName)) return null;
        return (pathName.substring(0, pathName.lastIndexOf(File.separator)));
    }
}
