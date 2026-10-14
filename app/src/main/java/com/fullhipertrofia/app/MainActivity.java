package com.fullhipertrofia.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {
    static final String HOST = "appassets.androidplatform.net";
    static final int RQ_PERM = 1, RQ_FILE = 2;

    WebView web;
    ValueCallback<Uri[]> fileCb;
    Uri camUri;
    SensorManager sm;
    volatile float stepsVal = -1;
    SpeechRecognizer sr;
    boolean voiceOn = false;
    final Handler h = new Handler(Looper.getMainLooper());
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = getSharedPreferences("fh", 0);
        Notifier.ensureChannel(this);
        pedirPermisos();

        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (!r.isForMainFrame()) return false;
                Uri u = r.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception e) { /* sin app para abrir el link */ }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> need = new ArrayList<>();
                        for (String res : r.getResources()) {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && !has(Manifest.permission.CAMERA))
                                need.add(Manifest.permission.CAMERA);
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && !has(Manifest.permission.RECORD_AUDIO))
                                need.add(Manifest.permission.RECORD_AUDIO);
                        }
                        if (need.isEmpty()) {
                            r.grant(r.getResources());
                        } else {
                            r.deny();
                            requestPermissions(need.toArray(new String[0]), RQ_PERM);
                        }
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                camUri = null;
                String[] types = p.getAcceptTypes();
                boolean img = false, vid = false;
                if (types != null) {
                    for (String t : types) {
                        if (t != null && t.startsWith("image")) img = true;
                        if (t != null && t.startsWith("video")) vid = true;
                    }
                }
                try {
                    if (p.isCaptureEnabled() && img) {
                        File dir = new File(getCacheDir(), "fotos");
                        dir.mkdirs();
                        File f = File.createTempFile("foto", ".jpg", dir);
                        camUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", f);
                        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, camUri);
                        startActivityForResult(i, RQ_FILE);
                        return true;
                    }
                    if (p.isCaptureEnabled() && vid) {
                        startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE), RQ_FILE);
                        return true;
                    }
                } catch (Exception e) {
                    camUri = null; // si falla la cámara, se usa el selector normal
                }
                try {
                    startActivityForResult(p.createIntent(), RQ_FILE);
                } catch (Exception e) {
                    fileCb = null;
                    cb.onReceiveValue(null);
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidFH");

        if (prefs.getBoolean("steps_on", false)) startStepSensor();
        if (prefs.getBoolean("rem", false)) Notifier.scheduleAll(this);

        web.loadUrl("https://" + HOST + "/assets/index.html");
    }

    boolean has(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    void pedirPermisos() {
        List<String> l = new ArrayList<>();
        if (!has(Manifest.permission.CAMERA)) l.add(Manifest.permission.CAMERA);
        if (!has(Manifest.permission.RECORD_AUDIO)) l.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 29 && !has(Manifest.permission.ACTIVITY_RECOGNITION))
            l.add(Manifest.permission.ACTIVITY_RECOGNITION);
        if (Build.VERSION.SDK_INT >= 33 && !has(Manifest.permission.POST_NOTIFICATIONS))
            l.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!l.isEmpty()) requestPermissions(l.toArray(new String[0]), RQ_PERM);
    }

    @Override
    public void onRequestPermissionsResult(int rq, String[] perms, int[] res) {
        super.onRequestPermissionsResult(rq, perms, res);
        if (prefs.getBoolean("steps_on", false)) startStepSensor();
    }

    @Override
    protected void onActivityResult(int rq, int rc, Intent data) {
        super.onActivityResult(rq, rc, data);
        if (rq != RQ_FILE || fileCb == null) return;
        Uri[] out = null;
        if (rc == RESULT_OK) {
            if (camUri != null && (data == null || data.getData() == null)) {
                out = new Uri[]{camUri};
            } else {
                out = WebChromeClient.FileChooserParams.parseResult(rc, data);
            }
        }
        fileCb.onReceiveValue(out);
        fileCb = null;
        camUri = null;
    }

    // ---------- Pasos ----------
    void startStepSensor() {
        if (sm == null) sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor st = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sm.unregisterListener(this);
        if (st != null) sm.registerListener(this, st, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.values != null && e.values.length > 0) stepsVal = e.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor s, int a) { }

    // ---------- Voz ----------
    void js(final String code) {
        web.post(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript(code, null);
            }
        });
    }

    void voiceStart() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            js("window.fhVoiceOff&&window.fhVoiceOff('no')");
            return;
        }
        if (!has(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RQ_PERM);
            js("window.fhVoiceOff&&window.fhVoiceOff('perm')");
            return;
        }
        voiceOn = true;
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle b) { }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float v) { }
                @Override public void onBufferReceived(byte[] b) { }
                @Override public void onEndOfSpeech() { }
                @Override public void onPartialResults(Bundle b) { }
                @Override public void onEvent(int i, Bundle b) { }

                @Override
                public void onResults(Bundle b) {
                    ArrayList<String> a = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (a != null && !a.isEmpty()) {
                        js("window.fhVoice&&window.fhVoice(" + JSONObject.quote(a.get(0)) + ")");
                    }
                    h.postDelayed(new Runnable() { @Override public void run() { voiceListen(); } }, 300);
                }

                @Override
                public void onError(int err) {
                    if (err == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        voiceOn = false;
                        js("window.fhVoiceOff&&window.fhVoiceOff('perm')");
                        return;
                    }
                    long wait = err == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1000 : 400;
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!voiceOn) return;
                            try { sr.cancel(); } catch (Exception e) { }
                            voiceListen();
                        }
                    }, wait);
                }
            });
        }
        voiceListen();
    }

    void voiceListen() {
        if (!voiceOn || sr == null) return;
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { sr.startListening(i); } catch (Exception e) { }
    }

    void voiceStop() {
        voiceOn = false;
        h.removeCallbacksAndMessages(null);
        if (sr != null) {
            try { sr.cancel(); } catch (Exception e) { }
        }
    }

    // ---------- Puente con la página ----------
    class Bridge {
        @JavascriptInterface
        public void setReminders(boolean on) {
            prefs.edit().putBoolean("rem", on).apply();
            if (on) Notifier.scheduleAll(MainActivity.this);
            else Notifier.cancelAll(MainActivity.this);
        }

        @JavascriptInterface
        public void notifyNow(String m) {
            if (m == null) return;
            Notifier.show(MainActivity.this, m, 50 + (m.hashCode() & 0xFFFF));
        }

        @JavascriptInterface
        public void startSteps() {
            prefs.edit().putBoolean("steps_on", true).apply();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 29 && !has(Manifest.permission.ACTIVITY_RECOGNITION)) {
                        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, RQ_PERM);
                    }
                    startStepSensor();
                }
            });
        }

        @JavascriptInterface
        public int steps() {
            float v = stepsVal;
            return v < 0 ? -1 : (int) v;
        }

        @JavascriptInterface
        public void startVoice() {
            runOnUiThread(new Runnable() { @Override public void run() { voiceStart(); } });
        }

        @JavascriptInterface
        public void stopVoice() {
            runOnUiThread(new Runnable() { @Override public void run() { voiceStop(); } });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (voiceOn) {
            voiceStop();
            js("window.fhVoiceOff&&window.fhVoiceOff('pause')");
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        voiceStop();
        if (sr != null) { sr.destroy(); sr = null; }
        if (sm != null) sm.unregisterListener(this);
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
