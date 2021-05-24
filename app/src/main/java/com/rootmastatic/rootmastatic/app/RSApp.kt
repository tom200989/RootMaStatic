package com.rootmastatic.rootmastatic.app

import android.app.Application
import android.content.Context
import com.rootmastatic.rootmastatic.core.RMS


/*
 * Created by Administrator on 2021/5/19.
 */
open class RSApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 配置数据源
        val src = "MT42"
        val key = "YNlV0JJMaFcj37ivPKBlfR23QZ7AcCibgOADlPBvxMcR-tNG5hs"
        val uid = "16029161077920650326"
        val token = "ssMAAjJdtrES8eZj5CO-giWFBfbFfaC2pYyvjTnLVE03nuwYpX4nw-kRsAYSTxGFnormEErGV8wOJgubJZhJgg_aPgo"
        // 绑定
        RMS.attach(this, src, key, uid, token)
    }
}
