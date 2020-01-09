package com.zoomcar.zcnetwork.core

import android.content.Context
import com.readystatesoftware.chuck.ChuckInterceptor
import com.zoomcar.zcnetwork.utils.RetrofitConstants
import com.zoomcar.zcnetwork.utils.TimeoutDefaults
import com.zoomcar.zcnetwork.utils.TimeoutHeaders
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/*
  * @created 05/01/2020 - 10:19 PM
  * @project ZC-Network-Client
  * @author Paras
  * Copyright (c) 2020 Zoomcar. All rights reserved.
*/
class ZcRequestManager(
    private val applicationContext: Context
) {

    private lateinit var defaultApiService: ZcApiService
    private val retrofit: Retrofit
    private var headerMap: HashMap<String, String>? = null
    private var isDebugLogEnabled: Boolean = false

    init {
        retrofit = Retrofit.Builder().baseUrl(getBaseUrl())
            .client(getClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        @Volatile
        private var instance: ZcRequestManager? = null

        fun getInstance(applicationContext: Context) =
            instance ?: synchronized(applicationContext) {
                instance ?: ZcRequestManager(applicationContext).also { instance = it }
            }
    }

    fun getDefaultApiService(): ZcApiService {
        defaultApiService = retrofit.create(ZcApiService::class.java)
        return defaultApiService
    }

    private fun getBaseUrl(): String = RetrofitConstants.BASE_URL

    private fun getClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor { chain ->
            var request: Request = chain.request()
            val url: HttpUrl = request.url().newBuilder().build()
            var requestBuilder: Request.Builder = request.newBuilder()
            headerMap?.let {
                for ((key, value) in headerMap!!) {
                    requestBuilder = requestBuilder.addHeader(key, value)
                }
            }
            request = requestBuilder.url(url).build()

            val connectTimeout = TimeoutDefaults.CONNECT
            val readTimeout = TimeoutDefaults.READ
            val writeTimeout = TimeoutDefaults.WRITE

            // Remove synthetic headers, as we don't need to pass them to server
            requestBuilder.removeHeader(TimeoutHeaders.CONNECT)
            requestBuilder.removeHeader(TimeoutHeaders.READ)
            requestBuilder.removeHeader(TimeoutHeaders.WRITE)

            request = requestBuilder.build()

            chain.withConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .withReadTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .withWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .proceed(request)
        }

        if (isDebugLogEnabled) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(logging)
            builder.addInterceptor(ChuckInterceptor(applicationContext))
        }

        return builder.build()
    }

    private fun setHeaderParams(headerMap: HashMap<String, String>) {
        this.headerMap = headerMap
    }

    private fun setDebugLog(isDebugLogEnabled: Boolean) {
        this.isDebugLogEnabled = isDebugLogEnabled
    }
}