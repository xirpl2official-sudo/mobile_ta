package com.xirpl2.SASMobile

import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.StatusSholat
import com.xirpl2.SASMobile.model.JadwalSholatData
import java.util.Calendar

/**
 * Helper class untuk mengelola jadwal sholat berdasarkan jenis kelamin dan hari
 */
object JadwalSholatHelper {
    
    enum class JenisKelamin {
        LAKI_LAKI,
        PEREMPUAN
    }

    // Daftar sholat yang diizinkan untuk ditampilkan/diproses aplikasi
    val ALLOWED_PRAYERS = listOf("Dhuha", "Dzuhur", "Jumat")
    
    /**
     * Mendapatkan daftar jadwal sholat berdasarkan jenis kelamin dan hari
     * @param jenisKelamin Jenis kelamin user (dari data yang sudah ada)
     * @param calendar Calendar object untuk mengecek hari (default: hari ini)
     * @return List jadwal sholat yang sesuai
     */
    fun getJadwalSholatByGender(
        jenisKelamin: JenisKelamin,
        calendar: Calendar = Calendar.getInstance()
    ): List<String> {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isJumat = dayOfWeek == Calendar.FRIDAY
        
        return when {
            // Hari Jumat
            isJumat -> {
                when (jenisKelamin) {
                    JenisKelamin.LAKI_LAKI -> listOf("Dhuha", "Jumat") // Dhuha + Jumat for males
                    JenisKelamin.PEREMPUAN -> listOf("Dhuha", "Dzuhur") // Dhuha + Dzuhur for females
                }
            }
            // Hari selain Jumat
            else -> {
                listOf("Dhuha", "Dzuhur") // Dhuha + Dzuhur
            }
        }
    }
    
    /**
     * Get all prayers for admin dashboard view
     * Shows all prayer types based on day and gender context
     * @param calendar Calendar object untuk mengecek hari (default: hari ini)
     * @return List of all prayer names for today
     */
    fun getAdminJadwalSholat(calendar: Calendar = Calendar.getInstance()): List<String> {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isJumat = dayOfWeek == Calendar.FRIDAY
        
        return if (isJumat) {
            // On Friday: show Dhuha, Jumat (for males), Dzuhur (for females)
            listOf("Dhuha", "Jumat", "Dzuhur")
        } else {
            // Other days: show Dhuha and Dzuhur
            listOf("Dhuha", "Dzuhur")
        }
    }
    
    /**
     * Get Indonesian day name from Calendar
     */
    fun getIndonesianDay(calendar: Calendar = Calendar.getInstance()): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> "Senin"
        }
    }

    /**
     * Check if a specific day is within a range or matches exactly
     */
    fun isDayMatch(targetDay: String, dbDay: String?): Boolean {
        if (targetDay.isEmpty() || dbDay == null || dbDay.isEmpty()) return false

        // Handle format "all days"
        if (dbDay.equals("Senin-Minggu", ignoreCase = true) ||
            dbDay.equals("Semua Hari", ignoreCase = true) ||
            dbDay.equals("ALL_DAYS", ignoreCase = true) ||
            dbDay.equals("1-7", ignoreCase = true)) {
            return true
        }

        // Exact match
        if (targetDay.equals(dbDay, ignoreCase = true)) return true

        // Handle range formats
        return when (dbDay) {
            "Senin-Jumat", "1-5", "Weekdays" ->
                listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat").contains(targetDay)
            "Senin-Kamis", "1-4" ->
                listOf("Senin", "Selasa", "Rabu", "Kamis").contains(targetDay)
            "Selasa-Jumat", "2-5" ->
                listOf("Selasa", "Rabu", "Kamis", "Jumat").contains(targetDay)
            else -> false
        }
    }
    
    /**
     * Get the status of a prayer time based on current time
     */
    fun getStatusSholat(jamMulai: String, jamSelesai: String): StatusSholat {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val startParts = jamMulai.split(":")
        val endParts = jamSelesai.split(":")
        val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
        val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

        return when {
            currentTimeInMinutes < startMinutes -> StatusSholat.AKAN_DATANG
            currentTimeInMinutes in startMinutes..endMinutes -> StatusSholat.SEDANG_BERLANGSUNG
            else -> StatusSholat.SELESAI
        }
    }

    /**
     * Process list of JadwalSholatData from API to domain model with status
     */
    fun processJadwalList(
        list: List<JadwalSholatData>,
        calendar: Calendar = Calendar.getInstance()
    ): List<JadwalSholat> {
        val todayIndo = getIndonesianDay(calendar)

        return list.filter { data ->
            // 1. Filter by day matching (tetap sama)
            val dayMatch = isDayMatch(todayIndo, data.hari)

            // 2. Remove jurusan filter so all roles see all scheduled prayers for the day
            dayMatch
        }.map { data ->
            val status = getStatusSholat(data.jam_mulai, data.jam_selesai)
            JadwalSholat(
                id = data.id,
                namaSholat = data.jenis_sholat,
                jamMulai = data.jam_mulai,
                jamSelesai = data.jam_selesai,
                status = status
            )
        }
    }

    /**
     * Get upcoming or current prayer from API list.
     * Only shows a prayer if:
     * 1. It is currently active (SEDANG_BERLANGSUNG), OR
     * 2. It starts within the next 2 hours (120 minutes)
     * Returns null if no prayer is within the 2-hour window.
     */
    fun getUpcomingPrayerFromList(
        list: List<JadwalSholatData>,
        calendar: Calendar = Calendar.getInstance()
    ): JadwalSholat? {
        val todayIndo = getIndonesianDay(calendar)

        // Filter: allowed prayers + day match
        val filteredList = list.filter {
            ALLOWED_PRAYERS.any { allowed -> allowed.equals(it.jenis_sholat, ignoreCase = true) } &&
                    isDayMatch(todayIndo, it.hari)
        }

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val twoHoursAhead = currentTimeInMinutes + 120

        // PRIORITAS 1: Cari yang sedang berlangsung (SEDANG_BERLANGSUNG)
        val current = filteredList.find {
            val status = getStatusSholat(it.jam_mulai, it.jam_selesai)
            status == StatusSholat.SEDANG_BERLANGSUNG
        }
        if (current != null) {
            return JadwalSholat(
                namaSholat = current.jenis_sholat,
                jamMulai = current.jam_mulai,
                jamSelesai = current.jam_selesai,
                status = StatusSholat.SEDANG_BERLANGSUNG
            )
        }

        // PRIORITAS 2: Cari yang akan datang dalam 2 jam, urutkan berdasarkan waktu mulai
        val upcoming = filteredList.filter {
            val parts = it.jam_mulai.split(":")
            val startMinutes = parts[0].toInt() * 60 + parts[1].toInt()
            startMinutes > currentTimeInMinutes && startMinutes <= twoHoursAhead
        }.minByOrNull {
            val parts = it.jam_mulai.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        }

        if (upcoming != null) {
            return JadwalSholat(
                namaSholat = upcoming.jenis_sholat,
                jamMulai = upcoming.jam_mulai,
                jamSelesai = upcoming.jam_selesai,
                status = StatusSholat.AKAN_DATANG
            )
        }

        // Tidak ada prayer dalam window 2 jam
        return null
    }

    /**
     * Generate static jadwal sholat for notifications
     */
    fun generateJadwalSholat(jenisKelamin: JenisKelamin): List<JadwalSholat> {
        val allowedNames = getJadwalSholatByGender(jenisKelamin)
        val result = mutableListOf<JadwalSholat>()
        var idCounter = 1

        if (allowedNames.contains("Dhuha")) {
            result.add(JadwalSholat(idCounter++, "Dhuha", "06:30", "09:00", getStatusSholat("06:30", "09:00")))
        }
        if (allowedNames.contains("Dzuhur")) {
            result.add(JadwalSholat(idCounter++, "Dzuhur", "11:30", "13:00", getStatusSholat("11:30", "13:00")))
        }
        if (allowedNames.contains("Jumat")) {
            result.add(JadwalSholat(idCounter++, "Jumat", "11:30", "13:00", getStatusSholat("11:30", "13:00")))
        }

        return result
    }
}

