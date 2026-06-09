package com.xirpl2.SASMobile.helper

object TestData {

    const val TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token.here"

    // === AUTH ===
    val MOCK_LOGIN = """{"message":"Login berhasil","data":{"token":"$TOKEN","role":"siswa","nis":"20228PL001","nama_siswa":"Siti Aminah","jk":"P","jurusan":"RPL","kelas":"11 RPL A","email":"siti@smkn2.sch.id","is_verified":true}}"""
    val MOCK_LOGIN_ADMIN = """{"message":"Login berhasil","data":{"token":"$TOKEN","role":"admin","username":"admin@smkn2.sch.id","nama":"Administrator","email":"admin@smkn2.sch.id","nip":"198501012020011001"}}"""
    val MOCK_PROFILE = """{"data":{"nis":"20228PL001","nama_siswa":"Siti Aminah","jk":"P","jurusan":"RPL","kelas":"11 RPL A","email":"siti@smkn2.sch.id","role":"siswa"}}"""

    // === PRAYER ===
    val MOCK_PRAYER_TODAY = """{"message":"success","data":[{"id_jadwal":1,"hari":"Senin","waktu_sholat":{"id_jenis":1,"nama_jenis":"Dhuha","waktu_mulai":"06:00","waktu_selesai":"11:30"}},{"id_jadwal":2,"hari":"Senin","waktu_sholat":{"id_jenis":2,"nama_jenis":"Dhuhur","waktu_mulai":"12:00","waktu_selesai":"15:00"}}]}"""
    val MOCK_CLOSEST = """{"message":"success","data":{"id_jadwal":1,"jenis_sholat":"Dhuha","waktu_mulai":"06:00","waktu_selesai":"11:30","hari":"Senin"}}"""

    // === ATTENDANCE ===
    val MOCK_ATTENDANCE = """{"message":"success","data":{"absensi":[{"id_absen":1,"nis":"20228PL001","nama_siswa":"Siti Aminah","kelas":"11 RPL","jurusan":"RPL","jenis_sholat":"Dhuha","tanggal":"2026-06-08","status":"hadir"}],"pagination":{"total":1,"current_page":1,"per_page":100}}}"""
    val MOCK_QR_VERIFY = """{"message":"Kehadiran berhasil dicatat!","data":{"valid":true,"nis":"20228PL001","nama_siswa":"Siti Aminah","kelas":"11 RPL","jurusan":"RPL","jenis_sholat":"Dhuha","tanggal":"2026-06-08","status":"hadir"}}"""
    val MOCK_QR_CURRENT = """{"message":"success","data":{"qr_code":"ABSEN:test123456","jenis_sholat":"Dhuha","expires_at":"2026-06-09T07:00:00Z"}}"""
    val MOCK_QR_PNG_BYTES = ByteArray(128) { (it % 26 + 65).toByte() }
    val MOCK_CODE_GEN = """{"message":"success","data":{"code":"48291","expires_in":20}}"""
    val MOCK_SISWA_ATTENDANCE = """{"message":"success","data":{"periode":"Minggu ini","absensi":[{"id_absen":1,"status":"hadir","tanggal":"2026-06-08","jenis_sholat":"Dhuha"},{"id_absen":2,"status":"hadir","tanggal":"2026-06-08","jenis_sholat":"Dhuhur"}]}}"""

    // === ANALYTICS ===
    val MOCK_STATS = """{"message":"success","data":{"total_siswa":500,"hadir":450,"izin":30,"sakit":10,"alpha":10}}"""
    val MOCK_CHARTS = """{"data":{"daily":[{"date":"2026-06-01","hadir":100,"izin":5,"alpha":2}],"by_sholat":[{"jenis":"Dhuha","hadir":200},{"jenis":"Dhuhur","hadir":250}]}}"""
    val MOCK_PENDING = """{"message":"success","data":{"pending":5,"siswa":[]}}"""

    // === SISWA ===
    val MOCK_SISWA_LIST = """{"message":"success","data":{"students":{"data":[{"nis":"20228PL001","nama_siswa":"Siti Aminah","jk":"P","kelas":"11 RPL A","jurusan":"RPL"},{"nis":"20228PL002","nama_siswa":"Ahmad Fauzi","jk":"L","kelas":"11 RPL A","jurusan":"RPL"}],"pagination":{"current_page":1,"total":2,"per_page":100}},"pagination":{"total":2}}}"""
    val MOCK_FILTERS = """{"data":{"kelas":["10","11","12"],"jurusan":["RPL","TKJ","TEI","TAV","DKV","ANM","BC","TMT"]}}"""

    // === PERIZINAN ===
    val MOCK_IZIN_LIST = """{"message":"success","data":[{"id_pengajuan":1,"id_siswa":1,"jenis_izin":"sakit","tanggal_awal":"2026-06-08","tanggal_akhir":"2026-06-10","status":"disetujui","keterangan":"Sakit demam"}],"pagination":null}"""
    val MOCK_IZIN_CREATE = """{"message":"Pengajuan izin berhasil dikirim"}"""

    // === HALANGAN ===
    val MOCK_HALANGAN_OK = """{"status":"success","message":"Izin halangan tercatat","data":{"id":1,"tanggal_mulai":"2026-06-08","tanggal_selesai":"2026-06-22","berlaku":"2026-06-08 s/d 2026-06-22"}}"""
    val MOCK_HALANGAN_ACTIVE = """{"status":"success","data":{"active":true,"perizinan":{"id":1,"siswa_id":100,"tanggal_mulai":"2026-06-08","tanggal_selesai":"2026-06-22","status_validasi":"approved"}}}"""

    // === NOTIFICATIONS ===
    val MOCK_NOTIFS = """{"message":"success","data":[{"id":1,"title":"Pengumuman","message":"Jadwal sholat telah diperbarui","type":"info","is_read":false,"created_at":"2026-06-08T06:00:00Z"}],"pagination":null}"""

    // === LOOKUPS ===
    val MOCK_KELAS = """{"message":"success","data":[{"id_kelas":1,"tingkatan":10,"part":"A","nama_jurusan":"RPL"}]}"""
    val MOCK_JURUSAN = """{"message":"success","data":[{"id_jurusan":1,"nama_jurusan":"RPL"},{"id_jurusan":2,"nama_jurusan":"TKJ"}]}"""
    val MOCK_KELAS_MGMT = """{"message":"success","data":[{"id_kelas":1,"tingkatan":11,"part":"A","nama_jurusan":"RPL","nama_wali_kelas":"Bu Wali","jumlah_siswa":30}]}"""
    val MOCK_GURU_LIST = """{"message":"success","data":[{"id_guru":1,"id_staff":1,"nip":"198601012020012002","nama":"Ustadzah Aisyah","kelas_wali":"11 RPL A"}],"pagination":{"total":1}}"""
    val MOCK_WALI_LIST = """{"message":"success","data":[],"pagination":{"total":0}}"""
    val MOCK_OVERVIEW = """{"message":"success","data":{"total_siswa":500,"siswa_aktif":480,"siswa_lulus":20}}"""
    val MOCK_STAFF_LOOKUP = """{"message":"success","data":[{"id":1,"nama":"Ustadzah Aisyah","nip":"198601012020012002","tipe_staff":"guru"}]}"""
    val MOCK_SCHEDULES = """{"message":"success","data":[],"pagination":{"total":0}}"""
    val MOCK_DHUHA_TODAY = """{"message":"success","data":[]}"""
    val MOCK_BACKUP_STATUS = """{"message":"success","data":{"backed_up":false}}"""
}
