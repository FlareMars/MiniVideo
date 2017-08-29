package com.jb.zcamera.utils;

/**
 * Action 常量
 * 
 * Created by oujingwen on 15-9-24.
 */
public class ActionConstant {
    /**
     * Action常量定义
     */
    public static class Actions {

        /**
         * 拍照并分享的Action
         */
        public static final String ACTION_IMAGE_CAPTURE_AND_SHARE = "com.steam.photoeditor.action.IMAGE_CAPTURE_AND_SHARE";
        /**
         * 编辑图片并分享的Action
         */
        public static final String ACTION_IMAGE_EDIT_AND_SHARE = "com.steam.photoeditor.action.IAMGE_EDIT_AND_SHARE";
        /**
         * 选择图片并编辑分享的Action
         */
        public static final String ACTION_IMAGE_PICK_TO_EDIT_AND_SHARE = "com.steam.photoeditor.action.IMAGE_PICK_TO_EDIT_AND_SHARE";
        /**
         * 选择图片或动态图片并编辑
         */
        public static final String ACTION_PICK_TO_EDIT = "com.steam.photoeditor.action.PICK_TO_EDIT";
        /**
         * 选择图片并进行画中画编辑
         */
        public static final String ACTION_PICK_TO_PIP_EDIT = "com.steam.photoeditor.action.PICK_TO_PIP_EDIT";
        /**
         * 预览动态图片并分享的Action
         */
        public static final String ACTION_MOTION_VIEW_AND_SHARE = "com.steam.photoeditor.action.MOTION_VIEW_AND_SHARE";
        /**
         * 动态图片拍摄并分享的Action
         */
        public static final String ACTION_MOTION_CAPTURE_AND_SHARE = "com.steam.photoeditor.action.MOTION_CAPTURE_AND_SHARE";
        /**
         * 动态图片拍摄的Action
         */
        public static final String ACTION_MOTION_CAPTURE = "com.steam.photoeditor.action.MOTION_CAPTURE";
        /**
         * 进入相册选择图片拼接的Action
         */
        public static final String ACTION_SELECT_IMAGES_TO_COLLAGE = "com.steam.photoeditor.action.SELECT_IMAGE_TO_COLLAGE";
        /**
         * 进入相册选择图片杂志拼接的Action
         */
        public static final String ACTION_SELECT_IMAGES_TO_TEMPLET_COLLAGE = "com.steam.photoeditor.action.SELECT_IMAGE_TO_TEMPLET_COLLAGE";
        /**
         * 进入相册选择锁屏背景
         */
        public static final String ACTION_SELECT_SCREEN_LOCK_BG = "com.steam.photoeditor.action.SELECT_SCREEN_LOCK_BG";
        /**
         * 更换拼接的图片的Action
         */
        public static final String ACTION_CHANGE_IMAGES_TO_COLLAGE = "com.steam.photoeditor.action.CHANGE_IMAGE_TO_COLLAGE";
        /**
         * 拍照或拍摄动态图片编辑并发布的Action
         */
        public static final String ACTION_CAPTURE_TO_EDIT_AND_PUBLISH = "com.steam.photoeditor.action.CAPTURE_TO_EDIT_AND_PUBLISH";
        /**
         * 选择照片或动态图片编辑并发布的Action
         */
        public static final String ACTION_PICK_TO_EDIT_AND_PUBLISH = "com.steam.photoeditor.action.PICK_TO_EDIT_AND_PUBLISH";
        /**
         * 编辑图片并发布的Action
         */
        public static final String ACTION_IMAGE_EDIT_AND_PUBLISH = "com.steam.photoeditor.action.IAMGE_EDIT_AND_PUBLISH";
        public static final String ACTION_MOTION_EDIT_AND_PUBLISH = "com.steam.photoeditor.action.MOTION_EDIT_AND_PUBLISH";
        /**
         * 选择照片编辑并进入Emoji界面的Action
         */
        public static final String ACTION_PICK_TO_ADD_STICKER_EDIT = "com.jb.zcamera.action.PICK_TO_ADD_STICKER_EDIT";

        public static final String ACTION_PICK_TO_TEMPLET_EDIT = "com.jb.zcamera.action.PICK_TO_TEMPLET_EDIT";
        /**
         * 从拍照贴纸进入Emoji界面的Action
         */
        public static final String ACTION_TO_CAMERA_ADD_STICKER_EDIT = "com.jb.zcamera.action.TO_CAMERA_ADD_STICKER_EDIT";

        public static final String ACTION_NOTIFY_TO_RECOMMEND = "com.steam.photoeditor.action.NOTIFY_TO_RECOMMEND";
    }

    /**
     * Extra常量定义
     */
    public static class Extras {
        public static final String EXTRA_FILTER_NAME = "com.steam.photoeditor.extra.FILTER_NAME";
        public static final String EXTRA_TOPIC_ID = "com.steam.photoeditor.extra.TOPIC_ID";
        public static final String EXTRA_URI_ARRAY = "com.steam.photoeditor.extra.URI_ARRAY";
        public static final String EXTRA_PACKAGE_NAME = "com.steam.photoeditor.extra.PACKAGE_NAME";
        public static final String EXTRA_ENTRANCE = "com.steam.photoeditor.extra.ENTRANCE";
        public static final String EXTRA_PAGE = "com.steam.photoeditor.extra.PAGE";
        public static final String EXTRA_BEAUTY_ON = "com.steam.photoeditor.extra.BEAUTY_ON";
        public static final String EXTRA_SHOW_STYLE = "com.steam.photoeditor.extra.SHOW_STYLE";
        public static final String EXTRA_DATASET = "com.steam.photoeditor.extra.DATASET";
    }
}
