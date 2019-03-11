package com.tonyodev.dispatcherapp

import com.tonyodev.dispatch.Dispatch
import retrofit2.http.GET

interface TestService {

    @GET("/api/data?list=englishmonarchs&format=json")
    fun getSampleJson(): Dispatch<List<TestJsonData>>

}