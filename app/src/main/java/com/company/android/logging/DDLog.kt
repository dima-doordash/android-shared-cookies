package com.company.android.logging

import android.util.Log

/**
 * DDLog copy
 */
object DDLog {
    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
}
