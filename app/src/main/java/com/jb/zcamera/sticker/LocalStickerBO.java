package com.jb.zcamera.sticker;

/**
 * Created by ruanjiewei on 2017/8/30
 */

public class LocalStickerBO {

    private String name;
    private int resourceId;

    public LocalStickerBO() {
        this("", -1);
    }

    public LocalStickerBO(String name, int resourceId) {
        this.name = name;
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
