package com.tonyodev.dispatch

import java.util.*

internal class DispatchQueue(val queueId: Int,
                             val isIntervalDispatch: Boolean = false,
                             var cancelOnComplete: Boolean) {
    @Volatile
    var isCancelled = false
    @Volatile
    var completedDispatchQueue = false
    lateinit var rootDispatch: DispatchImpl<*, *>
    val queue = LinkedList<DispatchImpl<*, *>>()
    var errorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null
    var dispatchQueueController: DispatchQueueController? = null

}