package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class JurusanListResponse(
    val data: List<JurusanItem> = emptyList()
)

data class JurusanItem(
    @SerializedName("id_jurusan")
    val id: Int,
    @SerializedName("nama_jurusan")
    val nama: String
)

data class KelasListResponse(
    val data: List<KelasItem> = emptyList()
)

data class KelasItem(
    val id_kelas: Int,
    val id_jurusan: Int,
    val jurusan: String,
    val label: String,
    val part: String,
    val tingkatan: Int
)

data class StaffLookupResponse(
    val data: List<StaffInfo> = emptyList()
)
