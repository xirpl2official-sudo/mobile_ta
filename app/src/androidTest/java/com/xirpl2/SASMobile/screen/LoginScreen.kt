package com.xirpl2.SASMobile.screen

import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.screen.Screen
import com.xirpl2.SASMobile.R

class LoginScreen : Screen<LoginScreen>() {
    val etNIS = KEditText { withId(R.id.et_nis) }
    val etPassword = KEditText { withId(R.id.et_password) }
    val btnLogin = KButton { withId(R.id.btn_masuk) }
    val btnRegister = KTextView { withId(R.id.textBuatAkun) }
    val btnForgotPassword = KTextView { withId(R.id.textLupaPassword) }
}
