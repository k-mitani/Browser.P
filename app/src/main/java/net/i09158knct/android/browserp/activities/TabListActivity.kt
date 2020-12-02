package net.i09158knct.android.browserp.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import net.i09158knct.android.browserp.R
import net.i09158knct.android.browserp.Util
import net.i09158knct.android.browserp.browser.Browser
import net.i09158knct.android.browserp.browser.Tab
import net.i09158knct.android.browserp.databinding.TabItemBinding
import net.i09158knct.android.browserp.databinding.TabListActivityBinding

class TabListActivity : Activity(), TabListAdapter.IEventListener {
    companion object {
        const val EXTRA_SELECTED_TAB_INDEX = "EXTRA_SELECTED_TAB_INDEX"
    }

    lateinit var browser: Browser
    lateinit var binding: TabListActivityBinding
    lateinit var adapter: TabListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TabListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        browser = Browser.getInstance(applicationContext)
        browser.listeners.add(BrowserEventListener())

        adapter = TabListAdapter(this, browser.tabs, this)
        binding.lstTab.adapter = adapter

        binding.btnAddNewTab.setOnClickListener {
            val tab = browser.addNewTab()
            tab.webview.loadUrl(browser.homeUrl)
            adapter.notifyDataSetChanged()
        }

        binding.btnRestoreClosedTab.setOnClickListener {
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
    ArrayAdapter<Tab>(context, R.layout.tab_item, 0, tabs) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: TabItemBinding.inflate(LayoutInflater.from(context)).root
        val tab = getItem(position)!!
        return view.apply {
            view.findViewById<TextView>(R.id.txtTitle).text = tab.title
            view.findViewById<TextView>(R.id.txtUrl).text = tab.url
            view.findViewById<ViewGroup>(R.id.grpTabSelectArea).setOnClickListener { listener.onClickTab(tab) }
            view.findViewById<Button>(R.id.btnClose).setOnClickListener { listener.onClickTabClose(tab) }
        }
    }

    interface IEventListener {
        fun onClickTab(tab: Tab): Unit
        fun onClickTabClose(tab: Tab): Unit
    }
}