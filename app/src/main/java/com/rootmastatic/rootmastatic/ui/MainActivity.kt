package com.rootmastatic.rootmastatic.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.rootmastatic.rootmastatic.R
import com.rootmastatic.rootmastatic.bean.RMSEnum
import com.rootmastatic.rootmastatic.collectInfo
import com.rootmastatic.rootmastatic.core.RMS
import com.rootmastatic.rootmastatic.core.RMSCore
import com.rootmastatic.rootmastatic.core.RootMaAnno
import com.rootmastatic.rootmastatic.staticFileo
import kotlinx.android.synthetic.main.activity_main.*

@RootMaAnno(module = "MODULE_1", page = "PAGE_1")
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 1.先申请权限
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
        // 2.在需要统计的Activity或者Fragment的周期入口调用init()
        RMS.init(this)

        // 记录
        clickme.setOnClickListener {
            RMS.click("BTN_1")
        }

        // 查看
        checkout.setOnClickListener {
            RMS.report()
        }

        // 跳转
        skip.setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // 开始计时
        RMS.page(RMSEnum.START)
    }

    override fun onPause() {
        super.onPause()
        // 结束计时
        RMS.page(RMSEnum.END)
    }
}
