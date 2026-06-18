package com.gseterminal.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.gseterminal.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWebView()
        setupSwipeRefresh()
        setupBackPress()
        if (isOnline()) loadApp() else showOffline()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled       = true
                domStorageEnabled       = true
                databaseEnabled         = true
                useWideViewPort         = true
                loadWithOverviewMode    = true
                setSupportZoom(false)
                builtInZoomControls     = false
                mixedContentMode        = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode               = WebSettings.LOAD_DEFAULT
                userAgentString         = "$userAgentString GSETerminal/1.0"
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(settings, true)
            }
            addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    binding.progressBar.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView, url: String) {
                    binding.progressBar.visibility = View.GONE
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) showOffline()
                }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()
                    return when {
                        url.startsWith("file://") || url.startsWith("about:") -> false
                        url.startsWith("https://") || url.startsWith("http://") -> {
                            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
                            true
                        }
                        else -> true
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                    if (newProgress == 100) binding.progressBar.visibility = View.GONE
                }
                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                    android.util.Log.d("GSE_JS", msg.message())
                    return true
                }
            }
        }
    }

    private fun loadApp() {
        binding.errorLayout.visibility = View.GONE
        binding.webView.visibility     = View.VISIBLE
        binding.webView.loadUrl("file:///android_asset/index.html")
    }

    private fun showOffline() {
        binding.webView.visibility  = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false
    }

    fun retryLoad() {
        if (isOnline()) loadApp()
        else Toast.makeText(this, "Still offline. Check your connection.", Toast.LENGTH_SHORT).show()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.gse_green)
            setOnRefreshListener {
                if (isOnline()) { binding.webView.reload(); isRefreshing = false }
                else { isRefreshing = false; showOffline() }
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) binding.webView.goBack() else finish()
            }
        })
    }

    private fun isOnline(): Boolean {
        val cm   = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net  = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun onResume()  { super.onResume();  binding.webView.onResume() }
    override fun onPause()   { super.onPause();   binding.webView.onPause() }
    override fun onDestroy() { binding.webView.destroy(); super.onDestroy() }
}
