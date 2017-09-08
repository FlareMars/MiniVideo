package com.gomo.minivideo.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pixelslab.stickerpe.R;

public class ShareActivity extends AppCompatActivity {

    private static final String PARAM_VIDEO_PATH = "video_path";

    public static Intent getIntent(Context context, String resultFilePath) {
        Intent intent = new Intent(context, ShareActivity.class);
        intent.putExtra(PARAM_VIDEO_PATH, resultFilePath);
        return intent;
    }

    private String mVideoPath;

    private ImageView mThumbnailImageView;
    private ImageView mBackBtn;
    private TextView mTitleTv;
    private Button shareBtn;
    private Button installBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        mVideoPath = getIntent().getStringExtra(PARAM_VIDEO_PATH);
        mThumbnailImageView = (ImageView) findViewById(R.id.image_view);
        mBackBtn = (ImageView) findViewById(R.id.back);
        mTitleTv = (TextView) findViewById(R.id.title);
        shareBtn = (Button) findViewById(R.id.btn_share);
        installBtn = (Button) findViewById(R.id.btn_install);

        initViews();
    }

    private void initViews() {
        mBackBtn.setImageResource(R.drawable.top_panel_back);
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mTitleTv.setText("Share");

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        installBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        if (mVideoPath != null && !"".equals(mVideoPath)) {
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(mVideoPath, MediaStore.Video.Thumbnails.MINI_KIND);
            mThumbnailImageView.setImageBitmap(bitmap);
            mThumbnailImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = VideoViewActivity.getIntent(ShareActivity.this, mVideoPath);
                    startActivity(intent);
                }
            });
        }
    }
}
