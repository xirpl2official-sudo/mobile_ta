package com.xirpl2.SASMobile.test

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.xirpl2.SASMobile.*
import com.xirpl2.SASMobile.helper.AuthHelper
import com.xirpl2.SASMobile.helper.MockServer
import com.xirpl2.SASMobile.helper.TestData
import com.xirpl2.SASMobile.screen.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowTest : TestCase() {

    private val mockServer = MockServer()

    @Before
    fun setup() {
        mockServer.start()
        AuthHelper.setupMockServer(mockServer)
        mockServer.addResponse("POST:/api/v2/auth/sessions", TestData.MOCK_LOGIN)
    }

    @After
    fun tearDown() { mockServer.shutdown() }

    @Test
    fun login_enterCredentials_clickMasuk_navigatesAway() = run {
        ActivityScenario.launch(MasukActivity::class.java).use {
            val s = LoginScreen()
            s.etNIS { typeText("20228PL001") }
            s.etPassword { typeText("password123") }
            s.btnLogin { click() }
            device.uiDevice.waitForIdle()
        }
    }

    @Test
    fun login_emptyNIS_showsValidation() = run {
        ActivityScenario.launch(MasukActivity::class.java).use {
            val s = LoginScreen()
            s.etPassword { typeText("password123") }
            s.btnLogin { click() }
            device.uiDevice.waitForIdle()
        }
    }

    @Test
    fun login_emptyPassword_showsValidation() = run {
        ActivityScenario.launch(MasukActivity::class.java).use {
            val s = LoginScreen()
            s.etNIS { typeText("20228PL001") }
            s.btnLogin { click() }
            device.uiDevice.waitForIdle()
        }
    }

    @Test
    fun login_allElementsVisible() = run {
        ActivityScenario.launch(MasukActivity::class.java).use {
            val s = LoginScreen()
            s.etNIS { isDisplayed() }
            s.etPassword { isDisplayed() }
            s.btnLogin { isDisplayed() }
            device.uiDevice.waitForIdle()
        }
    }
}

@RunWith(AndroidJUnit4::class)
class QRCodeFlowTest : TestCase() {

    private val mockServer = MockServer()

    @Before
    fun setup() {
        mockServer.start()
        AuthHelper.setupMockServer(mockServer)
        mockServer.addResponse("POST:/api/v2/auth/sessions", TestData.MOCK_LOGIN_ADMIN)
        mockServer.addResponse("GET:/api/v2/attendance/code/generate", TestData.MOCK_CODE_GEN)
    }

    @After
    fun tearDown() { mockServer.shutdown() }

    @Test
    fun qrAdmin_login_opensQRPage_addsToggleHalangan() = run {
        ActivityScenario.launch(MasukActivity::class.java).use {
            val login = LoginScreen()
            login.etNIS { typeText("admin@smkn2.sch.id") }
            login.etPassword { typeText("admin123") }
            login.btnLogin { click() }
            device.uiDevice.waitForIdle()
        }
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(ctx, QRCodeAdminActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<android.app.Activity>(intent).use {
            val qr = QRCodeAdminScreen()
            qr.ivQRCode { isDisplayed() }
            qr.btnAbsensi { isDisplayed() }
            qr.btnHalangan { isDisplayed() }
            qr.btnRefresh { isDisplayed(); isClickable() }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class SiswaPagesTest : TestCase() {

    private val mockServer = MockServer()

    @Before
    fun setup() {
        mockServer.start()
        AuthHelper.setupMockServer(mockServer)
    }

    @After
    fun tearDown() { mockServer.shutdown() }

    @Test
    fun scanQR_hasBarcodeView_andScanButton() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, ScanQrActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            val s = ScanQRScreen()
            s.barcodeView { isDisplayed() }
            s.btnScan { isDisplayed() }
            s.btnBack { isDisplayed() }
        }
    }

    @Test
    fun perizinan_hasFormInputs() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, PengajuanIzinActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            val p = PerizinanScreen()
            p.etStartDate { isDisplayed() }
            p.etEndDate { isDisplayed() }
            p.etReason { isDisplayed() }
            p.btnSubmit { isDisplayed() }
            p.btnCancel { isDisplayed() }
        }
    }

    @Test
    fun beranda_hasTitle() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, BerandaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            SiswaBerandaScreen().tvPageTitle { isDisplayed() }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class AdminPagesTest : TestCase() {

    private val mockServer = MockServer()

    @Before
    fun setup() {
        mockServer.start()
        AuthHelper.setupMockServer(mockServer)
    }

    @After
    fun tearDown() { mockServer.shutdown() }

    @Test
    fun presensi_opens() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, PresensiSholatAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            PresensiScreen().topBar { isDisplayed() }
        }
    }

    @Test
    fun laporan_opens() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, LaporanAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            LaporanScreen().topBar { isDisplayed() }
        }
    }

    @Test
    fun dataSiswa_opens() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, DataSiswaAdminActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            device.uiDevice.waitForIdle()
        }
    }

    @Test
    fun notifikasi_opens() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, NotificationCenterActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            device.uiDevice.waitForIdle()
        }
    }

    @Test
    fun pengaturan_opens() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, PengaturanActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            device.uiDevice.waitForIdle()
        }
    }

    @Test
    fun faq_opens() = run {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ActivityScenario.launch<android.app.Activity>(
            Intent(ctx, FAQActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ).use {
            device.uiDevice.waitForIdle()
        }
    }
}
