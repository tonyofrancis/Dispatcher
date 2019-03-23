package com.tonyodev.dispatch

import android.os.Handler

internal class DispatchObservableImpl<R>(private val handler: Handler,
                                         private val shouldNotifyOnHandler: Boolean): DispatchObservable<R> {

    private val dispatchObserversSet = mutableSetOf<DispatchObserver<R>>()
    private var result: Any? = INVALID_RESULT

    override fun addObserver(dispatchObserver: DispatchObserver<R>): DispatchObservable<R> {
        dispatchObserversSet.add(dispatchObserver)
        if (result != INVALID_RESULT) {
            handler.post {
                notifyObserver(dispatchObserver)
            }
        }
        return this
    }

    override fun addObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchObservable<R> {
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

    override fun removeObserver(dispatchObserver: DispatchObserver<R>): DispatchObservable<R> {
        val iterator = dispatchObserversSet.iterator()
        while (iterator.hasNext()) {
            if (dispatchObserver == iterator.next()) {
                iterator.remove()
                break
            }
        }
        return this
    }

    override fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchObservable<R> {
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

    fun removeAllObservers() {
        val iterator = dispatchObserversSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun notifyObserver(dispatchObserver: DispatchObserver<R>) {
        val r = result
        if (r != INVALID_RESULT) {
            dispatchObserver.onChanged(r as R)
        }
    }

    override fun notify(result: R): DispatchObservable<R> {
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

}