package com.jb.zcamera.imagefilter;

import android.text.TextUtils;

/**
 * Created by chenfangyi on 16-8-1.
 */
public class FilterConstants {
    /**
     * 是不是内置的PIP
     * @param packageName
     * @return
     */
    public static boolean isInternalFilter(String packageName){
        if(TextUtils.isEmpty(packageName)) return false;
        return true;
    }
}
