package com.gomo.minivideo.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gomo.minivideo.CameraApp;
import com.gomo.minivideo.R;
import com.jb.zcamera.camera.Preview;
import com.jb.zcamera.camera.ProcessVideoService;
import com.jb.zcamera.camera.SensorHelper;
import com.jb.zcamera.camera.SettingsManager;
import com.jb.zcamera.filterstore.bo.LocalFilterBO;
import com.jb.zcamera.imagefilter.FilterAdapter;
import com.jb.zcamera.imagefilter.util.ImageFilterTools;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * Created by ruanjiewei on 2017/8/27
 */

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";

    public static final int MESSAGE_SHOW_ZOOMLAYOUT = 3;
    public static final int MESSAGE_SHOW_TOAST = 10;
    public static final int MESSAGE_UPDATE_GALLARY_ICON = 5;

    private FrameLayout mPreviewContainer;
    private View mPreviewOverlay;
    private TextView mVideoTime = null;
    private TextView mShowTextView = null;
    private ImageView mTakeVideoButton = null;
    private ImageView mCloseFiltersButton = null;
    private ImageView mFiltersButton = null;
    private LinearLayout mFiltersPanel = null;
    private RelativeLayout mBottomButtonsPanel = null;
    private RecyclerView mFiltersListView;

    private int mFiltersPanelHeight;

    private FilterAdapter mFilterAdapter;
    private Handler mHandler = new CameraUIHandler(this);
    private Preview mPreview;

    private int mCurrentOrientation = 0;

    private SensorHelper mSensorHelper;
    private OrientationEventListener mOrientationEventListener = null;

    private SensorEventListener mAccelerometerListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            mPreview.onAccelerometerSensorChanged(event);
        }
    };

    private SensorEventListener mMagneticListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            mPreview.onMagneticSensorChanged(event);
        }
    };

    private View.OnClickListener mTakeVideoButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mPreview.isVideo()) {
                mPreview.toggleTakeButtonClick(false);
            }
        }
    };

    private FilterAdapter.OnItemClickListener mOnFilterClickedListener = new FilterAdapter.OnItemClickListener() {

        @Override
        public void onItemClick(LocalFilterBO item, int position) {
            if (mFilterAdapter.setSelectItemPosition(position)) {
                if (position == FilterAdapter.ORIGINAL_FILTER_POSITION) {
                    mFiltersButton.setImageResource(R.drawable.main_filter_off_normal);
                } else {
                    mFiltersButton.setImageResource(R.drawable.main_filter_on_normal);
                }

                if (position == 0) {
                    mPreview.switchFilter(-1);
                } else {
                    mPreview.switchFilter(position - 1);
                }
            }
        }
    };

    private BroadcastReceiver mVideoProcessFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (ProcessVideoService.ACTION_UPDATE_GALLARY_ICON
                        .equals(intent.getAction())) {
                    if (!mHandler.hasMessages(MESSAGE_UPDATE_GALLARY_ICON)) {
                        mHandler.sendEmptyMessage(MESSAGE_UPDATE_GALLARY_ICON);
                    }
                }
            }
        }
    };

    private static class CameraUIHandler extends Handler {
        private final WeakReference<CameraFragment> mFragmentRef;

        CameraUIHandler(CameraFragment fragment) {
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraFragment fragment = mFragmentRef.get();
            if (fragment != null) {
                switch (msg.what) {
                    case MESSAGE_UPDATE_GALLARY_ICON:
                        fragment.useVideoFile(SettingsManager.getRecentlyVideoFile());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public CameraFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPreviewContainer = view.findViewById(R.id.preview);
        mPreviewOverlay = view.findViewById(R.id.preview_overlay);
        mTakeVideoButton = view.findViewById(R.id.take_video);
        mFiltersListView = view.findViewById(R.id.filter_list_view);
        mCloseFiltersButton = view.findViewById(R.id.close_btn);
        mFiltersPanel = view.findViewById(R.id.panel_filters);
        mFiltersButton = view.findViewById(R.id.filter_button);
        mBottomButtonsPanel = view.findViewById(R.id.panel_main_bottom_buttons);

        initViews(savedInstanceState);
    }

    private void initViews(Bundle savedInstanceState) {
        mPreviewOverlay.setAlpha(0f);
        mPreview = new Preview(this, savedInstanceState, mPreviewContainer, Preview.MODE_VIDEO);
        mPreview.addView();

        mSensorHelper = new SensorHelper(getContext());
        mOrientationEventListener = new OrientationEventListener(CameraApp.getApplication()) {
            @Override
            public void onOrientationChanged(int orientation) {
                CameraFragment.this.onOrientationChanged(orientation);
            }
        };
        mTakeVideoButton.setOnClickListener(mTakeVideoButtonClickedListener);

        List<LocalFilterBO> filterData = ImageFilterTools.getFilterData(getActivity());
        mFilterAdapter = new FilterAdapter(getContext(), filterData, FilterAdapter.MAIN_FILTER_TYPE);
        mFiltersListView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mFiltersListView.setAdapter(mFilterAdapter);
        mFilterAdapter.setOnItemClickListener(mOnFilterClickedListener);

        mCloseFiltersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomButtonsPanel.animate().alpha(1.0f).setDuration(300).start();
                mFiltersPanel.animate().translationY(mFiltersPanelHeight).setDuration(300).start();
            }
        });

        mFiltersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomButtonsPanel.animate().alpha(0.0f).setDuration(300).start();
                mFiltersPanel.animate().translationY(0).setDuration(300).start();
            }
        });

        mFiltersPanelHeight = getResources().getDimensionPixelSize(R.dimen.panel_filters_height);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorHelper.unregisterAccelerometerListener(mAccelerometerListener);
        mSensorHelper.unregisterMagneticListener(mMagneticListener);
        mOrientationEventListener.disable();
        mPreview.onPause();
        getActivity().unregisterReceiver(mVideoProcessFinishedReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mPreview.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPreview.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorHelper.registerAccelerometerListener(mAccelerometerListener);
        mSensorHelper.registerMagneticListener(mMagneticListener);
        mOrientationEventListener.enable();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ProcessVideoService.ACTION_UPDATE_GALLARY_ICON);
        filter.addAction(ProcessVideoService.ACTION_ACTIVITY_RESULT);
        getActivity().registerReceiver(mVideoProcessFinishedReceiver, filter);

        mPreview.onResume();
        layoutUI();
    }

    @Override
    public void onStart() {
        super.onStart();
        mPreview.onStart();
    }

    private void useVideoFile(String filePath) {
        Toast.makeText(getContext(), filePath, Toast.LENGTH_SHORT).show();
    }

    /**
     * 更新录像时间，0为停止录像
     */
    public void updateRecordTime(final long time) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mVideoTime == null) {
                    return;
                }
                long hour = time / 1000 / 60 / 60;
                long minute = (time / 1000 / 60) % 60;
                long second = (time / 1000) % 60;
                String text = String.format(Locale.CHINA, "%02d:%02d:%02d", hour, minute, second);
                mVideoTime.setText(text);
            }
        });
    }

    /**
     * 更新延迟拍摄剩余时间，0为隐藏显示
     */
    public void updateDelayRemainingTime(final int remainingTime) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mShowTextView == null) {
                    return;
                }
                if (remainingTime == 0) {
                    mShowTextView.setVisibility(View.GONE);
                } else {
                    if (mShowTextView.getVisibility() != View.VISIBLE) {
                        mShowTextView.setVisibility(View.VISIBLE);
                    }
                    mShowTextView.setText(remainingTime + "");
                }
            }
        });
    }

    public void showLoadingDialog() {

    }

    public void hideLoadingDialog() {

    }

    public boolean isSeekbarTouching() {
        return false;
    }

    public Handler getShowTextHandler() {
        return mHandler;
    }

    public void showCameraErrorDialog() {

    }

    public void updatePreviewMask() {

    }

    public void initCanvasEmojiView() {

    }

    public void updateZoomBarByPercent(final float percent) {

    }

    public void updateFlashValue(final String flashValue,
                                 final boolean showToast) {

    }

    public void startGallaryLoading() {

    }

    public void startOverlayGone() {

    }

    public void updateFlashButton() {

    }

    public void updateHdrButton(final boolean showToast) {

    }

    public boolean isTakeButtonPressed() {
        //return mTakePhotoButton.isPressed();
        return false;
    }

    public void showToast(String s) {
        Message msg = mHandler.obtainMessage(MESSAGE_SHOW_TOAST, s);
        mHandler.sendMessage(msg);
    }

    public FilterAdapter getFilterAdapter() {
        return mFilterAdapter;
    }

    public void changeUIForStartVideo(boolean isStart) {
        if (isStart) {
            if (mPreview.isVideo()) {
                mFiltersButton.setVisibility(View.INVISIBLE);
                mTakeVideoButton.setImageResource(R.drawable.main_take_video_stop_selector);
            }
        } else {
            if (mPreview.isVideo()) {
                mFiltersButton.setVisibility(View.VISIBLE);
                mTakeVideoButton.setImageResource(R.drawable.main_take_video_selector);
            }
        }
    }

    public void changeUIForResumeVideo(boolean isResume) {

    }

    public void layoutUI() {
        int rotation = getActivity().getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int relative_orientation = (mCurrentOrientation + degrees) % 360;
        int ui_rotation = (360 - relative_orientation) % 360;
        mPreview.setUIRotation(ui_rotation);
    }

    private void onOrientationChanged(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return;
        int diff = Math.abs(orientation - mCurrentOrientation);
        if (diff > 180)
            diff = 360 - diff;
        // only change orientation when sufficiently changed
        if (diff > 60) {
            orientation = (orientation + 45) / 90 * 90;
            orientation = orientation % 360;
            if (orientation != mCurrentOrientation) {
                mCurrentOrientation = orientation;
                Log.d(TAG, "mCurrentOrientation is now: " + mCurrentOrientation);
                layoutUI();
            }
        }
        mPreview.onOrientationChanged(orientation);
    }
}
