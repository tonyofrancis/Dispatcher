package com.tonyodev.dispatch

import com.tonyodev.dispatch.thread.TestThreadHandler
import org.junit.Test

class DispatchQueueObservableTest {

    @Test
    fun testAddObserver() {
        val observer = object: DispatchQueueObserver<Any> {
            override fun onChanged(t: Any) {

            }
        }
        val observable = DispatchQueueObservable<Any>(null)
        observable.addObserver(observer)
        assert(observable.getObservers().first() == observer)
        assert(observable.getObservers().size == 1)
        observable.addObservers(listOf(observer))
        assert(observable.getObservers().size == 1)
    }

    @Test
    fun testRemoveObserver() {
        val observer = object: DispatchQueueObserver<Any> {
            override fun onChanged(t: Any) {

            }
        }
        val observable = DispatchQueueObservable<Any>(null)
        observable.addObserver(observer)
        assert(observable.getObservers().size == 1)
        observable.removeObserver(observer)
        assert(observable.getObservers().isEmpty())
        observable.addObservers(listOf(observer))
        assert(observable.getObservers().size == 1)
        observable.removeObservers()
        assert(observable.getObservers().isEmpty())
    }

    @Test
    fun testObserverSize() {
        val observer = object: DispatchQueueObserver<Any> {
            override fun onChanged(t: Any) {

            }
        }
        val observable = DispatchQueueObservable<Any>(null)
        observable.addObserver(observer)
        assert(observable.getObservers().size == 1)
        observable.removeObserver(observer)
        assert(observable.getObservers().isEmpty())
    }

    @Test
    fun testThreadHandler() {
        val observable = DispatchQueueObservable<Any>(null)
        assert(observable.threadHandler == null)
        val handler = TestThreadHandler("Test")
        val observable1 = DispatchQueueObservable<Any>(handler)
        assert(observable1.threadHandler == handler)
        assert(observable1.shouldNotifyOnHandler)
    }

    @Test
    fun testShouldNotify() {
        val observable = DispatchQueueObservable<Any>(null)
        assert(!observable.shouldNotifyOnHandler)
        val handler = TestThreadHandler("Test")
        val observable1 = DispatchQueueObservable<Any>(handler)
        assert(observable1.shouldNotifyOnHandler)
    }

    @Test
    fun testNotify() {
        val value = 44
        val observer = object: DispatchQueueObserver<Any> {
            override fun onChanged(t: Any) {
                val t = t as Int
                assert(t == value)
            }
        }
        val observable = DispatchQueueObservable<Any>(null)
        observable.addObserver(observer)
        observable.notify(value)
    }

}