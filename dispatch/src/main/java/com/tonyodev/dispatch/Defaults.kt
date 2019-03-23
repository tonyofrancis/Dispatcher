package com.tonyodev.dispatch

internal var enableWarnings = false
internal var globalErrorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null