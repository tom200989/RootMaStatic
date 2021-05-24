package com.rootmastatic.rootmastatic.util

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.Log
import com.alibaba.fastjson.JSONObject
import com.rootmastatic.rootmastatic.TAG
import com.rootmastatic.rootmastatic.putErr
import com.rootmastatic.rootmastatic.putInfo
import com.rootmastatic.rootmastatic.putWarn
import org.xutils.common.Callback
import org.xutils.http.RequestParams
import org.xutils.x
import java.io.File

/*
 * Created by Administrator on 2021/5/24.
 */
var RELESE_MAIN_URL = "https://www.tcl-move.com/v1.2/" // 正式
var DEBUG_MAIN_URL = "https://api.tcl-move.com/v1.2/" // 测试
var SUB_URL = "/ops/v1.2/stat/report" // 副域

var context_h: Context? = null
val LASTEST_UPDATE: String = "LASTEST_UPDATE" // 临时存储Key

/**
 * 启动网络上报
 * @param spFileName String 上报后续保存到sp的文件名
 * @param reqJson String 上报数据
 */
@Synchronized
fun toHttp(spFileName: String, reqJson: String) {

    // TOAT: 2021/5/24 测试代码 - 假设上报成功  
    // if (TextUtils.isEmpty(reqJson)) return
    // Log.i(TAG, "toHttp: 上报1次, file: $spFileName, content: $reqJson")
    // val lastest_update = spFileName + "#" + System.currentTimeMillis() / 1000
    // ShareUtils.getIt(context_h!!).putString(LASTEST_UPDATE, lastest_update)
    // return

    if (TextUtils.isEmpty(reqJson)) return
    val param = RequestParams(DEBUG_MAIN_URL + SUB_URL)
    param.addHeader("Content-Type", "application/json")
    param.addHeader("Accept-Language", "en")
    param.addHeader("Authorization", "key=YNlV0JJMaFcj37ivPKBlfR23QZ7AcCibgOADlPBvxMcR-tNG5hs")
    param.addHeader("User-Agent", Build.MANUFACTURER + "-" + Build.MODEL)
    param.isAsJsonContent = true
    param.bodyContent = reqJson
    x.http().post(param, object : Callback.CommonCallback<String> {

        override fun onSuccess(result: String?) {
            putInfo(doServerResult(spFileName, result!!))
        }

        override fun onError(ex: Throwable?, isOnCallback: Boolean) {
            putErr("上报失败, app错误-> ${ex?.message}")
        }

        override fun onCancelled(cex: Callback.CancelledException?) {
            putWarn("上报取消-> ${cex?.message}")
        }

        override fun onFinished() {
        }
    })
}

/**
 * 处理服务器的返回数据
 * @param result String
 * @return String
 */
fun doServerResult(spFileName: String, result: String): String {
    // 包含error_field字段
    if (result.contains("error_field")) {
        val e2 = JSONObject.parseObject(result, Errbean2::class.java)
        when (e2.error_id) {
            0 -> {
                // 存入sp - LASTEST_UPDATE#162195789966
                val lastest_update = spFileName + "#" + System.currentTimeMillis() / 1000
                ShareUtils.getIt(context_h!!).putString(LASTEST_UPDATE, lastest_update)
                return "上报成功"
            }
            else -> return "服务器错误: error code = ${e2.error_id}, error field = ${e2.error_field}, error msg = ${e2.error_msg}"
        }
    } else {// 不包含
        val e1 = JSONObject.parseObject(result, Errbean1::class.java)
        when (e1.error_id) {
            0 -> return "上报成功"
            else -> return "服务器错误: error code = ${e1.error_id}, error msg = ${e1.error_msg}"
        }
    }
}

class Errbean1() {
    var error_id: Int? = null
    var error_msg: String? = null
}

class Errbean2() {
    var error_id: Int? = null
    var error_msg: String? = null
    var error_field: String? = null
}
