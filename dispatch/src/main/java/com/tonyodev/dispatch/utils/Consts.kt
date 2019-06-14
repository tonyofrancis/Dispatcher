package com.tonyodev.dispatch.utils

import com.tonyodev.dispatch.DispatchQueueErrorCallback

const val TAG = "DispatchQueue"
internal val INVALID_RESULT = InvalidResult()
internal val CANCELLED_ERROR_CALLBACK = DispatchQueueErrorCallback { }
const val THREAD_BACKGROUND = "DispatchQueueBackground"
const val THREAD_IO = "DispatchQueueIO"
const val THREAD_TEST = "DispatchQueueTest"
const val THREAD_MAIN_NO_UI = "DispatchQueueMain"
const val THREAD_NETWORK = "DispatchQueueNetwork"
const val THREAD_COMPUTATION = "DispatchQueueComputation"