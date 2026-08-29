package com.buysloans.admin

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/** Adds a small non-interactive copyright strip to every Admin Activity. */
object AdminCopyrightFooter {
    private const val FOOTER_TAG = "morley_admin_copyright_footer"

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = ensureFooter(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun ensureFooter(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(FOOTER_TAG) != null) return

        val heightPx = activity.resources.displayMetrics.density.times(18f).toInt()
        val text = TextView(activity).apply {
            tag = FOOTER_TAG
            this.text = "© 2026 Beau Stewart. All rights reserved."
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(ColorUtils.setAlphaComponent(0xFF8EA6C4.toInt(), 190))
            setBackgroundColor(0xFF030712.toInt())
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        for (index in 0 until content.childCount) {
            val child = content.getChildAt(index)
            val params = child.layoutParams
            if (params is FrameLayout.LayoutParams && child.tag != FOOTER_TAG) {
                params.bottomMargin = maxOf(params.bottomMargin, heightPx)
                child.layoutParams = params
            }
        }

        content.addView(
            text,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx,
                Gravity.BOTTOM
            )
        )
    }
}
