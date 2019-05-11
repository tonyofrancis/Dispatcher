package com.tonyodev.dispatch

import android.os.Handler
import com.tonyodev.dispatch.utils.INVALID_RESULT
import com.tonyodev.dispatch.utils.Threader

/**
 * Class that allows for the attaching of dispatch observers and publishing results to them.
 * */
class DispatchObservable<R> constructor(private val handler: Handler,
                                        private val shouldNotifyOnHandler: Boolean) {

    /**
     * Notifies the attached Observers on the ui thread.
     * */
    constructor(): this(Threader.uiHandler, true)

    /**
     * Set if the observers are notified on the handler.
     * */
    constructor(shouldNotifyOnHandler: Boolean): this(Threader.uiHandler, shouldNotifyOnHandler)

    /**
     * Notifies the attached Observers on the passed in handler.
     * */
    constructor(handler: Handler): this(handler, true)

    private val dispatchObserversSet = mutableSetOf<DispatchObserver<R>>()
    private var result: Any? = INVALID_RESULT

    /**
     * Adds a dispatch observer.
     * @param dispatchObserver the observer.
     * @return the dispatchObservable.
     * */
    fun addObserver(dispatchObserver: DispatchObserver<R>): DispatchObservable<R> {
        dispatchObserversSet.add(dispatchObserver)
        if (result != INVALID_RESULT) {
            if (shouldNotifyOnHandler) {
                handler.post {
                    notifyObserver(dispatchObserver)
                }
            } else {
                notifyObserver(dispatchObserver)
            }
        }
        return this
    }

    /**
     * Adds a list of dispatch observers.
     * @param dispatchObservers the list of observers.
     * @return the dispatchObservable.
     * */
    fun addObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchObservable<R> {
        dispatchObserversSet.addAll(dispatchObservers)
        if (result != INVALID_RESULT) {
            if (shouldNotifyOnHandler) {
                handler.post {
                    for (dispatchObserver in dispatchObservers) {
                        notifyObserver(dispatchObserver)
                    }
                }
            } else {
                for (dispatchObserver in dispatchObservers) {
                    notifyObserver(dispatchObserver)
                }
            }
        }
        return this
    }

    /**
     * Removes a dispatch observer.
     * @param dispatchObserver the observer to be removed.
     * @return the dispatchObservable.
     * */
    fun removeObserver(dispatchObserver: DispatchObserver<R>): DispatchObservable<R> {
        val iterator = dispatchObserversSet.iterator()
        while (iterator.hasNext()) {
            if (dispatchObserver == iterator.next()) {
                iterator.remove()
                break
            }
        }
        return this
    }

    /**
     * Remove a list of dispatch observers.
     * @param dispatchObservers the list of observers to be removed.
     * @return the dispatchObservable.
     * */
    fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchObservable<R> {
        val iterator = dispatchObserversSet.iterator()
        var count = 0
        while (iterator.hasNext()) {
            if (dispatchObservers.contains(iterator.next())) {
                iterator.remove()
                ++count
                if (count == dispatchObservers.size) {
                    break
                }
            }
        }
        return this
    }

    /** Removes all observers attached this observable.*/
    fun removeObservers() {
        val iterator = dispatchObserversSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    /** Notifies observers of the passed in result
     * @param result the result
     * @return the dispatchObservable.
     * */
    fun notify(result: R): DispatchObservable<R> {
        if (shouldNotifyOnHandler) {
            handler.post {
                notifyObservers(result)
            }
        } else {
            notifyObservers(result)
        }
        return this
    }

    private fun notifyObservers(result: R) {
        this.result = result
        val iterator = dispatchObserversSet.iterator()
        while (iterator.hasNext()) {
            iterator.next().onChanged(result)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun notifyObserver(dispatchObserver: DispatchObserver<R>) {
        val r = result
        if (r != INVALID_RESULT) {
            dispatchObserver.onChanged(r as R)
        }
    }

    /** Get all the observers attached to this observable*/
    fun getObservers(): List<DispatchObserver<R>> {
        return dispatchObserversSet.toList()
    }

}