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
import com.tcl.token.ndk.ServerEncrypt
import org.xutils.common.Callback
import org.xutils.http.RequestParams
import org.xutils.x
import java.net.URL


/*
 * Created by Administrator on 2021/5/24.
 */
var RELESE_MAIN_URL = "https://www.tcl-move.com/" // 正式
var DEBUG_MAIN_URL = "https://api.tcl-move.com/" // 测试
var SUB_URL = "/ops/v1.2/stat/report" // 副域

var URL: String? = null
var SEC_URL: String? = null
var UID = "16029161077920650326"// UID
var KEY = "YNlV0JJMaFcj37ivPKBlfR23QZ7AcCibgOADlPBvxMcR-tNG5hs"// key
var TOKEN = "ssMAAjJdtrES8eZj5CO-giWFBfbFfaC2pYyvjTnLVE03nuwYpX4nw-kRsAYSTxGFnormEErGV8wOJgubJZhJgg_aPgo"// TOKEN

var context_h: Context? = null
val LASTEST_UPDATE: String = "LASTEST_UPDATE" // 临时存储Key
var DO_REPORT = true // 允许/不允许发起上报

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

    if (!DO_REPORT) return
    if (TextUtils.isEmpty(reqJson)) return
    val main_new: (String) -> String = { if (it.endsWith("/")) "$it/" else it }
    val sub_new: (String) -> String = { if (it.startsWith("/")) it.trimMargin("/") else it }
    val param = RequestParams(main_new(URL ?: DEBUG_MAIN_URL) + sub_new(SEC_URL ?: SUB_URL))
    param.addHeader("Content-Type", "application/json")
    param.addHeader("Accept-Language", "en")
    param.addHeader("Authorization", getAuthoried())
    param.addHeader("User-Agent", Build.MANUFACTURER + "-" + Build.MODEL)
    param.isAsJsonContent = true
    param.bodyContent = reqJson
    Log.i(TAG, "toHttp: 准备上报\n$reqJson")
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

/**
 * 获取动态秘钥
 * @return String 动态秘钥
 */
fun getAuthoried(): String {
    val encrypt = ServerEncrypt(UID)
    val key = "key=$KEY;"
    val token = "token=$TOKEN;"
    val sign = "sign=" + encrypt.getSign().toString() + ";"
    val timeStamp = "timestamp=" + encrypt.getTimestamp().toString() + ";"
    val newToken = "newtoken=" + encrypt.getNewtoken().toString() + ";"
    return key + token + sign + timeStamp + newToken
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
