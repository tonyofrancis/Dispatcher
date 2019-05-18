package com.tonyodev.dispatchretrofit

import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.ThreadHandler
import okhttp3.Request
import retrofit2.*
import java.lang.IllegalArgumentException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Dispatch Queue Call Adapter Factory for Retrofit.
 * */
class DispatchQueueCallAdapterFactory constructor(
    /** Optional threadHandler used by the DispatchQueues created by this Factory.*/
    private val threadHandler: ThreadHandler? = null,
    /** Optional error threadHandler for network requests made by a DispatchQueueCallAdapter instance.
     * Only called in an error occurred. Note: The errors will still be thrown inside the DispatchQueue.
     * This callback only allows for observing at a global level. Called on a background thread. */
    private val errorHandler: ((HttpException, Request) -> Unit)?): CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val clazz = getRawType(returnType)
        if (clazz == DispatchQueue::class.java) {
            if (returnType !is ParameterizedType) {
                throw IllegalArgumentException("DispatchQueue return type must be parameterized as DispatchQueue<Foo>")
            }
            val responseType = getParameterUpperBound(0, returnType)
            return DispatchQueueCallAdapter<Any>(responseType, threadHandler, errorHandler)
        }
        return null
    }

    companion object {

        /**
         * Creates an instance of DispatchQueueCallAdapterFactory
         * @param threadHandler Optional threadHandler used to start this dispatch object.
         * @param errorHandler Optional global error threadHandler. Called on a background thread.
         * @throws IllegalArgumentException is the passed in threadHandler uses the main thread to do background work.
         * @return new instance of DispatchQueueCallAdapterFactory.
         * */
        @JvmStatic
        @JvmOverloads
        fun create(threadHandler: ThreadHandler? = null, errorHandler: ((HttpException, Request) -> Unit)? = null): DispatchQueueCallAdapterFactory {
            if (threadHandler?.threadName == DispatchQueue.threadHandlerFactory.create(ThreadType.MAIN).threadName) {
                throw IllegalArgumentException("DispatchQueueCallAdapterFactory: ThreadHandler cannot be the main thread for network operations.")
            }
            return DispatchQueueCallAdapterFactory(threadHandler, errorHandler)
        }

        /**
         * Creates an instance of DispatchQueueCallAdapterFactory that uses a test dispatch queue to perform network requests.
         * @param errorHandler Optional global error threadHandler.
         * @return new instance of DispatchQueueCallAdapterFactory for test.
         * */
        @JvmStatic
        @JvmOverloads
        fun createTestFactory(errorHandler: ((HttpException, Request) -> Unit)? = null): DispatchQueueCallAdapterFactory {
            return DispatchQueueCallAdapterFactory(DispatchQueue.threadHandlerFactory.create(ThreadType.TEST), errorHandler)
        }

    }

    class DispatchQueueCallAdapter<R>(private val responseType: Type,
                                      private val threadHandler : ThreadHandler?,
                                      private val errorHandler: ((HttpException, Request) -> Unit)?): CallAdapter<R, DispatchQueue<*>> {

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
                    errorHandler?.invoke(data, callClone.request())
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