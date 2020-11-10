package net.i09158knct.android.browserp.browser

import android.content.Context
import android.util.Log
import android.webkit.WebView
import net.i09158knct.android.browserp.Util
import kotlin.math.min

class Browser(val context: Context) {
    // シングルトンにする。
    companion object {
        private var instance: Browser? = null
        fun getInstance(context: Context): Browser {
            if (instance == null) instance = Browser(context)
            return instance!!
        }
    }

    var homeUrl: String = "https://www.google.com"
    var searchUrl: String = "https://www.google.com/search?q="
    var isJsEnabled: Boolean = false
        get
        set(value) {
            field = value
            tabs.forEach { it.webview.settings.javaScriptEnabled = value }
        }

    var isImageEnabled: Boolean = true
        get
        set(value) {
            field = value
            tabs.forEach { it.webview.settings.loadsImagesAutomatically = value }
        }

    val tabs = mutableListOf<Tab>()
    val listeners = mutableListOf<IEventListener>()
    var foregroundTab: Tab? = null
    private  var initialized = false

    fun initializeIfNeeded() {
        if (initialized) return
        restoreState()
        initialized = true
    }

    fun saveState() {
        // 現在のタブ一覧を"タブ番号,URL,タイトル"のSetに変換する。
        val setIndexAndUrlAndTitle = tabs
            .mapIndexed { i, tab ->
                // タイトルが長すぎるなら省略する。
                var title = tab.webview.title
                if (title.length > 20) {
                    title = title.take(20) + Typography.ellipsis
                }
                "$i,${tab.webview.url},${title}"
            }
            .toSet()

        // SharedPreferencesに保存する。
        context.getSharedPreferences("browser", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("tabs", setIndexAndUrlAndTitle)
            .apply()

        // デバッグ用にログ出力する。
        if (setIndexAndUrlAndTitle.isNotEmpty()) {
            Log.d(Util.tag, "saved\n" + setIndexAndUrlAndTitle.reduce { s, acc -> "$s\n$acc" })
        }
    }

    fun restoreState() {
        // タブ一覧を復元する。
        val pref = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val data = pref.getStringSet("tabs", setOf())!!
        val urls = data
            .map { it.split(",") }
            .sortedBy { it[0].toInt() }
            .map { it[1] }
        if (urls.isNotEmpty()) Log.d(Util.tag, "restored\n" + urls.reduce { s, acc -> "$s\n$acc" })
        urls.forEach {
            addNewTab().apply { webview.loadUrl(it) }
        }
    }

    fun changeForeground(tab: Tab) {
        // プロパティを更新する。
        val oldTab = foregroundTab
        foregroundTab = tab

        // コールバックを呼び出す。
        listeners.forEach {
            it.onForegroundTabChanged(oldTab, tab)
            it.onTitleChanged(tab, tab.webview.title)
            it.onUrlChanged(tab, tab.webview.url)
            it.onProgressChanged(tab, tab.webview.progress)
            it.onBackForwardStateChanged(tab)
        }
    }

    fun openNewTab(url: String): Tab {
        val tab = addNewTab()
        tab.webview.loadUrl(url)
        changeForeground(tab)
        return tab
    }

    fun addNewTab(url: String? = null): Tab {
        val tab = Tab(context, this, isJsEnabled, isImageEnabled, null)
        tabs.add(tab)
        listeners.forEach { it.onTabCountChanged(tabs.count()) }
        return tab
    }

    fun closeTab(tab: Tab) {
        // タブをtabsから削除する。
        val index = tabs.indexOf(tab)
        tabs.remove(tab)

        // 閉じたタブがフォアグラウンドだった場合は別のタブをフォアグラウンドにする。
        // ただし、タブが全部閉じられた場合は何もしない。
        if (foregroundTab == tab && !tabs.isEmpty()) {
            changeForeground(tabs[min(index, tabs.count() - 1)])
        }

        // タブを破棄する。
        tab.destroy()
        // TODO タブ復元機能

        // タブ数変更を通知する。
        listeners.forEach { it.onTabCountChanged(tabs.count()) }
    }

    private val validSchemaList = listOf(
        "http:",
        "https:",
        "javascript:",
        "about:",
        "data:",
        "file:",
        "content:"
    )

    fun query(query: String) {
        val valid = validSchemaList.any { query.startsWith(it) }
        val url = if (valid) query else Util.generateSearchUrl(query, searchUrl)
        foregroundTab?.webview?.loadUrl(url)
    }

    interface IEventListener {
        fun onTabCountChanged(count: Int)
        fun onForegroundTabChanged(oldTab: Tab?, newTab: Tab)

        fun onTitleChanged(tab: Tab, title: String)
        fun onUrlChanged(tab: Tab, url: String)

        fun onPageStarted(tab: Tab)
        fun onPageFinished(tab: Tab)

        fun onProgressChanged(tab: Tab, progress: Int)
        fun onBackForwardStateChanged(tab: Tab)
        fun onReloadStopStateChanged(tab: Tab, loading: Boolean)
    }
}