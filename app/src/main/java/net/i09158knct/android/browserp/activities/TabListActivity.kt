package net.i09158knct.android.browserp.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_tab_list.*
import kotlinx.android.synthetic.main.item_tab.view.*
import net.i09158knct.android.browserp.R
import net.i09158knct.android.browserp.Util
import net.i09158knct.android.browserp.browser.Browser
import net.i09158knct.android.browserp.browser.Tab

class TabListActivity : Activity(), TabListAdapter.IEventListener {
    companion object {
        const val EXTRA_SELECTED_TAB_INDEX = "EXTRA_SELECTED_TAB_INDEX"
    }

    lateinit var browser: Browser
    lateinit var adapter: TabListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_list)
        browser = Browser.getInstance(applicationContext)
        browser.listeners.add(BrowserEventListener())

        adapter = TabListAdapter(this, browser.tabs, this)
        lstTab.adapter = adapter

        btnAddNewTab.setOnClickListener {
            val tab = browser.addNewTab()
            tab.webview.loadUrl(browser.homeUrl)
            adapter.notifyDataSetChanged()
        }

        btnRestoreClosedTab.setOnClickListener {
            Util.showToast("TODO")
        }
    }

    override fun onPause() {
        super.onPause()
        browser.saveState()
    }

    override fun onClickTabClose(tab: Tab) {
        browser.closeTab(tab)
        adapter.notifyDataSetChanged()
    }

    override fun onClickTab(tab: Tab) {
        val intent = Intent()
        val pos = browser.tabs.indexOf(tab)
        intent.putExtra(EXTRA_SELECTED_TAB_INDEX, pos)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        browser.listeners.add(BrowserEventListener())
        super.onDestroy()
    }

    inner class BrowserEventListener : Browser.IEventListener {
        override fun onTabCountChanged(count: Int) {
        }

        override fun onForegroundTabChanged(oldTab: Tab?, newTab: Tab) {
        }

        override fun onTitleChanged(tab: Tab, title: String) {
            adapter.notifyDataSetInvalidated()
        }

        override fun onUrlChanged(tab: Tab, url: String) {
            adapter.notifyDataSetInvalidated()
        }

        override fun onPageStarted(tab: Tab) {
        }

        override fun onPageFinished(tab: Tab) {
        }

        override fun onProgressChanged(tab: Tab, progress: Int) {
        }

        override fun onBackForwardStateChanged(tab: Tab) {
        }

        override fun onReloadStopStateChanged(tab: Tab, loading: Boolean) {
        }
    }
}

class TabListAdapter(context: Context, val tabs: List<Tab>, val listener: IEventListener) :
    ArrayAdapter<Tab>(context, R.layout.item_tab, 0, tabs) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: View.inflate(context, R.layout.item_tab, null)
        val tab = getItem(position)!!
        return view.apply {
            txtTitle.text = tab.title
            txtUrl.text = tab.url
            grpTabSelectArea.setOnClickListener { listener.onClickTab(tab) }
            btnClose.setOnClickListener { listener.onClickTabClose(tab) }
        }
    }

    interface IEventListener {
        fun onClickTab(tab: Tab): Unit
        fun onClickTabClose(tab: Tab): Unit
    }
}