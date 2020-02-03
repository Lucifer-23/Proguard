package com.mik.mikdex;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mik.com.mikdex.R;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}