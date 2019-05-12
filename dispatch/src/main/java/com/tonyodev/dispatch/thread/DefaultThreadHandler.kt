package com.tonyodev.dispatch.thread

import java.lang.IllegalStateException

/**
 * The default Thread Handler. Performs it works on a plain old thread.
 * */
class DefaultThreadHandler(override val threadName: String): ThreadHandler {

    @Volatile
    private var isCancelled = false
    @Volatile
    private var isLooperSleeping = false
    private var threadSleepMillis = DEFAULT_SLEEP_MILLIS
    private val queue = mutableListOf<QueueItem>()
    private var thread: Thread? = null
    private var looper: Runnable? = null

    private fun sleep() {
        try {
            if (!isCancelled) {
                isLooperSleeping = true
                Thread.sleep(threadSleepMillis)
                threadSleepMillis *= 2
                isLooperSleeping = false
            }
        } catch (e: InterruptedException) {
            threadSleepMillis = DEFAULT_SLEEP_MILLIS
            isLooperSleeping = false
        }
    }

    private fun setLopper() {
        looper = Runnable {
            var index = 0
            var queueItem: QueueItem?
            var isQueueEmpty: Boolean
            while (!isCancelled) {
                synchronized(queue) { isQueueEmpty = queue.isEmpty() }
                if(isQueueEmpty) {
                    sleep()
                } else {
                    queueItem = synchronized(queue) { queue.getOrNull(index) }
                    if (!isCancelled) {
                        if (queueItem != null) {
                            if (queueItem.isReady) {
                                queueItem.runnable?.run()
                                queueItem.recycle()
                                if (!isCancelled) {
                                    synchronized(queue) {
                                        if (queue.size > index) {
                                            queue.removeAt(index)
                                        }
                                    }
                                    index = 0
                                }
                            } else {
                                synchronized(queue) {
                                    val item = queue.minBy { it.delay }
                                    if (item != null) {
                                        index = queue.indexOf(item)
                                    } else {
                                        index = 0
                                        threadSleepMillis = DEFAULT_SLEEP_MILLIS
                                        sleep()
                                    }
                                }
                            }
                            queueItem = null
                        } else {
                            index = 0
                            threadSleepMillis = DEFAULT_SLEEP_MILLIS
                            sleep()
                        }
                    }
                }
            }
        }
    }

    override fun post(runnable: Runnable) {
        postDelayed(0, runnable)
    }

    override fun postDelayed(delayInMilliseconds: Long, runnable: Runnable) {
        synchronized(queue) {
            if (isCancelled) {
                throw IllegalStateException("Cannot post runnable because quit() was already called.")
            }
            queue.add(QueueItem.obtain(delayInMilliseconds, runnable))
            if (thread == null) {
                setLopper()
                thread = Thread(looper)
                thread?.name = threadName
                thread?.start()
            }
            if (isLooperSleeping) {
                thread?.interrupt()
            }
        }
    }

    override fun removeCallbacks(runnable: Runnable) {
        synchronized(queue) {
            val iterator = queue.iterator()
            var queueItem: QueueItem
            while (iterator.hasNext()) {
                queueItem = iterator.next()
                if (queueItem.runnable == runnable) {
                    iterator.remove()
                }
            }
        }
    }

    override fun quit() {
        if (!isCancelled) {
            isCancelled = true
            synchronized(queue) {
                if (isLooperSleeping) {
                    thread?.interrupt()
                    looper = null
                    thread = null
                }
                queue.clear()
            }
        }
    }

    private class QueueItem private constructor() {

        var delay: Long = 0
            private set
        var runnable: Runnable? = null
            private set
        private var startWaitTime = 0L
        private var next: QueueItem? = null

        val waitTime: Long
            get() {
                return if (startWaitTime == 0L) 0 else System.currentTimeMillis() - startWaitTime
            }

        val isReady: Boolean
            get() {
                return delay < 1 || waitTime >= delay
            }

        fun recycle() {
            synchronized(poolSync) {
                startWaitTime = 0
                delay = 0
                runnable = null
                if (poolSize <= MAX_POOL_SIZE) {
                    next = pool
                    pool = this
                    poolSize++
                }
            }
        }

        companion object {

            private val poolSync = Any()
            private var pool: QueueItem? = null
            private const val MAX_POOL_SIZE = 50
            private var poolSize = 0

            @JvmStatic
            fun obtain(delayInMilliseconds: Long, runnable: Runnable): QueueItem {
                return synchronized(poolSync) {
                    val queueItem: QueueItem
                    if (pool == null) {
                        queueItem = QueueItem()
                    } else {
                        queueItem = pool!!
                        pool = pool?.next
                        poolSize--
                    }
                    queueItem.delay = delayInMilliseconds
                    queueItem.runnable = runnable
                    queueItem.startWaitTime = System.currentTimeMillis()
                    queueItem
                }
            }

        }

    }

    private companion object {
        private const val DEFAULT_SLEEP_MILLIS = 500L
    }

}