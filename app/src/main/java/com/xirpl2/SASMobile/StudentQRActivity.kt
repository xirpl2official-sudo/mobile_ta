package com.xirpl2.SASMobile

import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class StudentQRActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_qr_v2)
        
        // Set status bar color
        window.statusBarColor = getColor(R.color.qr_primary)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
