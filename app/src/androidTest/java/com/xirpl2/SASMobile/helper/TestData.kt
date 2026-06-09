package com.xirpl2.SASMobile.helper

object TestData {

    const val VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"

    val LOGIN_SISWI = """{"message":"Login berhasil","data":{"token":"$VALID_TOKEN","role":"siswa","nis":"20228PL001","nama_siswa":"Siti Aminah","jk":"P","jurusan":"RPL","kelas":"11 RPL A","email":"siti@smkn2.sch.id"}}"""

    val LOGIN_SISWA_L = """{"message":"Login berhasil","data":{"token":"$VALID_TOKEN","role":"siswa","nis":"20228PL002","nama_siswa":"Ahmad Fauzi","jk":"L","jurusan":"RPL","kelas":"11 RPL A","email":"ahmad@smkn2.sch.id"}}"""

    val LOGIN_ADMIN = """{"message":"Login berhasil","data":{"token":"$VALID_TOKEN","role":"admin","username":"admin@smkn2.sch.id","nama":"Administrator","email":"admin@smkn2.sch.id","nip":"198501012020011001"}}"""

    val LOGIN_GURU_P = """{"message":"Login berhasil","data":{"token":"$VALID_TOKEN","role":"guru","username":"gurup@smkn2.sch.id","nama":"Ustadzah Aisyah","email":"gurup@smkn2.sch.id","nip":"198601012020012002","jk":"P"}}"""

    val LOGIN_GURU_L = """{"message":"Login berhasil","data":{"token":"$VALID_TOKEN","role":"guru","username":"gurul@smkn2.sch.id","nama":"Ustadz Ahmad","email":"gurul@smkn2.sch.id","nip":"198701012020012003","jk":"L"}}"""

    val LOGIN_WALI_KELAS = """{"message":"Login berhasil","data":{"token":"$VALID_TOKEN","role":"wali_kelas","username":"wali@smkn2.sch.id","nama":"Bu Wali","email":"wali@smkn2.sch.id","nip":"198801012020012004","jk":"P"}}"""

    val PROFILE_SISWI = """{"data":{"nis":"20228PL001","nama_siswa":"Siti Aminah","jk":"P","jurusan":"RPL","kelas":"11 RPL A","email":"siti@smkn2.sch.id","role":"siswa"}}"""

    val PROFILE_ADMIN = """{"data":{"username":"admin@smkn2.sch.id","nama":"Administrator","nip":"198501012020011001","role":"admin","email":"admin@smkn2.sch.id"}}"""

    val PRAYER_SCHEDULES_TODAY = """{"message":"success","data":[{"id_jadwal":1,"hari":"Senin","waktu_sholat":{"id_jenis":1,"nama_jenis":"Dhuha","waktu_mulai":"06:00","waktu_selesai":"11:30"},"jurusans":[{"id_jurusan":1,"nama_jurusan":"RPL"}]},{"id_jadwal":2,"hari":"Senin","waktu_sholat":{"id_jenis":2,"nama_jenis":"Dhuhur","waktu_mulai":"12:00","waktu_selesai":"15:00"},"jurusans":[]}]}"""

    val ATTENDANCE_HISTORY = """{"message":"success","data":{"periode":"Minggu ini","absensi":[{"id_absen":1,"status":"hadir","tanggal":"2026-06-08","jenis_sholat":"Dhuha"},{"id_absen":2,"status":"hadir","tanggal":"2026-06-08","jenis_sholat":"Dhuhur"}]}}"""

    val STATISTICS = """{"message":"success","data":{"total_siswa":500,"hadir":450,"izin":30,"sakit":10,"alpha":10}}"""

    val QR_CODE_ABSENSI = """{"message":"success","data":{"qr_code":"ABSEN:test123","jenis_sholat":"Dhuha","expires_at":"2026-06-09T07:00:00Z"}}"""

    val VERIFY_HALANGAN_OK = """{"status":"success","message":"Izin halangan tercatat","data":{"id":1,"tanggal_mulai":"2026-06-08","tanggal_selesai":"2026-06-22","berlaku":"2026-06-08 s/d 2026-06-22"}}"""

    val VERIFY_HALANGAN_COOLDOWN = """{"status":"error","message":"Masa suci minimal 15 hari belum terpenuhi."}"""

    val PENGAJUAN_IZIN_LIST = """{"message":"success","data":[{"id_pengajuan":1,"jenis_izin":"sakit","tanggal_awal":"2026-06-08","tanggal_akhir":"2026-06-10","status":"disetujui","keterangan":"Sakit demam"}],"pagination":null}"""

    val CREATE_IZIN_OK = """{"message":"Pengajuan izin berhasil dikirim"}"""

    val NOTIFICATIONS = """{"message":"success","data":[{"id":1,"title":"Pengumuman","message":"Jadwal sholat telah diperbarui","type":"info","is_read":false,"created_at":"2026-06-08T06:00:00Z"}],"pagination":null}"""

    val KELAS_LIST = """{"message":"success","data":[{"id_kelas":1,"tingkatan":10,"part":"A","nama_jurusan":"RPL"}]}"""

    val REPORT_EXCEL_BYTES = "PK\u0003\u0004".toByteArray()

    val SISWA_LIST = """{"message":"success","data":{"students":{"data":[{"nis":"20228PL001","nama_siswa":"Siti Aminah","jk":"P","kelas":"11 RPL A","jurusan":"RPL"}],"pagination":{"current_page":1,"total":1,"per_page":100}},"pagination":{"total":1,"current_page":1,"per_page":100}}}"""

    val PRESENSI_LIST = """{"message":"success","data":{"absensi":[{"id_absen":1,"nis":"20228PL001","nama_siswa":"Siti Aminah","kelas":"11 RPL","jurusan":"RPL","jenis_sholat":"Dhuha","tanggal":"2026-06-08","status":"hadir"}],"pagination":{"total":1}}}"""

    val GURU_LIST = """{"message":"success","data":[],"pagination":{"total":0}}"""

    val CHART_DATA = """{"data":{}}"""
}
