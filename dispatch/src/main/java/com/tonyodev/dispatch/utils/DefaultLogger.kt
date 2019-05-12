package com.tonyodev.dispatch.utils

import android.util.Log

class DefaultLogger: Logger {

    override fun print(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun print(tag: String, throwable: Throwable) {
        Log.d(tag, throwable.message, throwable)
    }

    override fun print(tag: String, any: Any?) {
        Log.d(tag, any?.toString())
    }

    override fun print(message: String) {
        Log.d(TAG, message)
    }

    override fun print(throwable: Throwable) {
        Log.d(TAG, throwable.message, throwable)
    }

    override fun print(any: Any?) {
        Log.d(TAG, any?.toString())
    }

}