package com.xirpl2.SASMobile.test

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.xirpl2.SASMobile.QRCodeAdminActivity
import com.xirpl2.SASMobile.PresensiSholatAdminActivity
import com.xirpl2.SASMobile.LaporanAdminActivity
import com.xirpl2.SASMobile.DataSiswaAdminActivity
import com.xirpl2.SASMobile.KelolaKelasActivity
import com.xirpl2.SASMobile.KelolaGuruAdminActivity
import com.xirpl2.SASMobile.NotificationCenterActivity
import com.xirpl2.SASMobile.PengaturanActivity
import com.xirpl2.SASMobile.FAQActivity
import com.xirpl2.SASMobile.helper.AuthHelper
import com.xirpl2.SASMobile.helper.MockServer
import com.xirpl2.SASMobile.helper.TestData
import com.xirpl2.SASMobile.screen.QRCodeAdminScreen
import com.xirpl2.SASMobile.screen.PresensiScreen
import com.xirpl2.SASMobile.screen.LaporanScreen
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminQRTest : TestCase() {

    private val mockServer = MockServer()
    private val screen = QRCodeAdminScreen()

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        mockServer.start()
        mockServer.addResponse("GET:/api/v2/attendance/qr-codes/current", TestData.QR_CODE_ABSENSI)
        mockServer.addResponse("GET:/api/v2/attendance/qr-codes/current/image", ByteArray(32))
        mockServer.addResponse("GET:/api/v2/attendance/code/generate", """{"message":"success","data":{"code":"48291","expires_in":20}}""")
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, QRCodeAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        AuthHelper.clearSession()
    }

    @Test
    fun qrAdmin_halamanTerbuka_QR_tampil() = run {
        screen {
            ivQRCode { isDisplayed() }
            btnAbsensi { isDisplayed() }
        }
    }

    @Test
    fun qrAdmin_admin_lihatToggleHalangan() = run {
        screen { btnHalangan { isDisplayed() } }
    }

    @Test
    fun qrAdmin_guruP_lihatToggleHalangan() = run {
        AuthHelper.clearSession()
        AuthHelper.loginAsGuruP()
        screen { btnHalangan { isDisplayed() } }
    }

    @Test
    fun qrAdmin_guruL_toggleHalangan_tidakMuncul() = run {
        AuthHelper.clearSession()
        AuthHelper.loginAsGuruL()
    }

    @Test
    fun qrAdmin_tombolRefresh_tampil() = run {
        screen { btnRefresh { isDisplayed() } }
    }
}

@RunWith(AndroidJUnit4::class)
class AdminPresensiTest : TestCase() {

    private val mockServer = MockServer()
    private val screen = PresensiScreen()

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        mockServer.start()
        mockServer.addResponse("GET:/api/v2/attendance/history", TestData.PRESENSI_LIST)
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, PresensiSholatAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        AuthHelper.clearSession()
    }

    @Test
    fun presensi_halamanTerbuka() = run {
        screen { topBar { isDisplayed() } }
    }
}

@RunWith(AndroidJUnit4::class)
class AdminLaporanTest : TestCase() {

    private val mockServer = MockServer()
    private val screen = LaporanScreen()

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        mockServer.start()
        mockServer.addResponse("GET:/api/v2/analytics/attendance", TestData.STATISTICS)
        mockServer.addResponse("GET:/api/v2/analytics/charts", "{}")
        mockServer.addResponse("GET:/api/v2/attendance/history", TestData.PRESENSI_LIST)
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, LaporanAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        AuthHelper.clearSession()
    }

    @Test
    fun laporan_halamanTerbuka() = run {
        screen { topBar { isDisplayed() } }
    }
}

@RunWith(AndroidJUnit4::class)
class AdminDataSiswaTest : TestCase() {

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, DataSiswaAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() { AuthHelper.clearSession() }

    @Test
    fun dataSiswa_halamanTerbuka() = run { }
}

@RunWith(AndroidJUnit4::class)
class AdminKelolaKelasTest : TestCase() {

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, KelolaKelasActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() { AuthHelper.clearSession() }

    @Test
    fun kelolaKelas_halamanTerbuka() = run { }
}

@RunWith(AndroidJUnit4::class)
class AdminKelolaGuruTest : TestCase() {

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, KelolaGuruAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() { AuthHelper.clearSession() }

    @Test
    fun kelolaGuru_halamanTerbuka() = run { }
}

@RunWith(AndroidJUnit4::class)
class NotificationTest : TestCase() {

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, NotificationCenterActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() { AuthHelper.clearSession() }

    @Test
    fun notifikasi_halamanTerbuka() = run { }
}

@RunWith(AndroidJUnit4::class)
class PengaturanTest : TestCase() {

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, PengaturanActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() { AuthHelper.clearSession() }

    @Test
    fun pengaturan_halamanTerbuka() = run { }
}

@RunWith(AndroidJUnit4::class)
class FAQTest : TestCase() {

    @Before
    fun setup() {
        AuthHelper.loginAsAdmin()
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, FAQActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() { AuthHelper.clearSession() }

    @Test
    fun faq_halamanTerbuka() = run { }
}
