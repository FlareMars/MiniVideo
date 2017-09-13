package com.gomo.minivideo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.gomo.minivideo.camera.CameraFragment;
import com.jb.zcamera.gallery.util.FileUtil;
import com.jb.zcamera.utils.ZipUtils;
import com.pixelslab.stickerpe.R;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.jb.zcamera.gallery.util.FileUtil.ANIMATION_BG_DIR;

public class MainActivity extends AppCompatActivity {

    public static String ANIM_BGS_DIR;

    private static final int REQUEST_CAMERA = 1;
    private RelativeLayout mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getKeyHash(this, "com.pixelslab.stickerpe");
        mRootView = (RelativeLayout) findViewById(R.id.root);

        initResources();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 权限未被授予
            requestCameraPermission();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_layout, new CameraFragment()).commit();
        }
    }

    private void initResources() {
        String resourceDir = getFilesDir().getAbsolutePath() + File.separator + ANIMATION_BG_DIR + File.separator;
        ANIM_BGS_DIR = resourceDir;
        if (!FileUtil.checkExist(ANIM_BGS_DIR)) {
            try {
                ZipUtils.unzipSteam(getResources().getAssets().open("bgs.zip"), resourceDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            Snackbar.make(mRootView, R.string.permission_camera_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA);
                        }
                    })
                    .show();
        } else {
            // 第一次申请，就直接申请
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }
    }

    /**
     17      * 注意运行的时候，app需要正式的签名
     18      * @param context
     19      * @param packageName  app的包名
     20      */
    public static void getKeyHash(Context context , String packageName ){
        try {
            PackageInfo info = null;
            info = context.getPackageManager().getPackageInfo( packageName , PackageManager.GET_SIGNATURES );
            for ( Signature signature : info.signatures ) {
                MessageDigest messageDigest = null;
                messageDigest = MessageDigest.getInstance("SHA");
                messageDigest.update(signature.toByteArray());
                String hs = Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT) ;
                Log.d("zjy", ""+hs);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSupportFragmentManager().beginTransaction().replace(R.id.content_layout, new CameraFragment()).commitAllowingStateLoss();
            }
        }
    }
}
