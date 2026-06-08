package com.xirpl2.SASMobile.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 1. URL Configuration — debug: localhost via USB adb reverse; release: production
    private val BASE_URL: String = "http://127.0.0.1:3000/api/"

    // 2. Variabel Context untuk Autentikasi
    private var appContext: Context? = null

    /**
     * Panggil fungsi ini di onCreate() aplikasi kamu (SASMobileApp.kt)
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // 3. Logging Interceptor — BODY only in debug; NONE in production to prevent data leaks
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.xirpl2.SASMobile.BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    // 4. OkHttpClient Configuration
    private val okHttpClient by lazy {
        val ctx = appContext ?: throw IllegalStateException(
            "RetrofitClient.init(context) must be called before any API call"
        )

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(ctx)) // Pastikan class TokenAuthenticator sudah ada
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 5. ApiService Instance
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}