package com.tonyodev.dispatcherapp

import com.tonyodev.dispatch.DispatchQueue
import retrofit2.http.GET

interface TestService {

    @GET("/todos")
    fun getSampleJson(): DispatchQueue<List<TestJsonData>>

}