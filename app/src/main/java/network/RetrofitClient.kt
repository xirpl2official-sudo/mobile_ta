package com.xirpl2.SASMobile.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Development URLs:
    // - Android Emulator: http://10.0.2.2:3000/api/
    // - Physical device (USB): Run 'adb reverse tcp:3000 tcp:3000' and use http://127.0.0.1:3000/api/
    // - Production: https://absensholat-api.vercel.app/api/
    private const val BASE_URL = "http://127.0.0.1:3000/api/"

    private lateinit var appContext: Context

    /**
     * Must be called from SASMobileApp.onCreate() before any API calls are made.
     * Provides application context for TokenAuthenticator to access SharedPreferences.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(appContext))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
