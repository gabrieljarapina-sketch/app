package com.fullhipertrofia.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        const val HOST = "appassets.androidplatform.net"
    }

    lateinit var web: WebView
    private lateinit var bridge: Bridge

    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private var filePathCb: ValueCallback<Array<Uri>>? = null
    private var camFile: File? = null
    private var permDone: ((Map<String, Boolean>) -> Unit)? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val cb = permDone
            permDone = null
            cb?.invoke(result)
        }

    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val cb = filePathCb
            filePathCb = null
            val cf = camFile
            camFile = null
            val data = res.data
            val out: Array<Uri>? =
                if (res.resultCode == RESULT_OK &&
                    (data == null || (data.data == null && data.clipData == null)) &&
                    cf != null && cf.length() > 0
                ) arrayOf(Uri.fromFile(cf))
                else WebChromeClient.FileChooserParams.parseResult(res.resultCode, data)
            cb?.onReceiveValue(out)
        }

    /** Pide permisos de Android y avisa si todos quedaron concedidos. */
    fun ask(perms: Array<String>, done: (Boolean) -> Unit) {
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            done(true)
            return
        }
        permDone = { r -> done(missing.all { r[it] == true }) }
        permLauncher.launch(missing.toTypedArray())
    }

    private fun hideCustomView() {
        val v = customView ?: return
        (v.parent as? ViewGroup)?.removeView(v)
        customView = null
        customCb?.onCustomViewHidden()
        customCb = null
        web.visibility = View.VISIBLE
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        web.keepScreenOn = true
        setContentView(web)
        bridge = Bridge(this)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        web.addJavascriptInterface(bridge, "AndroidFH")

        val loader = WebViewAssetLoader.Builder()
            .setDomain(HOST)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                if (!request.isForMainFrame) return false
                val u = request.url
                if (u.host == HOST) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, u))
                } catch (_: Exception) {
                }
                return true
            }
        }

        web.webChromeClient = object : WebChromeClient() {

            override fun onPermissionRequest(req: PermissionRequest) {
                runOnUiThread {
                    if (req.origin.host != HOST) {
                        req.deny()
                        return@runOnUiThread
                    }
                    val need = mutableListOf<String>()
                    for (r in req.resources) {
                        if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) need.add(Manifest.permission.CAMERA)
                        if (r == PermissionRequest.RESOURCE_AUDIO_CAPTURE) need.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (need.isEmpty()) {
                        req.deny()
                        return@runOnUiThread
                    }
                    ask(need.toTypedArray()) { ok ->
                        if (ok) req.grant(req.resources) else req.deny()
                    }
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                cb: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCb?.onReceiveValue(null)
                filePathCb = cb
                if (params == null) {
                    filePathCb = null
                    cb?.onReceiveValue(null)
                    return true
                }
                ask(arrayOf(Manifest.permission.CAMERA)) { camOk ->
                    try {
                        val chooser = Intent.createChooser(params.createIntent(), "Elegir foto")
                        camFile = null
                        val wantsImage = params.acceptTypes.isEmpty() ||
                            params.acceptTypes.any { it.isEmpty() || it.startsWith("image") }
                        if (camOk && wantsImage) {
                            val dir = File(cacheDir, "images").apply { mkdirs() }
                            val f = File.createTempFile("cam_", ".jpg", dir)
                            camFile = f
                            val uri = FileProvider.getUriForFile(
                                this@MainActivity, "$packageName.fileprovider", f
                            )
                            val cam = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cam))
                        }
                        fileLauncher.launch(chooser)
                    } catch (e: Exception) {
                        camFile = null
                        val c = filePathCb
                        filePathCb = null
                        c?.onReceiveValue(null)
                    }
                }
                return true
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customCb = callback
                addContentView(
                    view,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                )
                web.visibility = View.GONE
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) hideCustomView() else finish()
            }
        })

        web.loadUrl("https://$HOST/assets/index.html")
    }

    override fun onPause() {
        super.onPause()
        bridge.pauseVoice()
    }

    override fun onDestroy() {
        bridge.shutdown()
        web.destroy()
        super.onDestroy()
    }
}
