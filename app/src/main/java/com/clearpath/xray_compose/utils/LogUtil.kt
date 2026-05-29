package com.clearpath.xray_compose.utils

import android.util.Log
import com.clearpath.xray_compose.GlobalConst

object LogUtil {
    private fun mkTag(): String {
        // val stackTrace = Thread.currentThread().stackTrace
        // return stackTrace[4].className.substringAfterLast(".")
        return GlobalConst.appId
    }

    fun v(message: String) {
        Log.v(mkTag(), message)
    }

    fun v(message: String, exception: Throwable) {
        Log.v(mkTag(), message, exception)
    }

    fun d(message: String) {
        Log.d(mkTag(), message)
    }

    fun d(message: String, exception: Throwable) {
        Log.d(mkTag(), message, exception)
    }

    fun i(message: String) {
        Log.i(mkTag(), message)
    }

    fun i(message: String, exception: Throwable) {
        Log.i(mkTag(), message, exception)
    }

    fun w(message: String) {
        Log.w(mkTag(), message)
    }

    fun w(message: String, exception: Throwable) {
        Log.w(mkTag(), message, exception)
    }

    fun w(exception: Exception) {
        Log.w(mkTag(), exception)
    }

    fun e(message: String) {
        Log.e(mkTag(), message)
    }

    fun e(message: String, exception: Throwable) {
        Log.e(mkTag(), message, exception)
    }
}