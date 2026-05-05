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

    fun processJadwalList(
        list: List<JadwalSholatData>,
        calendar: Calendar = Calendar.getInstance()
    ): List<JadwalSholat> {
        val todayIndo = getIndonesianDay(calendar)

        return list.filter { data ->
            
            val dayMatch = isDayMatch(todayIndo, data.hari)

            
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

    fun getUpcomingPrayerFromList(
        list: List<JadwalSholatData>,
        calendar: Calendar = Calendar.getInstance()
    ): JadwalSholat? {
        val todayIndo = getIndonesianDay(calendar)

        
        val filteredList = list.filter {
            ALLOWED_PRAYERS.any { allowed -> allowed.equals(it.jenis_sholat, ignoreCase = true) } &&
                    isDayMatch(todayIndo, it.hari)
        }

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val twoHoursAhead = currentTimeInMinutes + 120

        
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

        
        return null
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

