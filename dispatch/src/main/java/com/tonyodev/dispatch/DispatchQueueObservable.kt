package com.tonyodev.dispatch

import com.tonyodev.dispatch.thread.ThreadHandler
import com.tonyodev.dispatch.utils.INVALID_RESULT
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.startThreadHandlerIfNotActive

/**
 * Class that allows for the attaching of dispatch queue observers and publishing results to them.
 * */
class DispatchQueueObservable<R> constructor(
    /**
     * Notifies the attached Observers on the passed in threadHandler.
     * */
    private val threadHandler: ThreadHandler?) {

    private var shouldNotifyOnHandler: Boolean = false

    /**
     * Notifies the attached Observers on the main thread.
     * */
    constructor(): this(Threader.getHandlerThreadInfo(ThreadType.MAIN).threadHandler)

    private val dispatchQueueObserversSet = mutableSetOf<DispatchQueueObserver<R>>()
    private var result: Any? = INVALID_RESULT

    init {
        if (threadHandler != null) {
            shouldNotifyOnHandler = true
        }
        if (shouldNotifyOnHandler && threadHandler != null) {
            startThreadHandlerIfNotActive(threadHandler)
        }
    }

    /**
     * Adds a dispatch queue observer.
     * @param dispatchQueueObserver the observer.
     * @return the dispatchQueueObservable.
     * */
    fun addObserver(dispatchQueueObserver: DispatchQueueObserver<R>): DispatchQueueObservable<R> {
        synchronized(dispatchQueueObserversSet) {
            dispatchQueueObserversSet.add(dispatchQueueObserver)
            if (result != INVALID_RESULT) {
                if (shouldNotifyOnHandler) {
                    threadHandler?.post(Runnable { notifyObserver(dispatchQueueObserver) })
                } else {
                    notifyObserver(dispatchQueueObserver)
                }
            }
        }
        return this
    }

    /**
     * Adds a list of dispatch queue observers.
     * @param dispatchQueueObservers the collection of observers.
     * @return the dispatchQueueObservable.
     * */
    fun addObservers(dispatchQueueObservers: Collection<DispatchQueueObserver<R>>): DispatchQueueObservable<R> {
        synchronized(dispatchQueueObserversSet) {
            dispatchQueueObserversSet.addAll(dispatchQueueObservers)
            if (result != INVALID_RESULT) {
                if (shouldNotifyOnHandler) {
                    val observers = dispatchQueueObserversSet.toList()
                    threadHandler?.post(Runnable {
                        for (dispatchObserver in observers) {
                            notifyObserver(dispatchObserver)
                        }
                    })
                } else {
                    for (dispatchObserver in dispatchQueueObservers) {
                        notifyObserver(dispatchObserver)
                    }
                }
            }
        }
        return this
    }

    /**
     * Removes a dispatch queue observer.
     * @param dispatchQueueObserver the observer to be removed.
     * @return the dispatchQueueObservable.
     * */
    fun removeObserver(dispatchQueueObserver: DispatchQueueObserver<R>): DispatchQueueObservable<R> {
        synchronized(dispatchQueueObserversSet) {
            dispatchQueueObserversSet.remove(dispatchQueueObserver)
        }
        return this
    }

    /**
     * Remove a list of dispatch queue observers.
     * @param dispatchQueueObservers the list of observers to be removed.
     * @return the dispatchQueueObservable.
     * */
    fun removeObservers(dispatchQueueObservers: Collection<DispatchQueueObserver<R>>): DispatchQueueObservable<R> {
        synchronized(dispatchQueueObserversSet) {
            dispatchQueueObserversSet.removeAll(dispatchQueueObservers)
        }
        return this
    }

    /** Removes all observers attached this observable.
     * @return the dispatchQueueObservable.
     * */
    fun removeObservers(): DispatchQueueObservable<R> {
        synchronized(dispatchQueueObserversSet) {
            dispatchQueueObserversSet.clear()
        }
        return this
    }

    /** Notifies observers of the passed in result
     * @param result the result
     * @return the dispatchQueueObservable.
     * */
    fun notify(result: R): DispatchQueueObservable<R> {
        if (shouldNotifyOnHandler) {
            threadHandler?.post(Runnable { notifyObservers(result) })
        } else {
            notifyObservers(result)
        }
        return this
    }

    private fun notifyObservers(result: R) {
        this.result = result
        synchronized(dispatchQueueObserversSet) {
            for (dispatchQueueObserver in dispatchQueueObserversSet) {
                dispatchQueueObserver.onChanged(result)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun notifyObserver(dispatchQueueObserver: DispatchQueueObserver<R>) {
        val r = result
        if (r != INVALID_RESULT) {
            dispatchQueueObserver.onChanged(r as R)
        }
    }

    /** Get all the observers attached to this observable*/
    fun getObservers(): List<DispatchQueueObserver<R>> {
        return synchronized(dispatchQueueObserversSet) { dispatchQueueObserversSet.toList() }
    }

    internal fun resetResult() {
        result = INVALID_RESULT
    }

}