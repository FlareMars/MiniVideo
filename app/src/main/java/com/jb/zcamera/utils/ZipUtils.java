package com.jb.zcamera.utils;

import com.jb.zcamera.gallery.util.DirectoryUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * 实现将文件压缩为zip包
 * @author hufengyuan
 */
public class ZipUtils {

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
}
