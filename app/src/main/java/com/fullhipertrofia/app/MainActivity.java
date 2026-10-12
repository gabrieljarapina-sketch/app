package com.fullhipertrofia.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private WebView wv;
    private PermissionRequest pending;
    private volatile float stepTotal = -1;
    private boolean stepsOn = false;
    private SensorManager sm;
    private Sensor stepSensor;
    private final SensorEventListener stepListener = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent e) { if (e.values.length > 0) stepTotal = e.values[0]; }
        @Override public void onAccuracyChanged(Sensor s, int a) {}
    };

    private void registerSteps() {
        if (stepsOn || stepSensor == null) return;
        stepsOn = sm.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        wv = new WebView(this);
        setContentView(wv);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            pending = r;
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                        } else {
                            r.grant(r.getResources());
                        }
                    }
                });
            }
        });

        wv.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void startSteps() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (Build.VERSION.SDK_INT >= 29
                            && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 3);
                        } else {
                            registerSteps();
                        }
                    }
                });
            }
            @JavascriptInterface
            public void notifyNow(final String t) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (Build.VERSION.SDK_INT >= 33
                            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
                            return;
                        }
                        ReminderReceiver.show(MainActivity.this, t);
                    }
                });
            }
            @JavascriptInterface
            public int steps() { return (int) stepTotal; }
            @JavascriptInterface
            public void setReminders(final boolean on) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (on && Build.VERSION.SDK_INT >= 33
                            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
                        }
                        Reminders.set(MainActivity.this, on);
                    }
                });
            }
        }, "AndroidFH");

        wv.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 3 && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) registerSteps();
        if (code == 1 && pending != null) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) pending.grant(pending.getResources());
            else pending.deny();
            pending = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (wv != null && wv.canGoBack()) wv.goBack();
        else super.onBackPressed();
    }
}
