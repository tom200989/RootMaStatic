package com.rootmastatic.rootmastatic.core

import android.app.Activity
import android.app.Application
import android.app.Fragment
import com.rootmastatic.rootmastatic.bean.RMSEnum

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
         */
        fun attach(cts: Application, src: String) {
            getRMSCore().attach(cts, src)
        }

        /**
         * 初始化(在onCreated调用)
         * @param ac 域
         */
        fun init(ac: Activity) {
            getRMSCore().init(ac)
        }

        /**
         * 初始化(在onCreated调用)
         * @param fr 域
         */
        fun init(fr: Fragment) {
            getRMSCore().init(fr)
        }

        /**
         * 点击
         * @param key 索引
         */
        fun click(key: String) {
            getRMSCore().click(key)
        }

        /**
         * 计时
         * @param rmsEnum 启动/停止
         */
        fun page(rmsEnum: RMSEnum) {
            getRMSCore().page(rmsEnum)
        }

        /**
         * 上报
         */
        fun report() {
            getRMSCore().report()
        }
    }
}
