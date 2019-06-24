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
    val threadHandler: ThreadHandler?) {

    @Volatile
    var shouldNotifyOnHandler: Boolean = false

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
    @Suppress("UNCHECKED_CAST")
    fun addObserver(dispatchQueueObserver: DispatchQueueObserver<R>): DispatchQueueObservable<R> {
        synchronized(dispatchQueueObserversSet) {
            dispatchQueueObserversSet.add(dispatchQueueObserver)
            if (result != INVALID_RESULT) {
                val value = result as R
                if (shouldNotifyOnHandler && threadHandler != null) {
                    threadHandler.post(Runnable { dispatchQueueObserver.onChanged(value) })
                } else {
                    dispatchQueueObserver.onChanged(value)
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
    @Suppress("UNCHECKED_CAST")
    fun addObservers(dispatchQueueObservers: Collection<DispatchQueueObserver<R>>): DispatchQueueObservable<R> {
        synchronized(dispatchQueueObserversSet) {
            dispatchQueueObserversSet.addAll(dispatchQueueObservers)
            val result = this.result
            if (result != INVALID_RESULT) {
                val value = result as R
                if (shouldNotifyOnHandler && threadHandler != null) {
                    val observers = dispatchQueueObservers.toList()
                    threadHandler.post(Runnable {
                        for (dispatchObserver in observers) {
                            dispatchObserver.onChanged(value)
                        }
                    })
                } else {
                    for (dispatchObserver in dispatchQueueObservers) {
                        dispatchObserver.onChanged(value)
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
        if (shouldNotifyOnHandler && threadHandler != null) {
            threadHandler.post(Runnable { notifyObservers(result) })
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

    /** Get all the observers attached to this observable*/
    fun getObservers(): List<DispatchQueueObserver<R>> {
        return synchronized(dispatchQueueObserversSet) { dispatchQueueObserversSet.toList() }
    }

}