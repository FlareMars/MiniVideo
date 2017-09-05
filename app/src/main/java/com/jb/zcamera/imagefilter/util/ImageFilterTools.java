package com.jb.zcamera.imagefilter.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.util.Log;

import com.gomo.minivideo.R;
import com.jb.zcamera.filterstore.bo.LocalFilterBO;
import com.jb.zcamera.filterstore.db.FilterDBUtils;
import com.jb.zcamera.imagefilter.filter.DawnFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageFilterGroup;
import com.jb.zcamera.imagefilter.filter.GPUImageLookupFilter;
import com.jb.zcamera.imagefilter.filter.GPUImagePointDownFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageVignetteFilter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ruanjiewei on 2017/8/27
 */

public class ImageFilterTools {

    public static final String[] FILTER_IMAGE_URL = new String[]{
            "filter_snow",
            "filter_eastern",
            "filter_am",
            "filter_680",
            "filter_breeze",
    };

    public static final String[] FILTER_STRING = new String[]{
            "Snow",
            "Sunrise",
            "Elapse",
            "Quiet",
            "Soft"
    };

    /**
     * 内置资源对应的服务端mapid
     */
    public static final int[] FILTER_MAPID = new int[] {
            102090513,
            12110640,
            12123556,
            12110638,
            12123564
    };

    private static final float[] FLTER_INTENSITY = new float[]{
            0,
            0.7f,
            0.7f,
            0.7f,
            0.7f
    };

    /**
     * 内置资源对应的包名
     */
    public static final String[] FILTER_PACKAGE_NAME = new String[] {
            "com.jb.zcamera.imagefilter.plugins.snow",
            "com.jb.zcamera.imagefilter.plugins.sunrise",
            "com.jb.zcamera.imagefilter.plugins.elapse",
            "com.jb.zcamera.imagefilter.plugins.quiet",
            "com.jb.zcamera.imagefilter.plugins.soft"
    };


    private static final int[] FILTER_RESOURCE = new int[] {
            0,
            R.drawable.sunrise,
            R.drawable.elapse,
            R.drawable.quiet,
            R.drawable.soft
    };

    public static final int[] FILTER_IMAGE = new int[]{
            R.drawable.filter_snow,
            R.drawable.filter_sunrise,
            R.drawable.filter_elapse,
            R.drawable.filter_quiet,
            R.drawable.filter_soft
    };

    /**
     * 老的另外22的ICON的URL map
     */
    public static final HashMap<String, String> OLD_DOWNLOAD_URL_MAP = new HashMap<String, String>(22);

    /**
     * 老的另外32的mapid
     */
    public static final HashMap<String, Integer> OLD_DOWNLOAD_MAP_ID = new HashMap<String, Integer>(32);

    /**
     * 老的另外32的mapid
     */
    public static final HashMap<String, String> OLD_DOWNLOAD_COLOR = new HashMap<String, String>(32);

    public static final HashMap<String, Integer> FILTER_NAME_TO_DRAWABLE_RESOURCE = new HashMap<>();
    static{
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.vitality", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/tnvkiQgs.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.sketch", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/2fnnba.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.lake", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/CdjKpIhV.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.subir", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/mgfw9VqB.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.leve", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/KxBPXcWQ.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.warm", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/LbGCRucP.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.heavy", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/5Z5eGox9.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.dream", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/AO1Rzujs.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.green", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/MFKRI2gf.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.a8", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/v4SBjI9Y.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.a9", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/SBM8iRLz.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.winter", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/XAMDs9pH.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.morning", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150911/QlxHoyyk.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.gedor", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/90ova.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.roman", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/2debhi.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.invert", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/2fngvg.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.reminiscent", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/2eeb64.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.sweet", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/6ifxi.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.lori", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/2rmfa.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.lips", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/6hld3.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.future", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/1q6zw.png");
        OLD_DOWNLOAD_URL_MAP.put("com.jb.zcamera.imagefilter.plugins.lunch", "http://goappdl.goforandroid.com/soft/repository/10/icon/20150910/2debfq.png");

        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.sunrise", 102085728);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.dawn", 102085740);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.elapse", 102085741);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.quiet", 102085726);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.soft", 102085746);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.cool", 102085683);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.pale", 102085724);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.rosy", 102085727);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.wine", 102085749);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.polaroid", 102085732);

        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.vitality", 102085748);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.sketch", 12133683);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.lake", 102085745);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.subir", 102085747);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.leve", 102085722);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.warm", 102085729);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.heavy", 102085688);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.dream", 102085685);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.green", 102085742);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.a8", 102085737);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.a9", 102085738);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.winter", 102085730);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.morning", 102085723);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.gedor", 12133322);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.roman", 12133682);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.invert", 12133323);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.reminiscent", 12133681);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.sweet", 12133684);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.lori", 12133662);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.lips", 12133470);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.future", 12133181);
        OLD_DOWNLOAD_MAP_ID.put("com.jb.zcamera.imagefilter.plugins.lunch", 12133693);


        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.snow", "#a6b3b9");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.sunrise", "#9492db");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.dawn", "#a15913");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.elapse", "#cfc05c");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.quiet", "#c09238");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.soft", "#7f3aa3");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.cool", "#006de7");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.pale", "#823e15");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.rosy", "#ab571f");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.wine", "#461149");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.polaroid", "#5acb9f");

        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.vitality", "#FFA717");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.sketch", "#6c6c6c");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.lake", "#a5480c");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.subir", "#bb9136");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.leve", "#9c440a");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.warm", "#36be89");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.heavy", "#0a9c41");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.dream", "#cedb62");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.green", "#29924d");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.a8", "#ad3f19");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.a9", "#755c92");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.winter", "#36b6ae");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.morning", "#bc37b7");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.gedor", "#855372");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.roman", "#81713b");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.invert", "#3de5e3");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.reminiscent", "#653b49");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.sweet", "#d89876");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.lori", "#a08e57");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.lips", "#622d27");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.future", "#bd985f");
        OLD_DOWNLOAD_COLOR.put("com.jb.zcamera.imagefilter.plugins.lunch", "#864b27");

        for (int i = 0; i < FILTER_IMAGE.length; i++) {
            FILTER_NAME_TO_DRAWABLE_RESOURCE.put(FILTER_STRING[i], FILTER_IMAGE[i]);
        }
    }

    public static final String FILTER_NAME_DEFAULT = "Original";

    /**
     * 解密字符串
     * @param str
     * @return
     */
    public static String getDecryptString(String str){
        return CryptTool.decrypt(str, NativeLibrary.getKString());
    }

    /**
     * 旋转贴图效果
     *
     * @param filter
     * @param rotation
     * @param isBadTwoInputFilter
     */
    public static void rotateFilter(GPUImageFilter filter, Rotation rotation, boolean isBadTwoInputFilter) {
        if (isBadTwoInputFilter) {
            rotateTwoInputFilter(filter, rotation);
        } else {
            filter.setRotation(rotation, false, false);
        }
    }


    public static void rotateTwoInputFilter(GPUImageFilter filter, Rotation rotation) {
        try {
            int twoInputRotation = 0;
            if (rotation == Rotation.ROTATION_90) {
                twoInputRotation = 270;
            } else if (rotation == Rotation.ROTATION_270) {
                twoInputRotation = 90;
            } else if (rotation == Rotation.ROTATION_180) {
                twoInputRotation = 180;
            }
            Class filterCl = filter.getClass();
            if (filterCl.getSuperclass().getSimpleName().equals("GPUImageTwoInputFilter")) {
                Class rotationCl = filterCl.getClassLoader().loadClass(filterCl.getPackage().getName() + ".Rotation");
                Method rotationfromInt = rotationCl.getDeclaredMethod("fromInt", int.class);
                Method setRotationMethod = filterCl.getSuperclass().getDeclaredMethod("setRotation",
                        rotationCl, boolean.class, boolean.class);
                setRotationMethod.invoke(filter, rotationfromInt.invoke(null, twoInputRotation), false, false);
            } else if (filter instanceof GPUImageFilterGroup) {
                GPUImageFilterGroup group = (GPUImageFilterGroup) filter;
                for(GPUImageFilter ft : group.getMergedFilters()) {
                    filterCl = ft.getClass();
                    if (filterCl.getSuperclass().getSimpleName().equals("GPUImageTwoInputFilter")) {
                        Class rotationCl = filterCl.getClassLoader().loadClass(filterCl.getPackage().getName() + ".Rotation");
                        Method rotationfromInt = rotationCl.getDeclaredMethod("fromInt", int.class);
                        Method setRotationMethod = filterCl.getSuperclass().getDeclaredMethod("setRotation",
                                rotationCl, boolean.class, boolean.class);
                        setRotationMethod.invoke(ft, rotationfromInt.invoke(null, twoInputRotation), false, false);
                    } else {
                        rotateHolloweenFilter(ft, twoInputRotation);
                    }
                }
            } else if (filterCl.getSuperclass().getSimpleName().equals("GPUImageFilterGroup")) {
                Method getMergedFiltersMethod = filterCl.getSuperclass().getDeclaredMethod("getMergedFilters");
                List<GPUImageFilter> filters = (List<GPUImageFilter>) getMergedFiltersMethod.invoke(filter);
                for(GPUImageFilter ft : filters) {
                    filterCl = ft.getClass();
                    if (filterCl.getSuperclass().getSimpleName().equals("GPUImageTwoInputFilter")) {
                        Class rotationCl = filterCl.getClassLoader().loadClass(filterCl.getPackage().getName() + ".Rotation");
                        Method rotationfromInt = rotationCl.getDeclaredMethod("fromInt", int.class);
                        Method setRotationMethod = filterCl.getSuperclass().getDeclaredMethod("setRotation",
                                rotationCl, boolean.class, boolean.class);
                        setRotationMethod.invoke(ft, rotationfromInt.invoke(null, twoInputRotation), false, false);
                    } else {
                        rotateHolloweenFilter(ft, twoInputRotation);
                    }
                }
            }
        } catch (Throwable tr) {
            Log.e(ImageFilterTools.class.getName(), "", tr);
        }
    }


    public static void rotateHolloweenFilter(GPUImageFilter filter, int rotation) {
        try {
            Class filterCl = filter.getClass();
            if (filterCl.getSuperclass().getSimpleName().equals("GPUImageFilterGroup")) {
                Method getMergedFiltersMethod = filterCl.getSuperclass().getDeclaredMethod("getMergedFilters");
                List<GPUImageFilter> filters = (List<GPUImageFilter>) getMergedFiltersMethod.invoke(filter);
                for (GPUImageFilter ft : filters) {
                    filterCl = ft.getClass();
                    if (filterCl.getSuperclass().getSimpleName().equals("GPUImageTwoInputFilter")) {
                        Class rotationCl = filterCl.getClassLoader().loadClass(filterCl.getPackage().getName() + ".Rotation");
                        Method rotationfromInt = rotationCl.getDeclaredMethod("fromInt", int.class);
                        Method setRotationMethod = filterCl.getSuperclass().getDeclaredMethod("setRotation",
                                rotationCl, boolean.class, boolean.class);
                        setRotationMethod.invoke(ft, rotationfromInt.invoke(null, rotation), false, false);
                    }
                }
            }
        } catch (Throwable tr) {
            Log.e(ImageFilterTools.class.getName(), "", tr);
        }
    }

    /**
     *
     * @param context
     * @param data 当前的位置(去除了Original)
     * @return
     */
    public static GPUImageFilter createFilterForType(final Context context, LocalFilterBO data) {
        if(data != null){
            int type = data.getType();
            if(type == LocalFilterBO.TYPE_LOCAL_INTERNAL) {
                String url = data.getImageUrl();
                int i = 0;
                for (; i < FILTER_IMAGE_URL.length; i++) {
                    if (FILTER_IMAGE_URL[i].equals(url)) {
                        break;
                    }
                }
                if(i == 0){
                    GPUImageFilterGroup group = new GPUImageFilterGroup();
                    group.addFilter(new GPUImagePointDownFilter());
                    PointF centerPoint = new PointF(0.5f, 0.5f);
                    GPUImageVignetteFilter vignetteFilter = new GPUImageVignetteFilter(centerPoint, new float[] {0.0f, 0.0f, 0.0f}, 0.0f, 1f);
                    group.addFilter(vignetteFilter);
                    return group;
                } else if (i == 2) {
                    return new DawnFilter(context);
                }
                else{
                    GPUImageLookupFilter lookupFilter = new GPUImageLookupFilter();
                    lookupFilter.setBitmap(BitmapFactory.decodeResource(context.getResources(), FILTER_RESOURCE[i]));
                    lookupFilter.setIntensity(FLTER_INTENSITY[i]);
                    return lookupFilter;
                }
            } else if(type == LocalFilterBO.TYPE_ORIGINAL){//如果是Original类型  特殊处理
                return new GPUImageFilter();
            }
        }
        return null;
    }

    public static Bitmap getDecryptBitmap(final Context context, int id){
        InputStream is = null;
        Bitmap bitmap = null;
        ByteArrayInputStream bais = null;
        try {
            is = context.getResources().openRawResource(id);
            byte[] buffer = new byte[is.available()];
            is.read(buffer, 0, is.available());
            byte[] result = CryptTool.decrypt(buffer, NativeLibrary.getKString());
            bais = new ByteArrayInputStream(result);
            bitmap = BitmapFactory.decodeStream(bais);
            return bitmap;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                bais.close();
            } catch (Throwable e) {

            }
        }
        return null;
    }

    public static boolean isBadTwoInputFilter(String packageName) {
        if ("com.jb.zcamera.imagefilter.plugins.halo".equals(packageName)
                || "com.jb.zcamera.imagefilter.plugins.genial".equals(packageName)
                || "com.jb.zcamera.imagefilter.plugins.horror".equals(packageName)
                || "com.jb.zcamera.imagefilter.plugins.pumpkin".equals(packageName)
                || "com.jb.zcamera.imagefilter.plugins.spider".equals(packageName)
                || "com.jb.zcamera.imagefilter.plugins.together".equals(packageName)
                || "com.jb.zcamera.imagefilter.plugins.rainbow".equals(packageName)
                || "com.jb.zcamera.imagefilter.plugins.claw".equals(packageName)) {
            return true;
        }
        return false;
    }

    public static ArrayList<LocalFilterBO> getFilterData(Context context) {
        ArrayList<LocalFilterBO> filterList = FilterDBUtils.getInstance().getLocalFilterListStatusUse();
        LocalFilterBO original = new LocalFilterBO();
        original.setName("Original");
        original.setType(LocalFilterBO.TYPE_ORIGINAL);
        original.setPackageName("com.jb.zcamera.imagefilter.plugins.original");
        filterList.add(0, original);
        return filterList;
    }

}
