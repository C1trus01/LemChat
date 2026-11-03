package com.flxrs.dankchat.ui.main.stream

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

@SuppressLint("SetJavaScriptEnabled")
class StreamWebView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.webViewStyle,
        defStyleRes: Int = 0,
    ) : WebView(context, attrs, defStyleAttr, defStyleRes) {
        init {
            with(settings) {
                javaScriptEnabled = true
                setSupportZoom(false)
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
            }
        }
    }
