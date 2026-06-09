package com.xirpl2.SASMobile.helper

import androidx.test.platform.app.InstrumentationRegistry
import com.xirpl2.SASMobile.network.RetrofitClient

object AuthHelper {

    fun setupMockServer(mockServer: MockServer) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val baseUrl = mockServer.baseUrl.ensureSlash()

        RetrofitClient.resetForTest(baseUrl, ctx)

        // Auth endpoints
        mockServer.addResponse("POST:/api/v2/auth/sessions", TestData.MOCK_LOGIN)
        mockServer.addResponse("GET:/api/v2/auth/profile", TestData.MOCK_PROFILE)

        // Prayer schedules
        mockServer.addResponse("GET:/api/v2/prayer-schedules/today", TestData.MOCK_PRAYER_TODAY)
        mockServer.addResponse("GET:/api/v2/prayer-schedules/closest", TestData.MOCK_CLOSEST)

        // Attendance
        mockServer.addResponse("GET:/api/v2/attendance/history", TestData.MOCK_ATTENDANCE)
        mockServer.addResponse("POST:/api/v2/attendance/qr-codes/verify", TestData.MOCK_QR_VERIFY)
        mockServer.addResponse("GET:/api/v2/attendance/qr-codes/current", TestData.MOCK_QR_CURRENT)
        mockServer.addResponse("GET:/api/v2/attendance/qr-codes/current/image", TestData.MOCK_QR_PNG_BYTES)
        mockServer.addResponse("GET:/api/v2/attendance/qr-codes/current/halangan", TestData.MOCK_QR_PNG_BYTES)
        mockServer.addResponse("GET:/api/v2/attendance/code/generate", TestData.MOCK_CODE_GEN)

        // Analytics
        mockServer.addResponse("GET:/api/v2/analytics/attendance", TestData.MOCK_STATS)
        mockServer.addResponse("GET:/api/v2/analytics/charts", TestData.MOCK_CHARTS)
        mockServer.addResponse("GET:/api/v2/analytics/pending-attendance", TestData.MOCK_PENDING)

        // Students
        mockServer.addResponse("GET:/api/v2/students", TestData.MOCK_SISWA_LIST)
        mockServer.addResponse("GET:/api/v2/students/filters", TestData.MOCK_FILTERS)
        mockServer.addResponse("GET:/api/v2/students/unregistered", TestData.MOCK_SISWA_LIST)
        mockServer.addResponse("GET:/api/v2/students/me/attendance-history", TestData.MOCK_SISWA_ATTENDANCE)

        // Perizinan
        mockServer.addResponse("GET:/api/v2/pengajuan-izin", TestData.MOCK_IZIN_LIST)
        mockServer.addResponse("POST:/api/v2/pengajuan-izin", TestData.MOCK_IZIN_CREATE)

        // Halangan
        mockServer.addResponse("POST:/api/v2/perizinan/halangan/verify", TestData.MOCK_HALANGAN_OK)
        mockServer.addResponse("POST:/api/v2/perizinan/halangan/request", TestData.MOCK_HALANGAN_OK)
        mockServer.addResponse("GET:/api/v2/perizinan/halangan/status", TestData.MOCK_HALANGAN_ACTIVE)

        // Notifications
        mockServer.addResponse("GET:/api/v2/notifications", TestData.MOCK_NOTIFS)
        mockServer.addResponse("GET:/api/v2/users/me/notifications", TestData.MOCK_NOTIFS)

        // Kelas & Jurusan
        mockServer.addResponse("GET:/api/v2/kelas", TestData.MOCK_KELAS)
        mockServer.addResponse("GET:/api/v2/jurusan", TestData.MOCK_JURUSAN)
        mockServer.addResponse("GET:/api/v2/admin/management/kelas", TestData.MOCK_KELAS_MGMT)
        mockServer.addResponse("GET:/api/v2/admin/management/guru", TestData.MOCK_GURU_LIST)
        mockServer.addResponse("GET:/api/v2/admin/management/wali-kelas", TestData.MOCK_WALI_LIST)
        mockServer.addResponse("GET:/api/v2/admin/student-control/overview", TestData.MOCK_OVERVIEW)
        mockServer.addResponse("GET:/api/v2/lookup/staff-guru", TestData.MOCK_STAFF_LOOKUP)
        mockServer.addResponse("GET:/api/v2/prayer-schedules", TestData.MOCK_SCHEDULES)
        mockServer.addResponse("GET:/api/v2/prayer-schedules/dhuha/today", TestData.MOCK_DHUHA_TODAY)
        mockServer.addResponse("GET:/api/v2/data-retention/backups/status", TestData.MOCK_BACKUP_STATUS)
    }

    private fun String.ensureSlash(): String = if (endsWith("/")) this else "$this/"
}
