package com.example.wesafe

import android.graphics.Bitmap
import android.os.Bundle
import android.text.Html
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.wesafe.dataClasses.Article
import kotlinx.android.synthetic.main.activity_article_view.*

const val KEY_ARTICLE = "article"
class ArticleViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_view)

        val article = intent.getParcelableExtra<Article>(KEY_ARTICLE)?:throw RuntimeException("Must pass article object")

        webView.webViewClient = object: WebViewClient(){
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }

        ivBanner.setImageResource(article.image)
        setSupportActionBar(toolbar)
        collapsingToolbar.title = article.title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        webView.loadUrl(article.link)
    }

}