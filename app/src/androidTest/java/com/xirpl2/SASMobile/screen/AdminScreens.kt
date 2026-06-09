package com.xirpl2.SASMobile.screen

import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.screen.Screen
import com.xirpl2.SASMobile.R

class QRCodeAdminScreen : Screen<QRCodeAdminScreen>() {
    val btnMenu = KImageView { withId(R.id.iconMenu) }
    val btnAbsensi = KButton { withId(R.id.btnAbsensi) }
    val btnHalangan = KButton { withId(R.id.btnHalangan) }
    val ivQRCode = KImageView { withId(R.id.ivQRCode) }
    val tvJenisSholat = KTextView { withId(R.id.tvJenisSholat) }
    val btnRefresh = KButton { withId(R.id.btnRefresh) }
    val tvStatus = KTextView { withId(R.id.tvStatus) }
    val progressBar = KImageView { withId(R.id.progressBar) }
}

class BerandaAdminScreen : Screen<BerandaAdminScreen>() {
    val btnMenu = KImageView { withId(R.id.iconMenu) }
}

class PresensiScreen : Screen<PresensiScreen>() {
    val topBar = KTextView { withId(R.id.tvPageTitle) }
}

class LaporanScreen : Screen<LaporanScreen>() {
    val topBar = KTextView { withId(R.id.tvPageTitle) }
}
