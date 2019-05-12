package com.tonyodev.dispatch.thread

interface ThreadHandler {

    val threadName: String

    fun post(runnable: Runnable)

    fun postDelayed(delayInMilliseconds: Long, runnable: Runnable)

    fun removeCallbacks(runnable: Runnable)

    fun quit()

}