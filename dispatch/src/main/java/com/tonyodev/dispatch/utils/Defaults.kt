package com.tonyodev.dispatch.utils

import com.tonyodev.dispatch.Dispatch

internal var enableWarnings = false
internal var globalErrorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null