package com.tonyodev.dispatcherapp

import com.tonyodev.dispatch.Dispatch
import retrofit2.http.GET

interface TestService {

    @GET("/todos")
    fun getSampleJson(): Dispatch<List<TestJsonData>>

}