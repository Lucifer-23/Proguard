package com.mik.mikdex;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.net.Proxy;
import java.net.ProxySelector;

import javax.net.ssl.HostnameVerifier;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Test test = new Test();
        test.setProxy(new Proxy(Proxy.Type.HTTP, null));
        ProxySelector.getDefault();
    }
}