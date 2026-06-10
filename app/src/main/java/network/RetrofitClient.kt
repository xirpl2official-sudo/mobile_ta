package com.xirpl2.SASMobile.network

import android.content.Context
import com.xirpl2.SASMobile.BuildConfig
import com.xirpl2.SASMobile.network.generated.api.AttendanceApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    var testBaseUrl: String? = null

    private val BASE_URL: String
        get() = testBaseUrl ?: if (BuildConfig.API_BASE_URL.endsWith("/")) BuildConfig.API_BASE_URL else BuildConfig.API_BASE_URL + "/"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun resetForTest(baseUrl: String, context: Context) {
        testBaseUrl = baseUrl
        appContext = context.applicationContext
        _apiService = null
        _attendanceApi = null
        _retrofit = null
        _okHttpClient = null
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        if (original.header("Authorization") != null) {
            chain.proceed(original)
        } else {
            val ctx = appContext
            if (ctx == null) {
                chain.proceed(original)
            } else {
                val token = try {
                    val userData = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(ctx)
                        .getString("auth_token", null)
                    if (userData.isNullOrEmpty()) {
                        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(ctx)
                        session.getString("auth_token", null)
                    } else userData
                } catch (_: Exception) { null }

                if (token.isNullOrEmpty()) {
                    chain.proceed(original)
                } else {
                    val modified = original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(modified)
                }
            }
        }
    }

    private var _okHttpClient: OkHttpClient? = null
    private val okHttpClient: OkHttpClient
        get() {
            if (_okHttpClient == null) {
                val ctx = appContext ?: throw IllegalStateException(
                    "RetrofitClient.init(context) must be called before any API call"
                )
                _okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(authInterceptor)
                    .authenticator(TokenAuthenticator(ctx))
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            }
            return _okHttpClient!!
        }

    private var _retrofit: Retrofit? = null
    private val retrofit: Retrofit
        get() {
            if (_retrofit == null) {
                _retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return _retrofit!!
        }

    private var _apiService: ApiService? = null
    val apiService: ApiService
        get() {
            if (_apiService == null) _apiService = retrofit.create(ApiService::class.java)
            return _apiService!!
        }

    private var _attendanceApi: AttendanceApi? = null
    val attendanceApi: AttendanceApi
        get() {
            if (_attendanceApi == null) _attendanceApi = retrofit.create(AttendanceApi::class.java)
            return _attendanceApi!!
        }
}
