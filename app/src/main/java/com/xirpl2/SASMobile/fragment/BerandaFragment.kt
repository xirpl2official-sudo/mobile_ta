package com.xirpl2.SASMobile.fragment

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.*
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.StudentMainActivity
import androidx.navigation.fragment.findNavController
import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.StatusSholat
import com.xirpl2.SASMobile.model.RiwayatAbsensi
import com.xirpl2.SASMobile.model.StatusAbsensi
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.utils.NotificationCounterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BerandaFragment : Fragment(R.layout.fragment_beranda) {

    private lateinit var rvJadwalSholat: RecyclerView
    private lateinit var rvRiwayatAbsensi: RecyclerView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvAlphaValue: TextView
    private lateinit var tvIzinSakitValue: TextView
    private lateinit var tvJadwalError: TextView
    private lateinit var tvRiwayatError: TextView
    private lateinit var cardWaktuAbsensi: androidx.cardview.widget.CardView
    private lateinit var notificationBellContainer: FrameLayout
    private lateinit var tvNotificationBadge: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var jadwalAdapter: JadwalSholatAdapter
    private lateinit var riwayatAdapter: RiwayatAbsensiAdapter
    private val repository = BerandaRepository()
    private var isDataLoaded = false

    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var customDateContainer: LinearLayout
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnApplyCustomDate: com.google.android.material.button.MaterialButton
    private var currentFilter: String = ""
    private var customStartDate: String? = null
    private var customEndDate: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyEdgeToEdge(view)
        initializeViews(view)
        setupJadwalSholat()
        setupRiwayatAbsensi()
        setupAbsensiButton(view)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadAllData() }
    }

    override fun onResume() {
        super.onResume()
        if (!isAdded) return
        NotificationCounterManager.syncFromPreferences(requireContext())
        if (!isDataLoaded) { isDataLoaded = true; loadAllData() }
    }

    override fun onPause() { super.onPause(); isDataLoaded = false }

    private fun initializeViews(view: View) {
        rvJadwalSholat = view.findViewById(R.id.rvJadwalSholat)
        rvRiwayatAbsensi = view.findViewById(R.id.rvRiwayatAbsensi)
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvAlphaValue = view.findViewById(R.id.tvAlphaValue)
        tvIzinSakitValue = view.findViewById(R.id.tvIzinSakitValue)
        tvJadwalError = view.findViewById(R.id.tvJadwalError)
        tvRiwayatError = view.findViewById(R.id.tvRiwayatError)
        cardWaktuAbsensi = view.findViewById(R.id.cardWaktuAbsensi)
        notificationBellContainer = view.findViewById(R.id.notificationBellContainer)
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge)
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter)
        customDateContainer = view.findViewById(R.id.customDateContainer)
        etStartDate = view.findViewById(R.id.etStartDate)
        etEndDate = view.findViewById(R.id.etEndDate)
        btnApplyCustomDate = view.findViewById(R.id.btnApplyCustomDate)
        setupFilterChips()
        notificationBellContainer.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            startActivity(Intent(requireContext(), NotificationCenterActivity::class.java))
        }
        NotificationCounterManager.counter.observe(viewLifecycleOwner) { count ->
            if (!isAdded) return@observe
            if (count > 0) { tvNotificationBadge.text = if (count > 99) "99+" else count.toString(); tvNotificationBadge.visibility = View.VISIBLE }
            else tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chipId = checkedIds[0]
            val isCustom = chipId == R.id.chipCustom
            customDateContainer.visibility = if (isCustom) View.VISIBLE else View.GONE
            if (!isCustom) {
                currentFilter = when (chipId) {
                    R.id.chipToday -> "today"
                    R.id.chipMonth -> "this_month"
                    else -> ""
                }
                customStartDate = null
                customEndDate = null
                loadAllData()
            } else {
                currentFilter = ""
            }
        }

        etStartDate.setOnClickListener { showDatePicker(true) }
        etEndDate.setOnClickListener { showDatePicker(false) }

        btnApplyCustomDate.setOnClickListener {
            val sd = etStartDate.text?.toString()
            val ed = etEndDate.text?.toString()
            if (sd.isNullOrBlank() || ed.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Pilih tanggal mulai dan selesai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentFilter = "custom"
            customStartDate = sd
            customEndDate = ed
            loadAllData()
        }
    }

    private fun showDatePicker(isStart: Boolean) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val constraints = CalendarConstraints.Builder().build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
            val dateStr = fmt.format(cal.time)
            if (isStart) etStartDate.setText(dateStr) else etEndDate.setText(dateStr)
        }
        picker.show(parentFragmentManager, "datePicker")
    }

    private fun setupAbsensiButton(view: View) {
        view.findViewById<Button>(R.id.btnAbsensi).setOnClickListener {
            if (!isAdded) return@setOnClickListener
            findNavController().navigate(R.id.navigation_scan_qr)
        }
        view.findViewById<androidx.cardview.widget.CardView>(R.id.cardTotalAlpha).setOnClickListener {
            if (!isAdded) return@setOnClickListener
            PresenceDetailDialogFragment().show(parentFragmentManager, "PresenceDetail")
        }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInputManualPresensi).setOnClickListener {
            if (!isAdded) return@setOnClickListener
            ManualPresensiSiswaDialogFragment.newInstance {
                loadAllData()
            }.show(parentFragmentManager, "ManualPresensi")
        }
    }

    private fun setupJadwalSholat() {
        jadwalAdapter = JadwalSholatAdapter()
        rvJadwalSholat.apply { layoutManager = LinearLayoutManager(requireContext()); adapter = jadwalAdapter; isNestedScrollingEnabled = false }
    }

    private fun setupRiwayatAbsensi() {
        riwayatAdapter = RiwayatAbsensiAdapter()
        rvRiwayatAbsensi.apply { layoutManager = LinearLayoutManager(requireContext()); adapter = riwayatAdapter; isNestedScrollingEnabled = false }
        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUnduhRiwayat)?.setOnClickListener { exportRiwayatToCsv() }
    }

    private fun loadAllData() {
        if (!isAdded) return
        val token = getToken()
        if (token.isEmpty()) { Toast.makeText(requireContext(), "Sesi berakhir", Toast.LENGTH_SHORT).show(); return }
        loadRiwayatFromAPI()
        viewLifecycleOwner.lifecycleScope.launch { loadJadwalSholatFromAPI(token) }
    }

    private suspend fun loadJadwalSholatFromAPI(token: String) {
        if (!isAdded) return
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
        val jenisKelaminStr = sharedPref.getString("jenis_kelamin", "L") ?: "L"
        val jenisKelamin = if (jenisKelaminStr == "P") JadwalSholatHelper.JenisKelamin.PEREMPUAN else JadwalSholatHelper.JenisKelamin.LAKI_LAKI
        val allowedByGender = JadwalSholatHelper.getJadwalSholatByGender(jenisKelamin)
        val sessPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(requireContext())
        val studentJurusan = sessPref.getString("user_jurusan", "") ?: ""

        repository.getJadwalSholatToday(token).fold(
            onSuccess = { jadwalDataList ->
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                val jadwalList = jadwalDataList
                    .filter { data -> allowedByGender.any { it.equals(data.jenis_sholat, ignoreCase = true) } }
                    .filter { data ->
                        val schedJurusan = data.jurusan; val schedJurusans = data.jurusans
                        if (studentJurusan.isEmpty()) true
                        else if (!schedJurusans.isNullOrEmpty()) schedJurusans.any { it.nama.equals(studentJurusan, ignoreCase = true) }
                        else if (!schedJurusan.isNullOrBlank()) schedJurusan.equals(studentJurusan, ignoreCase = true)
                        else true
                    }
                    .map { data -> JadwalSholat(id = data.id, namaSholat = data.jenis_sholat, jamMulai = data.jam_mulai, jamSelesai = data.jam_selesai, status = JadwalSholatHelper.getStatusSholat(data.jam_mulai, data.jam_selesai)) }
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (!isAdded) return@launch
                    if (!::jadwalAdapter.isInitialized) { jadwalAdapter = JadwalSholatAdapter(); rvJadwalSholat.adapter = jadwalAdapter }
                    if (jadwalList.isNotEmpty()) { tvJadwalError.visibility = View.GONE; rvJadwalSholat.visibility = View.VISIBLE; jadwalAdapter.submitList(jadwalList) }
                    else { rvJadwalSholat.visibility = View.GONE; tvJadwalError.text = getString(R.string.JadwalKosong); tvJadwalError.visibility = View.VISIBLE; jadwalAdapter.submitList(emptyList()) }
                    cardWaktuAbsensi.visibility = if (jadwalList.any { it.status == StatusSholat.SEDANG_BERLANGSUNG || it.status == StatusSholat.AKAN_DATANG }) View.VISIBLE else View.GONE
                }
            },
            onFailure = {
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (!isAdded) return@launch
                    rvJadwalSholat.visibility = View.GONE; tvJadwalError.visibility = View.VISIBLE; cardWaktuAbsensi.visibility = View.GONE
                }
            }
        )
    }

    private fun loadRiwayatFromAPI() {
        if (!isAdded) return
        val token = getToken()
        if (token.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getHistorySiswa(
                token = token,
                week = 0,
                filter = if (currentFilter.isNotEmpty()) currentFilter else null,
                startDate = customStartDate,
                endDate = customEndDate
            ).fold(
                onSuccess = { historyData ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    historyData.statistik?.let { stats ->
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            if (!isAdded) return@launch
                            tvTotalValue.text = stats.total_absensi.toString(); tvAlphaValue.text = stats.total_alpha.toString(); tvIzinSakitValue.text = (stats.total_izin + stats.total_sakit).toString()
                        }
                    }
                    val absensiList = historyData.absensi ?: emptyList()
                    val riwayatList = absensiList.map { data -> RiwayatAbsensi(id = data.id, tanggal = formatTanggal(data.tanggal), namaSholat = data.getPrayerName() ?: "-", status = StatusAbsensi.fromString(data.status), waktuAbsen = data.waktu_absen?.let { formatWaktu(it) }) }
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        if (!isAdded) return@launch
                        if (riwayatList.isNotEmpty()) { tvRiwayatError.visibility = View.GONE; rvRiwayatAbsensi.visibility = View.VISIBLE; riwayatAdapter.submitList(riwayatList) }
                        else { rvRiwayatAbsensi.visibility = View.GONE; tvRiwayatError.text = "Belum ada riwayat presensi"; tvRiwayatError.visibility = View.VISIBLE }
                    }
                },
                onFailure = {
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        if (!isAdded) return@launch
                        rvRiwayatAbsensi.visibility = View.GONE; tvRiwayatError.visibility = View.VISIBLE
                    }
                }
            )
        }
    }

    private fun exportRiwayatToCsv() {
        if (!isAdded) return
        val items = riwayatAdapter.currentList
        if (items.isEmpty()) { Toast.makeText(requireContext(), "Tidak ada data riwayat", Toast.LENGTH_SHORT).show(); return }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val csv = "\uFEFF" + (listOf("Tanggal,Salat,Waktu,Status") + items.map { "${it.tanggal},${it.namaSholat},${it.waktuAbsen ?: "-"},${it.status.name}" }).joinToString("\n")
            val fileName = "riwayat_absensi_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.csv"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val cv = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, fileName); put(MediaStore.Downloads.MIME_TYPE, "text/csv"); put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) }
                    requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)?.let { requireContext().contentResolver.openOutputStream(it)?.use { os -> os.write(csv.toByteArray()) } }
                } else {
                    @Suppress("DEPRECATION") val f = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName); f.writeText(csv)
                }
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), "Tersimpan di Downloads/$fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatTanggal(tanggal: String): String = try { val inp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id","ID")); val out = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("id","ID")); inp.parse(tanggal)?.let { out.format(it).uppercase() } ?: tanggal } catch (e: Exception) { tanggal }
    private fun formatWaktu(waktu: String?): String? = waktu?.let { try { it.split(":").take(2).joinToString(":") } catch (e: Exception) { it } }

    private fun getToken(): String {
        if (!isAdded) return ""
        val act = requireActivity()
        return if (act is StudentMainActivity) act.getAuthTokenSiswa() else ""
    }

    private fun applyEdgeToEdge(view: View) {
        val topBar = view.findViewById<View>(R.id.topBarContent) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::rvJadwalSholat.isInitialized) rvJadwalSholat.adapter = null
        if (::rvRiwayatAbsensi.isInitialized) rvRiwayatAbsensi.adapter = null
    }
}
