package com.tonyodev.dispatch.thread

import java.lang.IllegalStateException

/**
 * The default Thread Handler. Performs it works on a plain old thread.
 * */
class DefaultThreadHandler(override val name: String): Thread(), ThreadHandler {

    @Volatile
    private var isCancelled = false
    @Volatile
    private var isSleeping = false
    private val defaultSleepTime = 250L
    private var threadSleepMillis = defaultSleepTime
    private val queue = mutableListOf<QueueItem>()
    private var queueIndex = 0
    private var queueItem: QueueItem? = null
    private var isQueueEmpty = true
    private val minQueuePair = MinQueuePair()

    override val isActive: Boolean
        get()  {
            return isAlive
        }

    init {
        name = name
    }

    override fun run() {
        while (!isCancelled) {
            synchronized(queue) {
                isQueueEmpty = queue.isEmpty()
            }
            if(isQueueEmpty) {
                sleep()
            } else {
                processNextQueueItem()
            }
        }
    }

    private fun processNextQueueItem() {
        if (!isCancelled) {
            synchronized(queue) {
                if (queueIndex < queue.size) {
                    queueItem = queue[queueIndex]
                }
            }
            if (!isCancelled) {
                if (queueItem != null) {
                    if (queueItem!!.isReady) {
                        if (!isCancelled) {
                            synchronized(queue) {
                                queueItem!!.isRunning = true
                            }
                            queueItem!!.runnable?.run()
                        }
                        if (!isCancelled) {
                            synchronized(queue) {
                                queue.remove(queueItem!!)
                            }
                        }
                        queueItem?.recycle()
                        queueIndex = 0
                    } else {
                        val minQueuePair1 = findQueueMinIndex()
                        queueIndex = if (minQueuePair1.index > -1) {
                            minQueuePair1.index
                        } else {
                            0
                        }
                        if (minQueuePair1.delay > 0) {
                            threadSleepMillis = minQueuePair1.delay
                            sleep()
                        }
                    }
                    queueItem = null
                } else {
                    queueIndex = 0
                    threadSleepMillis = defaultSleepTime
                    sleep()
                }
            } else {
                queueItem?.recycle()
                queueItem = null
            }

        }
    }

    private fun findQueueMinIndex(): MinQueuePair {
        return synchronized(queue) {
            minQueuePair.index = -1
            minQueuePair.delay = -1
            var minDelay = -1L
            var counter = 0
            var queueItem: QueueItem
            while (counter < queue.size) {
                queueItem = queue[counter]
                if (minDelay == -1L || queueItem.delay < minDelay) {
                    minQueuePair.index = counter
                    minDelay = queueItem.delay
                    minQueuePair.delay = queueItem.delay - queueItem.waitTime
                    if (minQueuePair.delay < 0) {
                        minQueuePair.delay = 0
                    }
                }
                counter++
            }
            minQueuePair
        }
    }

    private fun sleep() {
        if (!isCancelled) {
            try {
                isSleeping = true
                sleep(threadSleepMillis)
                threadSleepMillis *= 2
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
        val queueItem = QueueItem.obtain(delayInMilliseconds, runnable)
        synchronized(queue) {
            queue.add(queueItem)
        }
        if (isSleeping) {
            interrupt()
        }
    }

    override fun removeCallbacks(runnable: Runnable) {
        synchronized(queue) {
            val recycleList = mutableListOf<QueueItem>()
            val iterator = queue.iterator()
            var queueItem: QueueItem
            while (iterator.hasNext()) {
                queueItem = iterator.next()
                if (queueItem.runnable == runnable) {
                    iterator.remove()
                    recycleList.add(queueItem)
                }
            }
            for (recycleQueueItem in recycleList) {
                if (!recycleQueueItem.isRunning) {
                    recycleQueueItem.recycle()
                }
            }
            recycleList.clear()
        }
    }

    override fun quit() {
        if (!isCancelled) {
            isCancelled = true
            if (isSleeping) {
                interrupt()
            }
            isSleeping = false
            synchronized(queue) {
                val recycleList = mutableListOf<QueueItem>()
                val iterator = queue.iterator()
                var queueItem: QueueItem
                while (iterator.hasNext()) {
                    queueItem = iterator.next()
                    iterator.remove()
                    recycleList.add(queueItem)
                }
                for (recycleQueueItem in recycleList) {
                    if (!recycleQueueItem.isRunning) {
                        recycleQueueItem.recycle()
                    }
                }
                recycleList.clear()
            }
            isQueueEmpty = true
            queueIndex = 0
            threadSleepMillis = defaultSleepTime
            minQueuePair.delay = -1
            minQueuePair.index = -1
        }
    }

    override fun start() {
        super.start()
    }

    private class MinQueuePair {
        var index = -1
        var delay = -1L
    }

    private class QueueItem private constructor() {

        var delay: Long = 0
            private set
        var runnable: Runnable? = null
            private set
        private var startWaitTime = 0L
        private var next: QueueItem? = null
        private var isRecycled = false
        var isRunning = false

        val waitTime: Long
            get() {
                return if (startWaitTime == 0L) 0 else System.currentTimeMillis() - startWaitTime
            }

        val isReady: Boolean
            get() {
                return (delay < 1 || waitTime >= delay) && !isRecycled
            }

        fun recycle() {
            synchronized(poolSync) {
                if (!isRecycled) {
                    startWaitTime = 0
                    delay = 0
                    runnable = null
                    isRecycled = true
                    isRunning = false
                    if (poolSize <= MAX_POOL_SIZE) {
                        next = pool
                        pool = this
                        poolSize++
                    }
                }
            }
        }

        companion object {

            private val poolSync = Any()
            private var pool: QueueItem? = null
            private const val MAX_POOL_SIZE = 100
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
                        queueItem.isRecycled = false
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