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
        // 设置客制化
        RMS.setUrl("http://www.tcl-move.com/") // 设置主域名
        RMS.setSubUrl("/op/v1.3/ss/repo") // 设置副域名
        RMS.setPeriod(50 * 1000)// 设置上报时长(毫秒)
        RMS.set_log(true)// T:记录日志, F:停止记录
        RMS.set_pause()// 停止上报
        RMS.set_run()// 停止上报后再次触发上报
        // 绑定
        RMS.attach(this, src, key, uid, token)
    }
}
