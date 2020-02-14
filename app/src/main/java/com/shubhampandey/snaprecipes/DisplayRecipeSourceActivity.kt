package com.shubhampandey.snaprecipes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.google.firebase.analytics.FirebaseAnalytics

class DisplayRecipeSourceActivity : AppCompatActivity() {

    var progressBar: ProgressBar? = null
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_recipe_source)

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // receiving transmitted data
        val intent = intent
        // assigning transmitted info to a variable
        // getStringExtra("key which was used during transmitting info")
        val url = intent.getStringExtra("recipeSourceRecipeURL")

        var wv: WebView? = null

        wv = findViewById(R.id.webviewRecipeSourceInfo)
        progressBar = findViewById(R.id.progressBarRecipeSourceInfo)
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
