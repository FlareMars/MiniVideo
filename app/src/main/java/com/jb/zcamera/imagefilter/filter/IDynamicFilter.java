package com.jb.zcamera.imagefilter.filter;

/**
 * 动态滤镜接口
 *
 * Created by oujingwen on 15-12-2.
 */
public interface IDynamicFilter {
    /**
     * 开启更新动态滤镜侦
     *
     * @param on
     */
    void setUpdateOn(boolean on);

    /**
     * 是否开启更新动态滤镜侦
     *
     * @return
     */
    boolean isUpdateOn();
}
