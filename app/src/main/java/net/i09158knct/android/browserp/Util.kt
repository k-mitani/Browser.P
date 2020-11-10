package net.i09158knct.android.browserp


import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import net.i09158knct.android.browserp.browser.Browser
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder

object Util {
    var context: Context? = null
    fun showToast(text: String?): Unit {
        if (context == null) return
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun showToast(id: Int): Unit {
        if (context == null) return
        Toast.makeText(context, id, Toast.LENGTH_SHORT).show()
    }

    val tag: String
        get() {
            if (BuildConfig.DEBUG) {
                val trace = Thread.currentThread().stackTrace[3]
                val shortClassName = trace.className.split('.').last();
                return "${shortClassName}.${trace.methodName}"
            }
            return "*"
        }

    fun debug(tag: String, msg: String): Unit {
        Log.d(tag, msg)
        showToast("${tag} | ${msg}")
    }

    fun shareUrl(a: Activity, url: String) {
        val i = Intent()
        i.setAction(Intent.ACTION_SEND)
        i.setType("text/plain")
        i.putExtra(Intent.EXTRA_TEXT, url)
        a.startActivity(Intent.createChooser(i, a.getString(R.string.menuShare)))
    }

    fun openInOtherBrowser(a: Activity, url: String) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        a.startActivity(Intent.createChooser(i, a.getString(R.string.menuOpenInOtherBrowser)))
    }

    fun generateSearchUrl(searchWord: String, searchUrl: String): String {
        try {
            return searchUrl + URLEncoder.encode(searchWord, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return searchUrl
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
    }
}