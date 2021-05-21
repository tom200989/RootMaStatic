package com.rootmastatic.rootmastatic.app

import android.app.Application
import android.content.Context
import com.rootmastatic.rootmastatic.core.RMS
import com.rootmastatic.rootmastatic.core.RMSCore


/*
 * Created by Administrator on 2021/5/19.
 */
open class RSApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 配置数据源
        RMS.attach(this, "MT42")
    }
}
