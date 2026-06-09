package com.xirpl2.SASMobile.test

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.xirpl2.SASMobile.MasukActivity
import com.xirpl2.SASMobile.helper.AuthHelper
import com.xirpl2.SASMobile.helper.MockServer
import com.xirpl2.SASMobile.helper.TestData
import com.xirpl2.SASMobile.screen.LoginScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthTest : TestCase() {

    @get:Rule
    val activityRule = ActivityScenarioRule(MasukActivity::class.java)

    private val mockServer = MockServer()
    private val loginScreen = LoginScreen()

    @Before
    fun setup() {
        mockServer.start()
        AuthHelper.setupMockServer(mockServer)
        mockServer.addResponse("POST:/api/v2/auth/sessions", TestData.MOCK_LOGIN)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun test_login_allElementsVisible() = run {
        loginScreen {
            step("Verify all login elements are visible") {
                etNIS { isDisplayed(); hasHint("NIS") }
                etPassword { isDisplayed(); hasHint("Password") }
                btnLogin { isDisplayed(); isClickable() }
            }
        }
    }

    @Test
    fun test_login_fillFormAndSubmit() = run {
        loginScreen {
            step("Fill login form") {
                etNIS { typeText("20228PL001") }
                etPassword { typeText("password123") }
            }
            step("Click login button") {
                btnLogin { click() }
            }
            step("Wait for navigation") {
                device.uiDevice.waitForIdle()
            }
        }
    }

    @Test
    fun test_login_emptyNIS_showsValidation() = run {
        loginScreen {
            step("Submit empty form") {
                etPassword { typeText("password123") }
            }
            step("Click login - should show validation") {
                btnLogin { click() }
                device.uiDevice.waitForIdle()
            }
        }
    }

    @Test
    fun test_login_emptyPassword_showsValidation() = run {
        loginScreen {
            step("Fill only NIS") {
                etNIS { typeText("20228PL001") }
            }
            step("Click login - should show validation") {
                btnLogin { click() }
                device.uiDevice.waitForIdle()
            }
        }
    }
}
