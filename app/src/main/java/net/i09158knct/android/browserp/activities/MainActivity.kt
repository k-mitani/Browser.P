package net.i09158knct.android.browserp.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import net.i09158knct.android.browserp.R
import net.i09158knct.android.browserp.Util
import net.i09158knct.android.browserp.browser.Browser
import net.i09158knct.android.browserp.browser.Tab
import net.i09158knct.android.browserp.databinding.MainActivityBinding
import net.i09158knct.android.browserp.views.SwipeLinearLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_SELECT_TAB: Int = 0x001
    }

    inner class BrowserEventListener : Browser.IEventListener {
        override fun onTabCountChanged(count: Int) {
            binding.btnTab.text = count.toString()
        }

        override fun onForegroundTabChanged(oldTab: Tab?, newTab: Tab) {
            binding.grpWebViewContainer.addView(
                newTab.webview,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            newTab.webview.requestFocus()
            registerForContextMenu(newTab.webview)

            // まだ読み込み開始されていなければ読み込みを行う。
            newTab.startLoadingIfNeeded()

            // スワイプでタブを切り替えるときにちらつくのを防ぐために
            // 適当に待機してから削除する。
            Handler().postDelayed({
                if (oldTab != null) {
                    binding.grpWebViewContainer.removeView(oldTab.webview)
                    unregisterForContextMenu(oldTab.webview)
                }
            }, 10)
        }

        override fun onTitleChanged(tab: Tab, title: String) {
            if (tab == browser.foregroundTab) {
                binding.btnTitle.text = title
            }
        }

        override fun onUrlChanged(tab: Tab, url: String) {
            if (tab == browser.foregroundTab) {
                binding.inputUrl.setText(url)
            }
        }

        override fun onPageStarted(tab: Tab) {
            if (tab == browser.foregroundTab) {
                val view = currentFocus
                if (view != null) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
                tab.webview.requestFocus()
                binding.toolbar.visibility = View.VISIBLE
                binding.prgLoadingProgress.progress = 5
                binding.prgLoadingProgress.visibility = View.VISIBLE
            }
            else {
                Util.showToast("Loading...")
            }
        }

        override fun onPageFinished(tab: Tab) {
            if (tab == browser.foregroundTab) {
                binding.prgLoadingProgress.visibility = View.INVISIBLE
                if (toolbarHelper.canHide()) {
                    binding.toolbar.visibility = View.INVISIBLE
                }
            }
            else {
                Util.showToast("Loaded.")
            }
        }

        override fun onProgressChanged(tab: Tab, progress: Int) {
            if (tab == browser.foregroundTab) {
                binding.prgLoadingProgress.progress = progress
                binding.prgLoadingProgress.visibility =
                    if (progress == 100) View.INVISIBLE
                    else View.VISIBLE
            }
        }

        override fun onBackForwardStateChanged(tab: Tab) {
            if (tab == browser.foregroundTab) {
                binding.btnBack.isEnabled = tab.webview.canGoBack()
                binding.btnForward.isEnabled = tab.webview.canGoForward()
            }
        }

        override fun onReloadStopStateChanged(tab: Tab, loading: Boolean) {
            if (tab == browser.foregroundTab) {
                binding.btnReload.visibility = if (loading) View.GONE else View.VISIBLE
                binding.btnStop.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
    }

    lateinit var browser: Browser
    lateinit var binding: MainActivityBinding
    lateinit var toolbarHelper: ToolBarHelper
    lateinit var bottombarHelper: BottomBarHelper
    val browserEventListener = BrowserEventListener()

    private var selectedWebNodeExtra: String? = null
    private var selectedWebNodeUrl: String? = null
    private var selectedWebNodeImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Util.context = applicationContext

        // ブラウザの初期化を行う。
        browser = Browser.getInstance(applicationContext)
        browser.listeners.add(browserEventListener)
        browser.initializeIfNeeded()

        // 画面の初期化を行う。
        toolbarHelper = ToolBarHelper()
        toolbarHelper.initialize()
        bottombarHelper = BottomBarHelper()
        bottombarHelper.initialize()

        // デフォルトのURLを読み込む。
        val initialUrl = intent?.dataString ?: browser.homeUrl
        browser.openNewTab(initialUrl)
    }

    override fun onPause() {
        super.onPause()
        browser.saveState()
    }

    override fun onDestroy() {
        toolbarHelper.onDestroy()
        browser.listeners.remove(browserEventListener)
        val foregroundWebView = browser.foregroundTab?.webview
        if (foregroundWebView?.parent != null) {
            (foregroundWebView.parent as ViewGroup).removeView(foregroundWebView)
        }
        super.onDestroy()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        if (menu == null) return
        if (v == null) return

        // WebView上の要素のコンテキストメニューの作成の場合
        if (v is WebView) {
            val node = v.hitTestResult

            // 意味のない場所やテキストボックスの場合はなにもしない。
            val type = node.type
            if (type == WebView.HitTestResult.UNKNOWN_TYPE ||
                type == WebView.HitTestResult.EDIT_TEXT_TYPE
            ) {
                return
            }

            // リンクの中身を取得する。
            selectedWebNodeExtra = node.extra

            // リンクのURLを取得する。
            selectedWebNodeUrl =
                    // 普通のリンクの場合
                if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) node.extra
                // 画像リンクの場合
                else if (type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    val handler = Handler()
                    val message = handler.obtainMessage()
                    browser.foregroundTab!!.webview.requestFocusNodeHref(message)
                    message.data.getString("url")
                }
                // その他ならリンクなし
                else null

            // 画像URLを取得する。
            selectedWebNodeImageUrl =
                    // 普通の画像の場合
                if (type == WebView.HitTestResult.IMAGE_TYPE) node.extra
                // 画像リンクの場合
                else if (type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) node.extra
                // その他なら画像なし
                else null

            // コンテキストメニューを表示する。
            menuInflater.inflate(R.menu.main_web_context, menu)
            menu.run {
                setHeaderTitle(selectedWebNodeUrl ?: selectedWebNodeExtra)
                setGroupVisible(R.id.menugAnchor, selectedWebNodeUrl != null)
                setGroupVisible(R.id.menugImage, selectedWebNodeImageUrl != null)
                setGroupVisible(R.id.menugPhone, type == WebView.HitTestResult.PHONE_TYPE)
                setGroupVisible(R.id.menugMail, type == WebView.HitTestResult.EMAIL_TYPE)
                setGroupVisible(R.id.menugGeo, type == WebView.HitTestResult.GEO_TYPE)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val extra = selectedWebNodeExtra
        val url = selectedWebNodeUrl
        val imageUrl = selectedWebNodeImageUrl
        val defaultValue = url ?: imageUrl ?: extra ?: return super.onContextItemSelected(item)

        when (item.itemId) {
            R.id.menuShare -> Util.shareUrl(this, defaultValue)
            R.id.menuOpenInNewTab -> {
                browser.openNewTab(url!!)
                binding.toolbar.visibility = View.VISIBLE
            }
            R.id.menuOpenInBackground -> browser.addNewTab(url!!)
            R.id.menuOpenInOtherBrowser -> Util.openInOtherBrowser(this, url!!)
            R.id.menuCopyUrl -> {
                Util.copyToClipboard(this, defaultValue)
                Util.showToast(R.string.copied)
            }
            R.id.menuImageOpenInNewTab -> browser.openNewTab(imageUrl!!)
            R.id.menuImageOpenInBackground -> browser.addNewTab(imageUrl!!)
            R.id.menuImageOpenInOtherBrowser -> Util.openInOtherBrowser(this, imageUrl!!)
            R.id.menuImageCopyUrl -> {
                Util.copyToClipboard(this, imageUrl!!)
                Util.showToast(R.string.copied)
            }
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // TabListActivityから戻ってきた場合。
        if (requestCode == REQUEST_SELECT_TAB) {
            if (resultCode == RESULT_OK) {
                // 選ばれたタブをフォアグラウンドにする。
                val tabIndex = data!!.getIntExtra(TabListActivity.EXTRA_SELECTED_TAB_INDEX, 0)
                val tab = browser.tabs[tabIndex]
                if (tab != browser.foregroundTab) {
                    browser.changeForeground(tab)
                }
            }
            // 戻るボタンなどで戻ってきた場合はなにもしない。

            // タブがひとつもない場合は新しくタブを開く。
            if (browser.tabs.isEmpty()) {
                browser.openNewTab(browser.homeUrl)
            }
            return
        }

        // 知らない場所から戻ってきた場合。
        Util.debug(Util.tag, "Unhandled Activity Result: $requestCode $resultCode")
        return super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent?) {
        // 他のアプリなどからURLを渡された場合の処理。
        if (intent?.dataString != null) {
            // 新しくタブを作ってURLを読み込んで表示する。
            browser.openNewTab(intent.dataString!!)
        }

        setIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onBackPressed() {
        // ページバック可能ならページバックする。
        if (browser.foregroundTab!!.webview.canGoBack()) {
            browser.foregroundTab!!.webview.goBack()
            return
        }

        // もうこれ以上戻れないならタブを閉じる。
        browser.closeTab(browser.foregroundTab!!)
        Util.showToast(R.string.tabClosed)

        // 全てのタブを閉じた場合はアプリを閉じる。
        if (browser.tabs.isEmpty()) {
            super.onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            // 音量キーの操作でPageUp/PageDownする。
            KeyEvent.KEYCODE_VOLUME_UP -> {
                browser.foregroundTab?.webview?.run {
                    scrollTo(scrollX, max(scrollY - height / 5, 0))
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                browser.foregroundTab?.webview?.run {
                    scrollTo(scrollX, scrollY + height / 5)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    inner class ToolBarHelper {
        private lateinit var topwrapper: TopWrapper

        fun initialize() {
            topwrapper = TopWrapper()
            windowManager.addView(topwrapper, topwrapper.windowParams)

            binding.btnTitle.setOnClickListener {
                binding.btnTitle.maxLines = if (binding.btnTitle.maxLines == 1) 10 else 1
            }
            binding.btnClearUrl.setOnClickListener {
                binding.inputUrl.text.clear()

                binding.inputUrl.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.inputUrl, InputMethodManager.SHOW_FORCED)
            }
            binding.btnPasteUrl.setOnClickListener {
                val text = try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip!!.getItemAt(0).text.toString()
                } catch (e: Exception) {
                    Util.showToast(e.message)
                    return@setOnClickListener
                }

                val start = binding.inputUrl.selectionStart
                val end = binding.inputUrl.selectionEnd
                binding.inputUrl.text.replace(min(start, end), max(start, end), text)
            }
            binding.btnEnterUrl.setOnClickListener {
                val text = binding.inputUrl.text.toString()
                if (text.isEmpty()) return@setOnClickListener
                browser.query(text)
            }
            binding.inputUrl.setOnFocusChangeListener { _, focused ->
                if (focused) binding.grpEditPanel.visibility = View.VISIBLE
                else binding.grpEditPanel.visibility = View.GONE
            }
            binding.inputUrl.setOnKeyListener { _: View, keyCode: Int, keyEvent: KeyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                    browser.query(binding.inputUrl.text.toString())
                    return@setOnKeyListener true
                }
                else {
                    return@setOnKeyListener false
                }
            }
        }

        fun canHide(): Boolean {
            val notFocused = binding.inputUrl.findFocus() == null
            val notTouched = !topwrapper.touhed
            return notFocused && notTouched
        }

        fun hide() {
            binding.grpEditPanel.visibility = View.GONE
            binding.toolbar.visibility = View.INVISIBLE
            topwrapper.visibility = View.INVISIBLE
            topwrapper.touhed = false
        }

        fun isShowing(): Boolean {
            return binding.toolbar.visibility == View.VISIBLE
        }

        fun show() {
            binding.grpEditPanel.visibility = View.VISIBLE
            binding.toolbar.visibility = View.VISIBLE
            topwrapper.visibility = View.VISIBLE
            binding.toolbar.measure(0, 0)
            topwrapper.height = binding.toolbar.measuredHeight
            topwrapper.touhed = false
        }

        fun onDestroy() {
            windowManager.removeViewImmediate(topwrapper)
        }

        @SuppressLint("AppCompatCustomView")
        inner class TopWrapper : Button(this@MainActivity) {
            val windowParams = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            var touhed: Boolean = false

            init {
                windowParams.gravity = Gravity.TOP

                visibility = View.INVISIBLE
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                background.alpha = 0
            }

            override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                visibility = View.INVISIBLE
                touhed = true
                bottombarHelper.popup?.dismiss()
                binding.toolbar.dispatchTouchEvent(event)
                touhed = false
                return super.dispatchTouchEvent(event)
            }
        }
    }

    inner class BottomBarHelper {
        var popup: PopupMenu? = null

        fun initialize() {
            // 左右にスワイプされたら前後のタブに切り替える。
            binding.grpButtons.onSwipeListener = object : SwipeLinearLayout.OnSwipeListener {
                override fun onSwipe(
                    view: View,
                    ev1: MotionEvent?,
                    ev2: MotionEvent?,
                    vx: Float,
                    vy: Float
                ): Boolean {
                    // 上下の移動量の方が多ければ何もしない。
                    if (abs(vx) < abs(vy)) return true

                    val tabOffset =
                        if (vx < 0) +1
                        else -1
                    val currentTabIndex = browser.tabs.indexOf(browser.foregroundTab)
                    val targetTabIndex = currentTabIndex + tabOffset
                    // 範囲外なら何もしない。
                    if (targetTabIndex < 0 || targetTabIndex >= browser.tabs.size) return true
                    // タブを切り替える。
                    val targetTab = browser.tabs[targetTabIndex]
                    browser.changeForeground(targetTab)
                    Util.showToast(if (tabOffset > 0) "→" else "←")
                    return true
                }
            }

            binding.btnBack.setOnClickListener { browser.foregroundTab!!.webview.goBack() }
            binding.btnForward.setOnClickListener { browser.foregroundTab!!.webview.goForward() }
            binding.btnReload.setOnClickListener { browser.foregroundTab!!.webview.reload() }
            binding.btnStop.setOnClickListener { browser.foregroundTab!!.webview.stopLoading() }
            binding.btnShare.setOnClickListener {
                Util.shareUrl(
                    this@MainActivity,
                    browser.foregroundTab!!.url
                )
            }
            binding.btnBookmark.setOnClickListener { }
            binding.btnTab.setOnClickListener {
                val intent = Intent(applicationContext, TabListActivity::class.java)
                startActivityForResult(intent, REQUEST_SELECT_TAB)
            }
            // 長押しなら新しいタブを開く。
            binding.btnTab.setOnLongClickListener {
                browser.openNewTab(browser.homeUrl)
                return@setOnLongClickListener true
            }
            binding.btnMenu.setOnClickListener {
                // ツールバーが表示中なら閉じる。
                if (toolbarHelper.isShowing()) {
                    toolbarHelper.hide()
                }
                // ツールバーが非表示ならツールバーを表示しつつ、
                // ポップアップメニューも表示する。
                else {
                    toolbarHelper.show()
                    popup = PopupMenu(this@MainActivity, binding.btnMenu).apply {
                        menuInflater.inflate(R.menu.main_tool, menu)
                        menu.findItem(R.id.menuJsEnable).isVisible = !browser.isJsEnabled
                        menu.findItem(R.id.menuJsDisable).isVisible = browser.isJsEnabled
                        menu.findItem(R.id.menuImageEnable).isVisible = !browser.isImageEnabled
                        menu.findItem(R.id.menuImageDisable).isVisible = browser.isImageEnabled
                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.menuShare -> Util.shareUrl(
                                    this@MainActivity,
                                    browser.foregroundTab!!.url
                                )
                                R.id.menuOpenInOtherBrowser -> Util.openInOtherBrowser(
                                    this@MainActivity,
                                    browser.foregroundTab!!.url
                                )
                                R.id.menuJsEnable -> browser.isJsEnabled = true
                                R.id.menuJsDisable -> browser.isJsEnabled = false
                                R.id.menuImageEnable -> browser.isImageEnabled = true
                                R.id.menuImageDisable -> browser.isImageEnabled = false
                            }
                            return@setOnMenuItemClickListener false
                        }
                        setOnDismissListener {
                            if (toolbarHelper.canHide()) {
                                toolbarHelper.hide()
                            }
                        }
                    }
                    popup!!.show()
                }
            }
        }
    }
}