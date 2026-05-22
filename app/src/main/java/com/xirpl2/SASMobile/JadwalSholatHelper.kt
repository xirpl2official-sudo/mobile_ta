package com.xirpl2.SASMobile

import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.StatusSholat
import com.xirpl2.SASMobile.model.JadwalSholatData
import java.util.Calendar

object JadwalSholatHelper {
    
    enum class JenisKelamin {
        LAKI_LAKI,
        PEREMPUAN
    }

    
    val ALLOWED_PRAYERS = listOf("Dhuha", "Dzuhur", "Jumat")
    
    fun getJadwalSholatByGender(
        jenisKelamin: JenisKelamin,
        calendar: Calendar = Calendar.getInstance()
    ): List<String> {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isJumat = dayOfWeek == Calendar.FRIDAY
        
        return when {
            
            isJumat -> {
                when (jenisKelamin) {
                    JenisKelamin.LAKI_LAKI -> listOf("Dhuha", "Jumat") 
                    JenisKelamin.PEREMPUAN -> listOf("Dhuha", "Dzuhur") 
                }
            }
            
            else -> {
                listOf("Dhuha", "Dzuhur") 
            }
        }
    }
    
    fun getAdminJadwalSholat(calendar: Calendar = Calendar.getInstance()): List<String> {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isJumat = dayOfWeek == Calendar.FRIDAY
        
        return if (isJumat) {
            
            listOf("Dhuha", "Jumat", "Dzuhur")
        } else {
            
            listOf("Dhuha", "Dzuhur")
        }
    }
    
    fun getIndonesianDay(calendar: Calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta"))): String {
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

    fun isDayMatch(targetDay: String, dbDay: String?): Boolean {
        if (targetDay.isEmpty() || dbDay == null || dbDay.isEmpty()) return false

        
        if (dbDay.equals("Senin-Minggu", ignoreCase = true) ||
            dbDay.equals("Semua Hari", ignoreCase = true) ||
            dbDay.equals("ALL_DAYS", ignoreCase = true) ||
            dbDay.equals("1-7", ignoreCase = true)) {
            return true
        }

        
        if (targetDay.equals(dbDay, ignoreCase = true)) return true

        
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
    
    fun getStatusSholat(jamMulai: String?, jamSelesai: String?): StatusSholat {
        if (jamMulai.isNullOrBlank() || jamSelesai.isNullOrBlank()) return StatusSholat.SELESAI

        return try {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            val startParts = jamMulai.split(":")
            if (startParts.size < 2) return StatusSholat.SELESAI

            val endParts = jamSelesai.split(":")
            if (endParts.size < 2) return StatusSholat.SELESAI

            val startMinutes = startParts[0].toIntOrNull()?.let { it * 60 + (startParts[1].toIntOrNull() ?: 0) } ?: 0
            val endMinutes = endParts[0].toIntOrNull()?.let { it * 60 + (endParts[1].toIntOrNull() ?: 0) } ?: 0

            // Tampilkan jadwal 30 menit sebelum waktu_mulai
            val adjustedStart = (startMinutes - 30).coerceAtLeast(0)

            when {
                currentTimeInMinutes < adjustedStart -> StatusSholat.AKAN_DATANG
                currentTimeInMinutes in adjustedStart..endMinutes -> StatusSholat.SEDANG_BERLANGSUNG
                else -> StatusSholat.SELESAI
            }
        } catch (e: Exception) {
            StatusSholat.SELESAI
        }
    }

    fun processJadwalList(
        list: List<JadwalSholatData>,
        calendar: Calendar = Calendar.getInstance()
    ): List<JadwalSholat> {
        val todayIndo = getIndonesianDay(calendar)

        return list.filter { data ->
            isDayMatch(todayIndo, data.hari)
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

    fun getUpcomingPrayerFromList(
        list: List<JadwalSholatData>,
        calendar: Calendar = Calendar.getInstance()
    ): JadwalSholat? {
        val todayIndo = getIndonesianDay(calendar)

        val filteredList = list.filter {
            !it.jenis_sholat.isNullOrBlank() &&
            ALLOWED_PRAYERS.any { allowed -> allowed.equals(it.jenis_sholat, ignoreCase = true) } &&
                    isDayMatch(todayIndo, it.hari)
        }
        
        if (filteredList.isEmpty()) return null

        val now = Calendar.getInstance()
        val currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // 1. Check for currently ongoing prayer
        val current = filteredList.find {
            getStatusSholat(it.jam_mulai, it.jam_selesai) == StatusSholat.SEDANG_BERLANGSUNG
        }
        if (current != null) {
            return JadwalSholat(
                namaSholat = current.jenis_sholat,
                jamMulai = current.jam_mulai,
                jamSelesai = current.jam_selesai,
                status = StatusSholat.SEDANG_BERLANGSUNG
            )
        }

        // 2. Sort by start time
        val sortedList = filteredList.sortedBy {
            val parts = it.jam_mulai.split(":")
            if (parts.size >= 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            } else 0
        }

        // 3. Find first upcoming (30 menit sebelum waktu_mulai)
        val upcoming = sortedList.find {
            val parts = it.jam_mulai.split(":")
            if (parts.size >= 2) {
                val startMinutes = (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
                val adjustedStart = (startMinutes - 30).coerceAtLeast(0)
                adjustedStart > currentTimeInMinutes
            } else false
        }

        // 4. No upcoming prayer found — all have passed
        if (upcoming == null) return null

        return JadwalSholat(
            namaSholat = upcoming.jenis_sholat,
            jamMulai = upcoming.jam_mulai,
            jamSelesai = upcoming.jam_selesai,
            status = StatusSholat.AKAN_DATANG
        )
    }

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

