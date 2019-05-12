package com.tonyodev.dispatch.thread

import java.lang.IllegalStateException

/**
 * The default Thread Handler. Performs it works on a plain old thread.
 * */
class DefaultThreadHandler(override val threadName: String): Thread(), ThreadHandler {

    @Volatile
    private var isCancelled = false
    @Volatile
    private var isSleeping = false
    private val defaultSleepTime = 500L
    private var threadSleepMillis = defaultSleepTime
    private val queue = mutableListOf<QueueItem>()

    init {
        name = threadName
        start()
    }

    override fun run() {
        var index = 0
        var queueItem: QueueItem? = null
        while (!isCancelled) {
            if(queue.isEmpty()) {
                sleep()
            } else {
                if (!isCancelled) {
                    synchronized(queue) {
                        if (index < queue.size) {
                            queueItem = queue[index]
                        }
                    }
                    if (queueItem != null) {
                        if (queueItem!!.isReady) {
                            queueItem!!.runnable?.run()
                            queueItem!!.recycle()
                            if (!isCancelled) {
                                if (queue.size > index) {
                                    queue.removeAt(index)
                                }
                            }
                            index = 0
                        } else {
                            synchronized(queue) {
                                queueItem = queue.minBy { it.delay }
                                index = queue.indexOf(queueItem)
                                if (index == -1) {
                                    index = 0
                                }
                            }
                            if (queueItem == null) {
                                index = 0
                                threadSleepMillis = defaultSleepTime
                                sleep()
                            }
                        }
                        queueItem = null
                    } else {
                        index = 0
                        threadSleepMillis = defaultSleepTime
                        sleep()
                    }
                }
            }
        }
    }

    private fun sleep() {
        if (!isCancelled) {
            try {
                isSleeping = true
                sleep(threadSleepMillis)
                threadSleepMillis *= 2
                println("$threadName sleeping for $threadSleepMillis")
                isSleeping = false
            } catch (e: InterruptedException) {
                threadSleepMillis = defaultSleepTime
                isSleeping = false
            }
        }
    }

    override fun post(runnable: Runnable) {
        postDelayed(0, runnable)
    }

    override fun postDelayed(delayInMilliseconds: Long, runnable: Runnable) {
        if (isCancelled) {
            throw IllegalStateException("Cannot post runnable because quit() was already called.")
        }
        synchronized(queue) {
            queue.add(QueueItem.obtain(delayInMilliseconds, runnable))
        }
        if (isSleeping) {
            interrupt()
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
            if (isSleeping) {
                interrupt()
            }
            synchronized(queue) {
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
                        queueItem.next = null
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

}