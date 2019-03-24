package com.tonyodev.dispatch

import android.os.Handler

/**
 * Class that allows for the attaching of dispatch observers and publishing results to them.
 * */
class DispatchObservable<R> private constructor(private val handler: Handler,
                                                private val shouldNotifyOnHandler: Boolean) {

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
            handler.post {
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
            handler.post {
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

    companion object {

        /**
         * Creates a new DispatchObservable that notifies observers of
         * the result on the ui thread.
         * @return dispatch observable
         * */
        @JvmStatic
        fun <R> create(): DispatchObservable<R> {
            return create(ThreadType.MAIN)
        }

        /**
         * Creates a new DispatchObservable that notifies observers of
         * the result on the passed in handler.
         * @param handler the handler.
         * @return dispatch observable
         * */
        @JvmStatic
        fun <R> create(handler: Handler): DispatchObservable<R> {
            return DispatchObservable(handler, true)
        }

        /**
         * Creates a new DispatchObservable that notifies observers of
         * the result on the passed in thread type.
         * @param threadType the thread type
         * @return dispatch observable
         * */
        @JvmStatic
        fun <R> create(threadType: ThreadType): DispatchObservable<R> {
            val threadPair = Threader.getHandlerPairForThreadType(threadType)
            return DispatchObservable(threadPair.first, true)
        }

        internal fun <R> create(handler: Handler, shouldNotifyOnHandler: Boolean): DispatchObservable<R> {
            return DispatchObservable(handler, shouldNotifyOnHandler)
        }

    }

}