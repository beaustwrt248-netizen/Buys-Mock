package com.buysloans.hub

import android.content.Context
import android.content.Intent

object UniversalBuyEntryPoints {
    const val EXTRA_QUERY = "morley.universal_buy.query"
    const val EXTRA_SOURCE = "morley.universal_buy.source"

    fun open(context: Context, query: String? = null, source: String = "dashboard") {
        context.startActivity(Intent(context, UniversalBuySearchActivity::class.java).apply {
            query?.trim()?.takeIf { it.isNotEmpty() }?.let { putExtra(EXTRA_QUERY, it) }
            putExtra(EXTRA_SOURCE, source)
        })
    }
}
