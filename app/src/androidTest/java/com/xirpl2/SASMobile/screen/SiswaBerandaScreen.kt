package com.xirpl2.SASMobile.screen

import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.screen.Screen
import com.xirpl2.SASMobile.R

class SiswaBerandaScreen : Screen<SiswaBerandaScreen>() {
    val tvPageTitle = KTextView { withId(R.id.tvPageTitle) }
    val btnAbsensi = KButton { withId(R.id.btnScanInner) }
    val iconMenu = KImageView { withId(R.id.iconMenu) }
}

class ProfilScreen : Screen<ProfilScreen>() {
    val tvProfileName = KTextView { withId(R.id.tvProfileName) }
    val tvProfileNIS = KTextView { withId(R.id.tvProfileNIS) }
    val tvProfileKelas = KTextView { withId(R.id.tvProfileKelas) }
    val btnLogout = KButton { withId(R.id.menuLogout) }
}

class ScanQRScreen : Screen<ScanQRScreen>() {
    val barcodeView = KImageView { withId(R.id.barcodeView) }
    val btnScan = KButton { withId(R.id.btnScan) }
    val cardResult = KImageView { withId(R.id.cardResult) }
    val tvStudentName = KTextView { withId(R.id.tvStudentName) }
    val tvAttendanceStatus = KTextView { withId(R.id.tvAttendanceStatus) }
    val tvPrayerType = KTextView { withId(R.id.tvPrayerType) }
    val tvStatus = KTextView { withId(R.id.tvStatus) }
    val btnBack = KImageView { withId(R.id.btnBack) }
    val progressBar = KImageView { withId(R.id.progressBarScan) }
}

class PerizinanScreen : Screen<PerizinanScreen>() {
    val etStartDate = KEditText { withId(R.id.etStartDate) }
    val etEndDate = KEditText { withId(R.id.etEndDate) }
    val etReason = KEditText { withId(R.id.etReason) }
    val rbSakit = KTextView { withId(R.id.rbSakit) }
    val rbIzin = KTextView { withId(R.id.rbIzin) }
    val btnSubmit = KButton { withId(R.id.btnSubmit) }
    val btnCancel = KButton { withId(R.id.btnCancel) }
}
