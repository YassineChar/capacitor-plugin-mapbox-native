package com.whisperspots.ui

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import com.whisperspots.plugins.MapboxNativePlugin

class WhisperHomeUIManager private constructor() {
    
    private var createWhisperButton: CreateWhisperButton? = null
    private var recenterButton: RecenterButton? = null
    private var rootView: FrameLayout? = null
    private var webView: WebView? = null
    private var mapboxPlugin: MapboxNativePlugin? = null
    private var activity: Activity? = null
    
    private var isSetup = false
    private var isHomeVisible = false
    
    companion object {
        @JvmStatic
        val shared = WhisperHomeUIManager()
    }
    
    fun setup(activity: Activity, webView: WebView, mapboxPlugin: MapboxNativePlugin) {
        if (isSetup) return
        
        this.activity = activity
        this.webView = webView
        this.mapboxPlugin = mapboxPlugin
        this.rootView = activity.window.decorView.findViewById(android.R.id.content)
        
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.addJavascriptInterface(this, "WhisperHomeUIAndroid")
        
        isSetup = true
    }
    
    @JavascriptInterface
    fun initHomeUI() {
        activity?.runOnUiThread {
            if (isHomeVisible) return@runOnUiThread
            
            createNativeUI()
            isHomeVisible = true
        }
    }
    
    @JavascriptInterface
    fun showHomeUI() {
        activity?.runOnUiThread {
            isHomeVisible = true
            createWhisperButton?.visibility = View.VISIBLE
        }
    }
    
    @JavascriptInterface
    fun hideHomeUI() {
        activity?.runOnUiThread {
            isHomeVisible = false
            createWhisperButton?.visibility = View.GONE
            recenterButton?.hide(animated = true)
        }
    }
    
    @JavascriptInterface
    fun showCreateButton() {
        activity?.runOnUiThread {
            if (createWhisperButton == null) {
                createNativeUI()
            }
            createWhisperButton?.visibility = View.VISIBLE
        }
    }
    
    @JavascriptInterface
    fun hideCreateButton() {
        activity?.runOnUiThread {
            createWhisperButton?.visibility = View.GONE
        }
    }
    
    @JavascriptInterface
    fun setCreateButtonLoading(isLoading: Boolean) {
        activity?.runOnUiThread {
            createWhisperButton?.setLoading(isLoading)
        }
    }
    
    @JavascriptInterface
    fun showRecenterButton() {
        activity?.runOnUiThread {
            if (recenterButton == null) {
                createRecenterButton()
            }
            recenterButton?.show(animated = true)
        }
    }
    
    @JavascriptInterface
    fun hideRecenterButton() {
        activity?.runOnUiThread {
            recenterButton?.hide(animated = true)
        }
    }
    
    @JavascriptInterface
    fun cleanupHomeUI() {
        activity?.runOnUiThread {
            isHomeVisible = false
            
            createWhisperButton?.let { rootView?.removeView(it) }
            createWhisperButton = null
            
            recenterButton?.let { rootView?.removeView(it) }
            recenterButton = null
        }
    }
    
    @JavascriptInterface
    fun bringUIToFront() {
        activity?.runOnUiThread {
            createWhisperButton?.bringToFront()
            recenterButton?.bringToFront()
        }
    }
    
    private fun createNativeUI() {
        if (createWhisperButton != null) return
        
        activity?.runOnUiThread {
            val context = activity ?: return@runOnUiThread
            val root = rootView ?: return@runOnUiThread
            
            val safeAreaBottom = getNavigationBarHeight()
            val buttonSize = 64.dpToPx(context)
            val buttonMargin = 20.dpToPx(context)
            
            createWhisperButton = CreateWhisperButton(context).apply {
                val layoutParams = FrameLayout.LayoutParams(
                    buttonSize,
                    buttonSize
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = safeAreaBottom + 90.dpToPx(context) + buttonMargin
                }
                
                this.layoutParams = layoutParams
                this.elevation = 16f
                
                onButtonClick = {
                    notifyAngularCreateButtonClicked()
                }
            }
            
            root.addView(createWhisperButton)
            createWhisperButton?.bringToFront()
        }
    }
    
    private fun createRecenterButton() {
        if (recenterButton != null) return
        
        activity?.runOnUiThread {
            val context = activity ?: return@runOnUiThread
            val root = rootView ?: return@runOnUiThread
            
            val safeAreaBottom = getNavigationBarHeight()
            val buttonSize = 48.dpToPx(context)
            val buttonMargin = 20.dpToPx(context)
            
            recenterButton = RecenterButton(context).apply {
                val layoutParams = FrameLayout.LayoutParams(
                    buttonSize,
                    buttonSize
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    bottomMargin = safeAreaBottom + 90.dpToPx(context) + buttonMargin
                    marginEnd = buttonMargin
                }
                
                this.layoutParams = layoutParams
                this.elevation = 16f
                
                onButtonClick = {
                    notifyAngularRecenterClicked()
                }
            }
            
            root.addView(recenterButton)
            recenterButton?.bringToFront()
        }
    }
    
    private fun notifyAngularCreateButtonClicked() {
        activity?.runOnUiThread {
            webView?.evaluateJavascript(
                "if (window.__whisperCreateButtonClick) window.__whisperCreateButtonClick();",
                null
            )
        }
    }
    
    private fun notifyAngularRecenterClicked() {
        activity?.runOnUiThread {
            webView?.evaluateJavascript(
                "if (window.__whisperRecenterClick) window.__whisperRecenterClick();",
                null
            )
        }
    }
    
    private fun getNavigationBarHeight(): Int {
        val context = activity ?: return 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }
    
    private fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
