package com.iyox.vouch.widget

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

internal fun View.widgetTexts(): List<TextView> = buildList {
    if (visibility != View.VISIBLE) return@buildList
    if (this@widgetTexts is TextView) add(this@widgetTexts)
    if (this@widgetTexts is ViewGroup) {
        for (index in 0 until childCount) addAll(getChildAt(index).widgetTexts())
    }
}
