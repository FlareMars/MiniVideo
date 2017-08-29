package com.gomo.minivideo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gomo.minivideo.camera.CameraFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_layout, new CameraFragment()).commit();
    }
}
