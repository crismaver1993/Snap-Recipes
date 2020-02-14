package com.shubhampandey.snaprecipes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.google.firebase.analytics.FirebaseAnalytics

class DeveloperInfoActivity : AppCompatActivity() {

    var progressBar: ProgressBar? = null
    internal var url = "https://gauravrawat97.github.io/Snap-Recipes/"

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_info)

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        var wv: WebView? = null

        wv = findViewById(R.id.webviewDeveloperInfo)
        progressBar = findViewById(R.id.progressBarDeveloperInfo)
        wv.webViewClient = myWebClient()
        // wv.settings.javaScriptEnabled = true
        wv.settings.builtInZoomControls = true
        wv.settings.displayZoomControls = false
        wv.settings.javaScriptEnabled = true
        wv.loadUrl(url)
    }

    inner class myWebClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            progressBar!!.visibility = View.VISIBLE
            view.loadUrl(url)
            return true
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            progressBar!!.visibility = View.GONE
        }
    }
}
