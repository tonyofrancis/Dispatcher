package com.tonyodev.dispatch.thread

import com.tonyodev.dispatch.ThreadType

/**
 * Interface that can be extend to create a Factory that creates thread handlers.
 * */
interface ThreadHandlerFactory {

    /**
     * Creates a thread Handler.
     * @param threadType the threadType.
     * @return thread handler.
     * */
    fun create(threadType: ThreadType): ThreadHandler

    /**
     * Creates a thread Handler with the passed in name.
     * @param threadName the thread name. If null, a name will be automatically generated.
     * @return thread handler.
     * */
    fun create(threadName: String?): ThreadHandler


}