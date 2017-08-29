#
#  Copyright (C) 2015 Google, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontusemixedcaseclassnames
-dontoptimize
-dontpreverify
#-obfuscationdictionary ../../../branches/buildtools/dictionaries/compact.txt

-printmapping ZeroCamera.map
-renamesourcefileattribute ZeroCamera
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes JavascriptInterface

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep public class org.apache.lucene.**
-keep class android.** { *;}
-keep class java.** { *;}
-keep class * extends android.os.Bundle  { public *;}
-keep class * extends android.os.Handler { public *;}
-keep class * extends android.os.IBinder { public *;}
-keep class android.**{ *;}
-keep class com.android.**{ *;}
-keep class com.jb.android.**{ *;}
-keep class com.jb.google.**{ *;}
-keep class com.jb.gosms.gif.**{ *;}
-keep class org.**{ *;}

-keep class * implements android.os.Parcelable { *;}
-keep class * implements android.os.Parcelable$* { *;}
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

#<!--使用JNI类不混淆-->
-keepclasseswithmembernames class * {native <methods>;}

#<!-- Android 固有的类及其子类不执行混淆 -->
-keep public class * extends android.app.Activity { public *;}
-keep public class * extends android.app.Service { public *;}
-keep public class * extends android.content.BroadcastReceiver { public *;}
-keep public class * extends android.content.ContentProvider { public *;}
-keep public class * extends android.view.View { public *;}
-keep public class * extends android.app.Application { public *;}
-keep public class * extends android.preference.Preference { public *;}

#<!--Facebook广告不混淆 -->
-keep class com.facebook.ads.** { *;}
-keep class * extends com.facebook.ads.AdListener { *;}
-keep class * extends com.facebook.ads.AdError { *;}
-keep class * extends com.facebook.ads.Ad { *;}
-keep class * extends com.facebook.ads.NativeAd { *;}
#<!--Facebook sdk不混淆 -->
-keep class com.facebook.** { *;}

-keep public class jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary { *;}
-keep public class com.jb.zcamera.image.shareimage.ShareImageTools { *;}
-keep public class com.jb.zcamera.image.BitmapBean { *;}
-keep public class com.jb.zcamera.image.BitmapBean$* { *;}

-keep class com.jb.zcamera.imagefilter.filter.** { *;}
-keep class com.jb.zcamera.imagefilter.util.** { *;}

-keep class com.coremedia.iso.** {*;}
-keep class com.googlecode.mp4parser.** {*;}
-keep class com.tencent.** {*;}

-keep public class com.jb.zcamera.setting.ZeroPlusJsInterface { *;}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.google.android.gms.** { *;}

-keep class com.squareup.picasso.** { *;}
-keep class com.jiubang.commerce.ad.sdk.** { *;}
-keep class com.loopme.** { *;}
-keep class com.squareup.okhttp.** { *;}

-keep class * implements com.coremedia.iso.boxes.Box { *; }
-dontwarn com.coremedia.iso.boxes.**
-dontwarn com.googlecode.mp4parser.authoring.tracks.mjpeg.**
-dontwarn com.googlecode.mp4parser.authoring.tracks.ttml.**

-keep class ly.kite.** { *;}
-keep class com.paypal.android.sdk.** { *;}
-keep class io.card.payment.** { *;}
-keep class com.squareup.picasso.** { *;}
-dontwarn ly.kite.**
-dontwarn com.paypal.android.sdk.**
-dontwarn io.card.payment.**
-dontwarn com.squareup.picasso.**

-keep class io.wecloud.message.** { *;}
-dontwarn io.wecloud.message.**

-keep class com.hp.mss.hpprint.** { *;}
-dontwarn com.hp.mss.hpprint.**

-keep class com.jiubang.commerce.** { *;}
-dontwarn com.jiubang.commerce.**

-keep class com.jb.ga0.commerce.** { *;}
-dontwarn com.jb.ga0.commerce.**

-keep class com.appsflyer.** { *;}
-dontwarn com.appsflyer.**

-keepnames class com.intowow.sdk.*
-keep class com.intowow.sdk.* { public *; }
-keepclassmembers class com.intowow.sdk.* { public *; }

-keep class com.amazonaws.**

-dontwarn com.amazonaws.**

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

#充电锁配置
#DB创建类(使用到反射)
-keep public class com.jiubang.commerce.database.DataBaseHelper{*;}
-keep public class com.jiubang.commerce.ad.sdk.SdkAdSourceListener{*;}
#adsdk混淆配置===END===========

#第3方sdk混淆配置===BEGIN===========
#google play service sdk
-keep public class com.google.ads.** {*;}
-keep public class com.google.android.gms.** {*;}
#facebook sdk
-keep public class com.facebook.ads.** {*;}
#loopme sdk
-keep public class com.loopme.** {*;}
#mobilecore sdk
-keepattributes InnerClasses, EnclosingMethod
-keep class com.ironsource.mobilcore.**{ *; }
#第3方sdk混淆配置===END===========

-keep public class com.jiubang.commerce.ad.AdSdkApi{*;}
-keep public class com.jiubang.commerce.ad.bean.** {*;}
-keep public class com.jiubang.commerce.ad.sdk.** {*;}
-keep public interface com.jiubang.commerce.ad.manager.**{*;}
-keep public class com.jiubang.commerce.utils.StringUtils{*;}
-keep public class com.jiubang.commerce.utils.AdTimer{*;}
-keep public class com.jiubang.commerce.ad.params.**{*;}
-keep public class com.jiubang.commerce.ad.http.bean.BaseModuleDataItemBean{*;}
-keep public class com.jiubang.commerce.ad.http.AdSdkRequestHeader$S2SParams{*;}

-keep class android.support.v4.**{*;}
-keep class com.jb.ga0.commerce.util.**{*;}

-keep class com.jiubang.commerce.chargelocker.statistic.plugin.PluginStatistic{*;}
-keep public interface com.jiubang.commerce.dynamicloadlib.framework.inter.IPluginParamsProxy{*;}
-keep public interface com.jiubang.commerce.dynamicloadlib.framework.inter.IFrameworkCenterCallBack{*;}
-keep class com.jiubang.commerce.dynamicloadlib.**{*;}
-keep public class com.jiubang.commerce.ad.cache.config.**{*;}
-keep public class com.jiubang.commerce.chargelocker.statistic.**{*;}

# 插件并入充电锁包后需要的混淆
-keep public interface com.jiubang.commerce.plugin.interfaces.IMainEntrance{*;}
-keep public class com.commerce.jiubang.dynamicplugin.**.MainEntrance{*;}

#
-keep class com.gau.go.gostaticsdk.**{*;}

-keep public class com.jiubang.commerce.dyload.core.proxy.activity.DyActivityPlugin
-keep public class com.jiubang.commerce.dyload.core.proxy.service.DyServicePlugin
-keep public class * extends com.jiubang.commerce.dyload.core.proxy.activity.DyActivityPlugin
-keep public class * extends com.jiubang.commerce.dyload.core.proxy.service.DyServicePlugin

-keep public interface com.jiubang.commerce.dyload.**{*;}
-keep class com.jiubang.commerce.dyload.**{*;}
-keep class com.jiubang.commerce.dynamicload4net.**{*;}
-keep class com.jiubang.commerce.chargelocker.component.manager.APIDelegate{*;}
# dyload的资源不能混淆

-dontwarn com.jb.ga0.commerce.util.**

-keepattributes Exceptions,InnerClasses,...
-keep class com.jiubang.commerce.daemon.DaemonApplication{*;}
-keep class com.jiubang.commerce.daemon.NativeDaemonBase{*;}
-keep class com.jiubang.commerce.daemon.nativ.NativeDaemonAPI21{*;}
-keep class com.jiubang.commerce.daemon.BootCompleteReceiver{*;}

-keep class com.jb.zcamera.mainbanner.MainBanner{*;}
-keep interface com.jb.zcamera.mainbanner.MainBanner{*;}

#积分墙
-keep interface com.jiubang.commerce.tokencoin.http.AppAdsDataHttpHandler$AppAdsDataRequestListener{*;}
-keep class * implements com.jiubang.commerce.tokencoin.http.AppAdsDataHttpHandler$AppAdsDataRequestListener{*;}
-keep interface * extends com.jiubang.commerce.tokencoin.http.AppAdsDataHttpHandler$AppAdsDataRequestListener{*;}
-keep class * implements com.jiubang.commerce.tokencoin.integralwall.AppAdsDataManager$IAppAdsDataListener{*;}
-keep interface com.jiubang.commerce.tokencoin.integralwall.AppAdsDataManager$IAppAdsDataListener extends com.jiubang.commerce.tokencoin.http.AppAdsDataHttpHandler$AppAdsDataRequestListener{*;}

#推送SDK混淆配置===BEGIN===========
#不混淆序列化的数据类
-keep class com.commerce.notification.main.config.bean.** { *; }
#推送SDK混淆配置===END=============

# ========================tokencoin start=====================
#dyload sdk
-keep class com.jiubang.commerce.dyload.**{*;}
-keep interface com.jiubang.commerce.dyload.**{*;}
-keep enum com.jiubang.commerce.dyload.**{*;}

-keep class com.jiubang.commerce.tokencoin.**{*;}
-keep interface com.jiubang.commerce.tokencoin.**{*;}
-keep enum com.jiubang.commerce.tokencoin.**{*;}

#第三方sdk混淆，接入积分墙必现添加
-keep  class com.gau.go.gostaticsdk.**{*;}
-keep  class com.jb.ga0.commerce.util.**{*;}
-keep class com.jirbo.adcolony.**{*;}
-keep class android.support.v4.**{*;}
-keep class * implements com.google.android.gms.ads.reward.RewardedVideoAdListener{
public *;
}
-keep class com.gau.utils.**
-keepclassmembers class com.gau.utils.** {*;}
-dontwarn com.gau.utils.**

-keep public class com.jiubang.commerce.ad.AdSdkApi{*;}
-keep public class com.jiubang.commerce.ad.bean.** {*;}
-keep public class com.jiubang.commerce.ad.sdk.** {*;}
-keep public interface com.jiubang.commerce.ad.manager.**{*;}
-keep public class com.jiubang.commerce.utils.StringUtils{*;}
-keep public class com.jiubang.commerce.utils.AdTimer{*;}
-keep public class com.jiubang.commerce.utils.AdCompatUtil{*;}
-keep public class com.jiubang.commerce.ad.params.**{*;}
-keep public class com.jiubang.commerce.ad.http.bean.BaseModuleDataItemBean{*;}
-keep public class com.jiubang.commerce.ad.http.AdSdkRequestHeader$S2SParams{*;}
-keep public class com.jiubang.commerce.ad.url.AdUrlPreParseTask{*;}
-keep public interface com.jiubang.commerce.ad.url.AdUrlPreParseTask$ExecuteTaskStateListener{*;}
-keep public class com.jiubang.commerce.ad.http.bean.ParamsBean{*;}

-keep  public class com.jiubang.commerce.ad.AdSdkLogUtils{*;}
-keep  public class com.jiubang.commerce.ad.PresolveUtils{*;}
-keep  public class com.jiubang.commerce.ad.http.bean.BaseIntellAdInfoBean{*;}
-keep  public class com.jiubang.commerce.ad.url.AppDetailsJumpUtil{*;}
-keep public class com.jiubang.commerce.ad.bean.AdInfoBean{*;}
-keep public class com.jiubang.commerce.ad.params.PresolveParams{*;}

#mopub
-keep class com.mopub.common.util.** { *; }
-keep class com.mopub.mobileads.** { *; }
-keep public interface com.mopub.nativeads.NativeAd$MoPubNativeEventListener{*;}
-keep class * implements com.mopub.nativeads.NativeAd$MoPubNativeEventListener{*;}
# ========================tokencoin end=====================