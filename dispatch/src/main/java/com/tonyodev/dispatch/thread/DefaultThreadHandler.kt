package com.tonyodev.dispatch.thread

import java.lang.IllegalStateException

/**
 * The default Thread Handler. Performs it works on a plain old thread.
 * */
class DefaultThreadHandler(override val threadName: String): Thread(), ThreadHandler {

    private val queue = mutableListOf<ThreadHandlerQueueItem>()
    private val minQueueIndexPair = MinQueueIndexPair()
    private val queueItemPool = ThreadHandlerQueueItem.Pool()
    private val defaultSleepTime = 250L

    @Volatile
    private var isCancelled = false
    @Volatile
    private var isSleeping = false
    private var queueIndex = 0
    private var queueItem: ThreadHandlerQueueItem? = null
    private var isQueueEmpty = true
    private var threadSleepMillis = defaultSleepTime

    override val isActive: Boolean
        get()  {
            if (isCancelled) {
                return false
            }
            return isAlive
        }

    init {
        name = threadName
    }

    override fun run() {
        while (!isCancelled) {
            synchronized(queue) {
                isQueueEmpty = queue.isEmpty()
            }
            if(isQueueEmpty) {
                sleep()
            } else {
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
                                        queueItem!!.isProcessing = true
                                    }
                                    queueItem!!.runnable?.run()
                                }
                                val item = queueItem
                                if (item != null) {
                                    if (!isCancelled) {
                                        synchronized(queue) {
                                            queue.remove(item)
                                        }
                                    }
                                    queueItemPool.recycle(item)
                                }
                                queueIndex = 0
                            } else {
                                val minQueuePair1 = findQueueMinIndex(queue, minQueueIndexPair)
                                queueIndex = if (minQueuePair1.index > -1)  minQueuePair1.index else 0
                                if (minQueuePair1.waitTime > 0) {
                                    threadSleepMillis = minQueuePair1.waitTime
                                    sleep(false)
                                }
                            }
                            queueItem = null
                        } else {
                            queueIndex = 0
                            threadSleepMillis = defaultSleepTime
                            sleep()
                        }
                    } else {
                        val item = queueItem
                        if (item != null) {
                            queueItemPool.recycle(item)
                        }
                        queueItem = null
                    }
                }
            }
        }
    }

    private fun sleep(backOffSleepTime: Boolean = true) {
        if (!isCancelled) {
            try {
                isSleeping = true
                sleep(threadSleepMillis)
                if (backOffSleepTime) {
                    threadSleepMillis *= 2
                } else {
                    threadSleepMillis = defaultSleepTime
                }
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
        val queueItem = queueItemPool.obtain(delayInMilliseconds, runnable)
        synchronized(queue) {
            queue.add(queueItem)
        }
        if (isSleeping) {
            interrupt()
        }
    }

    override fun removeCallbacks(runnable: Runnable) {
        synchronized(queue) {
            val iterator = queue.iterator()
            var queueItem: ThreadHandlerQueueItem
            while (iterator.hasNext()) {
                queueItem = iterator.next()
                if (queueItem.runnable == runnable) {
                    iterator.remove()
                    if (!queueItem.isProcessing) {
                        queueItemPool.recycle(queueItem)
                    }
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
            isSleeping = false
            synchronized(queue) {
                val iterator = queue.iterator()
                var queueItem: ThreadHandlerQueueItem
                while (iterator.hasNext()) {
                    queueItem = iterator.next()
                    iterator.remove()
                    if (!queueItem.isProcessing) {
                        queueItemPool.recycle(queueItem)
                    }
                }
            }
            isQueueEmpty = true
            queueIndex = 0
            threadSleepMillis = defaultSleepTime
            minQueueIndexPair.waitTime = -1
            minQueueIndexPair.index = -1
            queueItemPool.release()
        }
    }

    override fun start() {
        super.start()
    }

    class MinQueueIndexPair {
        var index = -1
        var waitTime = -1L
    }

}