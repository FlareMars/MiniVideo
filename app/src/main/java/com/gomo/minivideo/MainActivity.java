package com.gomo.minivideo;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;

import com.gomo.minivideo.camera.CameraFragment;
import com.jb.zcamera.gallery.util.FileUtil;
import com.jb.zcamera.utils.ZipUtils;

import java.io.File;
import java.io.FileInputStream;

import static com.jb.zcamera.gallery.util.FileUtil.ANIMATION_BG_DIR;

public class MainActivity extends AppCompatActivity {

    public static String ANIM_BGS_DIR;

    private static final int REQUEST_CAMERA = 1;
    private RelativeLayout mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSupportFragmentManager().beginTransaction().replace(R.id.content_layout, new CameraFragment()).commit();
            }
        }
    }
}
