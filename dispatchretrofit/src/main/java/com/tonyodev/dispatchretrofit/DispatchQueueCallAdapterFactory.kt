package com.tonyodev.dispatchretrofit

import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.ThreadHandler
import retrofit2.*
import java.lang.IllegalArgumentException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * DispatchQueueCallAdapterFactory for Retrofit.
 * */
class DispatchQueueCallAdapterFactory constructor(
    /** Optional threadHandler used by the DispatchQueues created by this Factory.*/
    private val threadHandler: ThreadHandler? = null,
    /** Optional error callback for network requests made by a DispatchQueueCallAdapter instance.
     * Only called if an error occurred. Note: The errors will still be thrown inside the DispatchQueue.
     * This callback only allows for observing at a global level.*/
    private val onErrorCallback: OnErrorCallback?): CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val clazz = getRawType(returnType)
        if (clazz == DispatchQueue::class.java) {
            if (returnType !is ParameterizedType) {
                throw IllegalArgumentException("DispatchQueue return type must be parameterized as DispatchQueue<Foo>")
            }
            val responseType = getParameterUpperBound(0, returnType)
            return DispatchQueueCallAdapter<Any>(responseType, threadHandler, onErrorCallback)
        }
        return null
    }

    companion object {

        /**
         * Creates an instance of DispatchQueueCallAdapterFactory
         * @param threadHandler Optional threadHandler used to start this dispatch object. Can be null.
         * @param onErrorCallback Optional on error callback. Can be null.
         * @throws IllegalArgumentException is the passed in threadHandler uses the main thread to do background work.
         * @return new instance of DispatchQueueCallAdapterFactory.
         * */
        @JvmStatic
        @JvmOverloads
        fun create(threadHandler: ThreadHandler? = null, onErrorCallback: OnErrorCallback? = null): DispatchQueueCallAdapterFactory {
            if (threadHandler?.threadName == DispatchQueue.threadHandlerFactory.create(ThreadType.MAIN).threadName) {
                throw IllegalArgumentException("DispatchQueueCallAdapterFactory: ThreadHandler cannot be the main thread for network operations.")
            }
            return DispatchQueueCallAdapterFactory(threadHandler, onErrorCallback)
        }

        /**
         * Creates an instance of DispatchQueueCallAdapterFactory
         * @param onErrorCallback Optional on error callback. Can be null.
         * @return new instance of DispatchQueueCallAdapterFactory.
         * */
        @JvmStatic
        fun create(onErrorCallback: OnErrorCallback? = null): DispatchQueueCallAdapterFactory {
            return DispatchQueueCallAdapterFactory(null, onErrorCallback)
        }

        /**
         * Creates an instance of DispatchQueueCallAdapterFactory that uses a test dispatch queue to perform network requests.
         * @param onErrorCallback Optional on error callback. Can be null.
         * @return new instance of DispatchQueueCallAdapterFactory for test.
         * */
        @JvmStatic
        @JvmOverloads
        fun createTestFactory(onErrorCallback: OnErrorCallback? = null): DispatchQueueCallAdapterFactory {
            return DispatchQueueCallAdapterFactory(DispatchQueue.threadHandlerFactory.create(ThreadType.TEST), onErrorCallback)
        }

    }

    class DispatchQueueCallAdapter<R>(private val responseType: Type,
                                      private val threadHandler : ThreadHandler?,
                                      private val onErrorCallback: OnErrorCallback?): CallAdapter<R, DispatchQueue<*>> {

        override fun adapt(call: Call<R>): DispatchQueue<*> {
            if (threadHandler != null && !threadHandler.isActive) {
                threadHandler.start()
            }
            return if (threadHandler == null) {
                DispatchQueue.createDispatchQueue(ThreadType.IO)
            } else {
                DispatchQueue.createDispatchQueue(threadHandler)
            }.async {
                val callClone = call.clone()
                val response = callClone.execute()
                val data: Any? = if (response.isSuccessful) {
                    response.body()
                } else {
                    HttpException(response)
                }
                callClone.cancel()
                if (data is HttpException) {
                    onErrorCallback?.onError(data, callClone.request(), response)
                    throw data
                }
                data
            }
        }

        override fun responseType(): Type {
            return responseType
        }

    }

}