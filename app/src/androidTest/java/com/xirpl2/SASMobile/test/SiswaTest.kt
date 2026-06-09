package com.xirpl2.SASMobile.test

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.xirpl2.SASMobile.BerandaActivity
import com.xirpl2.SASMobile.ScanQrActivity
import com.xirpl2.SASMobile.PengajuanIzinActivity
import com.xirpl2.SASMobile.helper.AuthHelper
import com.xirpl2.SASMobile.helper.MockServer
import com.xirpl2.SASMobile.helper.TestData
import com.xirpl2.SASMobile.screen.SiswaBerandaScreen
import com.xirpl2.SASMobile.screen.ScanQRScreen
import com.xirpl2.SASMobile.screen.PerizinanScreen
import com.xirpl2.SASMobile.screen.ProfilScreen
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SiswaBerandaTest : TestCase() {

    private val mockServer = MockServer()
    private val screen = SiswaBerandaScreen()

    @Before
    fun setup() {
        AuthHelper.loginAsSiswiP()
        mockServer.start()
        mockServer.addResponse("GET:/api/v2/prayer-schedules/today", TestData.PRAYER_SCHEDULES_TODAY)
        mockServer.addResponse("GET:/api/v2/analytics/attendance", TestData.STATISTICS)
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, BerandaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        AuthHelper.clearSession()
    }

    @Test
    fun beranda_halamanTerbuka_titleTampil() = run {
        screen { tvPageTitle { isDisplayed() } }
    }

    @Test
    fun beranda_iconMenu_tampil() = run {
        screen { iconMenu { isDisplayed() } }
    }
}

@RunWith(AndroidJUnit4::class)
class SiswaScanTest : TestCase() {

    private val mockServer = MockServer()
    private val screen = ScanQRScreen()

    @Before
    fun setup() {
        AuthHelper.loginAsSiswiP()
        mockServer.start()
        mockServer.addResponse("POST:/api/v2/attendance/qr-codes/verify", """{"message":"berhasil"}""")
        mockServer.addResponse("POST:/api/v2/perizinan/halangan/verify", TestData.VERIFY_HALANGAN_OK)
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, ScanQrActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        AuthHelper.clearSession()
    }

    @Test
    fun scanQR_halamanTerbuka_barcodeViewTampil() = run {
        screen { barcodeView { isDisplayed() } }
    }

    @Test
    fun scanQR_btnScanTampil() = run {
        screen {
            barcodeView { isDisplayed() }
            btnScan { isDisplayed() }
        }
    }

    @Test
    fun scanQR_btnBack_bisaDiklik() = run {
        screen { btnBack { isDisplayed(); isClickable() } }
    }
}

@RunWith(AndroidJUnit4::class)
class SiswaPerizinanTest : TestCase() {

    private val mockServer = MockServer()
    private val screen = PerizinanScreen()

    @Before
    fun setup() {
        AuthHelper.loginAsSiswiP()
        mockServer.start()
        mockServer.addResponse("GET:/api/v2/pengajuan-izin", TestData.PENGAJUAN_IZIN_LIST)
        mockServer.addResponse("POST:/api/v2/pengajuan-izin", TestData.CREATE_IZIN_OK)
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, PengajuanIzinActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        AuthHelper.clearSession()
    }

    @Test
    fun perizinan_form_inputs_tampil() = run {
        screen {
            etStartDate { isDisplayed() }
            etEndDate { isDisplayed() }
            etReason { isDisplayed() }
            btnSubmit { isDisplayed() }
            btnCancel { isDisplayed() }
        }
    }

    @Test
    fun perizinan_submit_tanpaIsi_toastError() = run {
        screen { btnSubmit { click() } }
    }

    @Test
    fun perizinan_radioButtons_tampil() = run {
        screen {
            rbSakit { isDisplayed() }
            rbIzin { isDisplayed() }
        }
    }
}
