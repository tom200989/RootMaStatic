package com.rootmastatic.rootmastatic.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.rootmastatic.rootmastatic.R
import com.rootmastatic.rootmastatic.bean.RMSEnum
import com.rootmastatic.rootmastatic.core.RMS
import com.rootmastatic.rootmastatic.core.RootMaAnno
import kotlinx.android.synthetic.main.activity_main2.*

@RootMaAnno(module = "MODULE_2", page = "PAGE_2")
class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        RMS.init(this)
        back.setOnClickListener {
            RMS.click("BTN_2")
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        RMS.page(RMSEnum.START)
    }

    override fun onPause() {
        super.onPause()
        RMS.page(RMSEnum.END)
    }
}
