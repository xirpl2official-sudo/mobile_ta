package com.xirpl2.SASMobile.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 1. URL Local (Android Emulator)
    private const val BASE_URL = "http://10.0.2.2:3000/api/"

    // 2. Variabel Context untuk Autentikasi
    private var appContext: Context? = null

    /**
     * Panggil fungsi ini di onCreate() aplikasi kamu (SASMobileApp.kt)
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // 3. Logging Interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Jika ingin melihat log request/response saat debug, ubah level ke BODY
        // Jika sudah production, gunakan NONE agar tidak berat
        level = HttpLoggingInterceptor.Level.BODY
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