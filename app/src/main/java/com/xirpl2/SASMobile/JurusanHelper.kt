package com.xirpl2.SASMobile

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xirpl2.SASMobile.model.Jurusan

object JurusanHelper {

    private const val PREFS_NAME = "JurusanCache"
    private const val KEY_JURUSAN_LIST = "jurusan_list"
    private const val KEY_LAST_FETCH = "last_fetch"
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    /**
     * Hardcoded fallback list used when API is unavailable.
     * Also serves as the color reference for all jurusan names.
     */
    private val FALLBACK_JURUSAN = listOf(
        Jurusan(nama = "Mekatronika", namaLengkap = "Mekatronika", warna = "#4CAF50"),
        Jurusan(nama = "TMT", namaLengkap = "Mekatronika", warna = "#4CAF50"),
        Jurusan(nama = "TKJ", namaLengkap = "Teknik Komputer dan Jaringan", warna = "#FFC107"),
        Jurusan(nama = "TEI", namaLengkap = "Teknik Elektronika Industri", warna = "#4CAF50"),
        Jurusan(nama = "Animasi", namaLengkap = "Animasi", warna = "#E91E63"),
        Jurusan(nama = "ANM", namaLengkap = "Animasi", warna = "#E91E63"),
        Jurusan(nama = "TAV", namaLengkap = "Teknik Audio Video", warna = "#81C784"),
        Jurusan(nama = "DKV", namaLengkap = "Desain Komunikasi Visual", warna = "#2196F3"),
        Jurusan(nama = "Broadcasting", namaLengkap = "Broadcasting", warna = "#F44336"),
        Jurusan(nama = "BC", namaLengkap = "Broadcasting", warna = "#F44336"),
        Jurusan(nama = "RPL", namaLengkap = "Rekayasa Perangkat Lunak", warna = "#FF9800")
    )

    private val gson = Gson()

    /**
     * Returns the hardcoded color for a given jurusan name.
     * Case-insensitive lookup against the fallback color map.
     * Returns a default blue (#2196F3) for unknown jurusan names.
     */
    fun getColorForJurusan(nama: String): String {
        val trimmed = nama.trim()
        return FALLBACK_JURUSAN
            .firstOrNull { it.nama.equals(trimmed, ignoreCase = true) }
            ?.warna
            ?: "#2196F3"
    }

    /**
     * Synchronous: returns cached jurusan list from SharedPreferences,
     * or the hardcoded fallback if no cache exists.
     */
    fun getAllJurusan(context: Context): List<Jurusan> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_JURUSAN_LIST, null)
        if (cached != null) {
            return try {
                val type = object : TypeToken<List<Jurusan>>() {}.type
                gson.fromJson<List<Jurusan>>(cached, type)
            } catch (e: Exception) {
                FALLBACK_JURUSAN
            }
        }
        return FALLBACK_JURUSAN
    }

    /**
     * Fetches jurusan list from API and caches in SharedPreferences.
     * Maps API response (id, nama) to Jurusan model using hardcoded color lookup.
     * Returns the fetched list, or null if the API call fails.
     */
    suspend fun fetchAndCache(context: Context): List<Jurusan>? {
        return withContext(Dispatchers.IO) {
            try {
                val token = getUserToken(context) ?: return@withContext null
                val response = RetrofitClient.apiService.getJurusanLookup("Bearer $token")
                if (response.isSuccessful) {
                    val apiItems = response.body()?.data ?: emptyList()
                    val jurusanList = apiItems.map { item ->
                        Jurusan(
                            nama = item.nama,
                            namaLengkap = item.nama,
                            warna = getColorForJurusan(item.nama)
                        )
                    }
                    // Cache to SharedPreferences
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString(KEY_JURUSAN_LIST, gson.toJson(jurusanList))
                        .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                        .apply()
                    jurusanList
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Checks if the cache is stale (older than CACHE_TTL_MS).
     * Returns true if a refresh is needed.
     */
    fun isCacheStale(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getLong(KEY_LAST_FETCH, 0)
        return System.currentTimeMillis() - lastFetch > CACHE_TTL_MS
    }

    private fun getUserToken(context: Context): String? {
        return try {
            val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(context)
            session.getString("auth_token", null)
        } catch (e: Exception) {
            null
        }
    }
}
