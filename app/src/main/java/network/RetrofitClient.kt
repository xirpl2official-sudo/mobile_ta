package com.xirpl2.SASMobile.network

import android.content.Context
import com.xirpl2.SASMobile.network.generated.api.AttendanceApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 1. URL Local — akses backend laptop dari device fisik via USB (adb reverse tcp:3000 tcp:3000)
    private const val BASE_URL = "http://127.0.0.1:3000/api/"

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

    // 5. Shared Retrofit instance
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 6. ApiService Instance
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // 7. Generated API services
    val attendanceApi: AttendanceApi by lazy { retrofit.create(AttendanceApi::class.java) }
}