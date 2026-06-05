package com.xirpl2.SASMobile.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.xirpl2.SASMobile.FAQActivity
import com.xirpl2.SASMobile.PengaturanActivity
import com.xirpl2.SASMobile.PengaturanAkunActivity
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.StudentMainActivity

class ProfilFragment : Fragment(R.layout.fragment_profil) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyEdgeToEdge(view)
        loadProfile(view)
        setupMenu(view)
    }

    private fun loadProfile(view: View) {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
        val nama = sharedPref.getString("nama_siswa", "Nama Siswa") ?: "Nama Siswa"
        val nis = sharedPref.getString("nis", "0000000000") ?: "0000000000"
        val kelas = sharedPref.getString("user_kelas", "") ?: ""

        view.findViewById<TextView>(R.id.tvProfileName).text = nama
        view.findViewById<TextView>(R.id.tvProfileNIS).text = "NIS: $nis"
        view.findViewById<TextView>(R.id.tvProfileKelas).text = if (kelas.isNotBlank()) kelas else ""
        view.findViewById<TextView>(R.id.tvInitial).text = nama.first().uppercase()
    }

    private fun setupMenu(view: View) {
        view.findViewById<View>(R.id.menuPengaturanAkun).setOnClickListener {
            startActivity(Intent(requireContext(), PengaturanAkunActivity::class.java))
        }
        view.findViewById<View>(R.id.menuPusatBantuan).setOnClickListener {
            startActivity(Intent(requireContext(), FAQActivity::class.java))
        }
        view.findViewById<View>(R.id.menuLogout).setOnClickListener {
            val act = requireActivity()
            if (act is StudentMainActivity) act.handleLogout()
        }
    }

    private fun applyEdgeToEdge(view: View) {
        val topBar = view.findViewById<View>(R.id.topBarContent) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }
}
