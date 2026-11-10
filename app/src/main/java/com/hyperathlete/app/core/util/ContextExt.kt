package com.hyperathlete.app.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks the wrapper chain (e.g. our localized context) back to the hosting activity. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
