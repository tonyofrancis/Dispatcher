package com.tonyodev.dispatch.thread

/**
 * Class used to queue runnables inside of a DefaultThreadHandler.
 * The ThreadHandlerQueueItem also keeps a record of how much time
 * it has waited in the queue.
 * */
class ThreadHandlerQueueItem private constructor() {

    /**The delay before this queue item is removed from the default thread handler queue and executed.*/
    var delay: Long = 0
        private set

    /** The runnable that will be executed by the default thread handler.*/
    var runnable: Runnable? = null
        private set

    /** Check is the queue item is currently being processed by a default thread handler.*/
    var isProcessing = false

    /** Checks how long in milliseconds a queue item has been waiting in a default thread handler queue.*/
    val waitTime: Long
        get() {
            return if (startWaitTime == 0L) 0 else System.currentTimeMillis() - startWaitTime
        }

    /** Checks if a queue item is ready to be queued by the default thread handler.*/
    val isReady: Boolean
        get() {
            return (delay < 1 || waitTime >= delay) && !isRecycled
        }

    private var startWaitTime = 0L
    private var next: ThreadHandlerQueueItem? = null
    private var isRecycled = false

    /**
     * Class used to maintain an ThreadHandlerQueueItem pool.
     * */
    class Pool(
        /**
         * The max pool size. The default size is 50
         * */
        val maxPoolSize: Int = 50) {

        private var pool: ThreadHandlerQueueItem? = null

        /**
         * The pool size
         * */
        var size = 0
            private set

        /**
         * Obtains a ThreadHandlerQueueItem from the pool.
         * @param delayInMilliseconds the delay in milliseconds before the runnable runs.
         * @param runnable the runnable that will be executed by the thread handler.
         * @return a ThreadHandlerQueueItem instance.
         * */
        fun obtain(delayInMilliseconds: Long, runnable: Runnable): ThreadHandlerQueueItem {
            return synchronized(this) {
                val queueItem: ThreadHandlerQueueItem
                if (pool == null) {
                    queueItem = ThreadHandlerQueueItem()
                } else {
                    queueItem = pool!!
                    pool = queueItem.next
                    queueItem.next = null
                    queueItem.isRecycled = false
                    size--
                }
                queueItem.delay = delayInMilliseconds
                queueItem.runnable = runnable
                queueItem.startWaitTime = System.currentTimeMillis()
                queueItem
            }
        }

        /**
         * Recycles a ThreadHandlerQueueItem. The ThreadHandlerQueueItem is added to this pool
         * if the max pool size is not reached.
         * */
        fun recycle(threadHandlerQueueItem: ThreadHandlerQueueItem) {
            synchronized(this) {
                if (!threadHandlerQueueItem.isRecycled) {
                    threadHandlerQueueItem.startWaitTime = 0
                    threadHandlerQueueItem.delay = 0
                    threadHandlerQueueItem.runnable = null
                    threadHandlerQueueItem.isRecycled = true
                    threadHandlerQueueItem.isProcessing = false
                    if (size <= maxPoolSize) {
                        threadHandlerQueueItem.next = pool
                        pool = threadHandlerQueueItem
                        size++
                    }
                }
            }
        }

    }

}