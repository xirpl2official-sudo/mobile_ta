package com.xirpl2.SASMobile.model

data class StatistikAdmin(
    val totalSiswa: Int,
    val totalHadirHariIni: Int,
    val izinSakit: Int,
    val persentaseKehadiran: Int // dalam persen (0-100)
)