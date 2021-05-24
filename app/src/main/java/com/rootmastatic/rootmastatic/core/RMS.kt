package com.rootmastatic.rootmastatic.core

import android.app.Activity
import android.app.Application
import android.app.Fragment
import android.text.TextUtils
import com.rootmastatic.rootmastatic.REPORT_PERIOD
import com.rootmastatic.rootmastatic.SWITCH
import com.rootmastatic.rootmastatic.bean.RMSEnum
import com.rootmastatic.rootmastatic.throwExcp
import com.rootmastatic.rootmastatic.util.*

/*
 * Created by Administrator on 2021/5/21.
 */
open class RMS {

    // 单例
    object RMSManager {
        internal val rmsCore = RMSCore()
    }

    companion object {
        internal fun getRMSCore(): RMSCore {
            return RMSManager.rmsCore
        }

        /**
         * 配置
         * @param cts 域
         * @param src 数据源(MTXX)
         * @param key 工程秘钥
         * @param uid 项目ID
         * @param token 工程令牌
         */
        @JvmStatic
        fun attach(cts: Application, src: String, key: String, uid: String, token: String) {
            getRMSCore().attach(cts, src, key, uid, token)
        }

        /**
         * 初始化(在onCreated调用)
         * @param ac 域
         */
        @JvmStatic
        fun init(ac: Activity) {
            getRMSCore().init(ac)
        }

        /**
         * 初始化(在onCreated调用)
         * @param fr 域
         */
        @JvmStatic
        fun init(fr: Fragment) {
            getRMSCore().init(fr)
        }

        /**
         * 点击
         * @param key 索引
         */
        @JvmStatic
        fun click(key: String) {
            if (TextUtils.isEmpty(key)) return
            getRMSCore().click(key)
        }

        /**
         * 计时
         * @param rmsEnum 启动/停止
         */
        @JvmStatic
        fun page(rmsEnum: RMSEnum) {
            getRMSCore().page(rmsEnum)
        }

        /**
         * 上报
         */
        @JvmStatic
        fun report() {
            getRMSCore().report()
        }

        /**
         * 设置主域
         * @param url String
         */
        @JvmStatic
        fun setUrl(url: String) {
            if (TextUtils.isEmpty(url)) throwExcp("主域名不能为空")
            URL = url
        }

        /**
         * 设置副域
         * @param url String
         */
        @JvmStatic
        fun setSubUrl(url: String) {
            if (TextUtils.isEmpty(url)) throwExcp("副域名不能为空")
            SUB_URL = url
        }

        /**
         * 设置上报间隔(毫秒: 范围[1sec - 3hr])
         * @param period Int
         */
        @JvmStatic
        fun setPeriod(period: Int) {
            if (period <= 0) throwExcp("时长必须为1秒-3小时以内")
            if (!(period in 1000..3 * 60 * 60 * 1000)) throwExcp("时长必须为1秒-3小时以内")
            REPORT_PERIOD = period
        }

        /**
         * 允许发起上报请求
         * (调用re_pause后, 调用该方法重新出发上报)
         */
        @JvmStatic
        fun set_run() {
            DO_REPORT = true
        }

        /**
         * 暂停发起上报请求
         * (当业务不需要上报时, 调用该方法, 但需要再次上报时, 需要调用re_run()重新触发)
         */
        @JvmStatic
        fun set_pause() {
            DO_REPORT = false
        }

        /**
         * 开启/关闭日志
         * @param switch Boolean T:开启
         */
        @JvmStatic
        fun set_log(switch: Boolean) {
            SWITCH = switch
        }

    }


}
