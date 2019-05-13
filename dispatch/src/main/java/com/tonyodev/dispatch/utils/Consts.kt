package com.tonyodev.dispatch.utils

const val TAG = "com.tonyodev.dispatch"
internal const val DISPATCH_TYPE_NORMAL = 0
internal const val DISPATCH_TYPE_ANY_RESULT = 1
internal val INVALID_RESULT = InvalidResult()

const val THREAD_BACKGROUND = "dispatchBackground"
const val THREAD_BACKGROUND_SECONDARY = "dispatchBackgroundSecondary"
const val THREAD_NETWORK = "dispatchNetwork"
const val THREAD_IO = "dispatchIO"
const val THREAD_TEST = "dispatchTest"
const val THREAD_MAIN_NO_UI = "dispatchMain"