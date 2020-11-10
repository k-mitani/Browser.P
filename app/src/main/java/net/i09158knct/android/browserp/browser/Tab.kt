package net.i09158knct.android.browserp.browser

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.webkit.*
import android.webkit.WebViewClient
import net.i09158knct.android.browserp.Util

class Tab(
    context: Context,
    val browser: Browser,
    js: Boolean,
    image: Boolean,
    userAgent: String?
) {
    val webview = WebView(context)

    init {
        webview.webChromeClient = CustomWebChromeClient()
        webview.webViewClient = CustomWebViewClient()

        webview.settings.apply {
            allowContentAccess = false // 202011 試しにfalseに変更してみる
            allowFileAccess = false // 202011
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            //        setAppCacheEnabled(false)
            //        setAppCachePath()
            blockNetworkImage = !image
            //        setBlockNetworkLoads(false)
            builtInZoomControls = true // default is false
            //        setCacheMode(WebSettings.LOAD_DEFAULT)
            //        setCursiveFontFamily("cursive")
            //        setDatabaseEnabled(false)
            //        setDatabasePath()
            //        setDefaultFixedFontSize(16)
            //        setDefaultFontSize(16)
            //        setDefaultTextEncodingName("UTF-8")
            //        setDefaultZoom(WebSettings.ZoomDensity.MEDIUM)
            displayZoomControls = false // default is true
            domStorageEnabled = true // default is false
            //        setEnableSmoothTransition(false)
            //        setFantasyFontFamily("fantasy")
            //        setFixedFontFamily("monospace")
            //        setGeolocationDatabasePath()
            //        setGeolocationEnabled()
            //        setJavaScriptCanOpenWindowsAutomatically(false)
            javaScriptEnabled = js
            //        setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS)
            //        setLightTouchEnabled()
            //        setLoadWithOverviewMode(false)
            // loadsImagesAutomatically = image // 202011 setBlockNetworkImage を使うように変更
            //        setMediaPlaybackRequiresUserGesture(true)
            //        setMinimumFontSize(8)
            //        setMinimumLogicalFontSize(8)
            //        setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)
            //        setNeedInitialFocus(true)
            // Sets whether this WebView should raster tiles when it is offscreen but attached
            // to a window. Turning this on can avoid rendering artifacts when animating an
            // offscreen WebView on-screen. Offscreen WebViews in this mode use more memory.
            // The default value is false.
            // Please follow these guidelines to limit memory usage:
            // - WebView size should be not be larger than the device screen size.
            // - Limit use of this mode to a small number of WebViews.
            //   Use it for visible WebViews and WebViews about to be animated to visible.
            //        setOffscreenPreRaster(false)
            //        setPluginState(WebSettings.PluginState.OFF)
            //        setRenderPriority(WebSettings.RenderPriority.NORMAL)
            //        setSansSerifFontFamily("sans-serif")
            // saveFormData = false // default is true // 202011 api26からno effectに
            //        setSavePassword(false) // default is true
            //        setSerifFontFamily("sans-serif") // !!
            //        setStandardFontFamily("sans-serif")
            setSupportMultipleWindows(false) // TODO implements onCreateWindow to set true
            //        setSupportZoom(true)
            //        setTextZoom(100)
            useWideViewPort = true
            setUserAgentString(userAgent)
        }
    }

    inner class CustomWebChromeClient : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            Log.v(Util.tag, "${title}")
            browser.listeners.forEach { it.onTitleChanged(this@Tab, title ?: "(no title)") }
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            Log.v(Util.tag, "${newProgress}")
            browser.listeners.forEach { it.onProgressChanged(this@Tab, newProgress) }
        }
    }

    inner class CustomWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.v(Util.tag, "url: ${url}")
            return false
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            Log.v(Util.tag, "url: $url")
            browser.listeners.forEach {
                it.onPageStarted(this@Tab)
                it.onUrlChanged(this@Tab, url)
                it.onReloadStopStateChanged(this@Tab, true)
                it.onBackForwardStateChanged(this@Tab)
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            Log.v(Util.tag, "url: $url")
            browser.listeners.forEach {
                it.onPageFinished(this@Tab)
                it.onReloadStopStateChanged(this@Tab, false)
                it.onBackForwardStateChanged(this@Tab)
            }
        }

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            Util.debug(Util.tag, "error $errorCode $description $failingUrl")
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError
        ) {
            Util.debug(Util.tag, "ssl error: $error")
        }

        override fun onReceivedHttpAuthRequest(
            view: WebView,
            handler: HttpAuthHandler,
            host: String,
            realm: String
        ) {
            Util.debug(Util.tag, "auth ${host} ${realm}")
        }
    }

    fun destroy() {
        webview.destroy()
    }
}
