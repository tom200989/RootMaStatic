package com.rootmastatic.rootmastatic

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSONObject
import com.rootmastatic.rootmastatic.bean.RMSBean
import com.rootmastatic.rootmastatic.bean.RMSReqParam
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


var switch = true // 日志开关
var TAG = "ROOT_MA_STATIC" // 日志标记
var log_dir = "/all_in_one_sc_log" // 日志目录
var logFileo: File? = null // 日志文件对象
var logList: ArrayList<String> = ArrayList()// 日志缓冲器

var static_dir = "/all_in_one_sc" // 统计目录
var staticFileo: File? = null // 统计文件对象
var staticList: ArrayList<String> = ArrayList()// 统计缓冲器

var split = "_" // 切割符
var split2 = "#" // 切割符
var exf = ".txt" // 日志文件后缀

var PAGE = 0 // 页面类型
var APP = 1 // 应用类型
var BTN = 2 // 控件类型

var REPORT_PERIOD = 3 * 1000 // 上报间隔 

/**
 * 打印info
 */
@Synchronized
fun putInfo(content: String) {
    logList.add(content)
    if (switch) Log.i(TAG, content)
}

/**
 * 打印error
 */
@Synchronized
fun putErr(content: String) {
    logList.add(content)
    if (switch) Log.e(TAG, content)
}

/**
 * 打印warn
 */
@Synchronized
fun putWarn(content: String) {
    logList.add(content)
    if (switch) Log.w(TAG, content)
}

/**
 * 打印Verbose
 */
@Synchronized
fun putVerbose(content: String) {
    logList.add(content)
    if (switch) Log.v(TAG, content)
}

/**
 * 抛出异常
 * @param content String
 */
fun throwExcp(content: String) {
    putErr(content)
    throw Exception(content)
}

@SuppressLint("SimpleDateFormat")
@Synchronized
fun writeLog(context: Context) {
    if (logList.size > 0) {
        // 获取格式化时间
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS").format(Date())
        // 创建buffer
        val buf = StringBuffer()
        // 拼接
        for (content in logList) {
            // 2021-05-21 18:28:28-> xxxx\n
            buf.append(format).append("-> ").append(content).append("\n")
        }
        // 权限通过 - 进行写出操作
        val isPassPermisson: Int = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (isPassPermisson == PackageManager.PERMISSION_GRANTED) {
            // 清理
            logList.clear()
            // 写出
            if (logFileo != null) logFileo!!.appendText(buf.toString())
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Synchronized
fun writeStatic(context: Context) {
    // 创建buffer
    val buf = StringBuffer()
    // 拼接
    for (content in staticList) {
        // {json}#\n
        buf.append(content).append(split2).append("\n")
    }
    // 权限通过 - 进行写出操作
    val isPassPermisson: Int = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    if (isPassPermisson == PackageManager.PERMISSION_GRANTED) {
        // 清理
        staticList.clear()
        // 写出
        if (staticFileo != null) staticFileo!!.appendText(buf.toString())
    }
}

/**
 * 收集数据(读取需要上报的文件)
 */
@Synchronized
fun collectInfo(static_file: File): String {

    // 容器
    val rmsBtns = ArrayList<RMSBean>() // 容纳控件类型
    val rmsPageApps = ArrayList<RMSBean>() // 容纳时长类型
    // 读取 (注意消除换行)
    val readText = static_file.readText()
    if (TextUtils.isEmpty(readText)) return ""
    val contents = readText.replace("\n", "")
    // 切割 # 
    val jsons = contents.split(split2)
    // 分类(拆分控件组和页面组)
    for (json in jsons) {
        if (TextUtils.isEmpty(json)) continue
        val rmsBean: RMSBean = JSONObject.parseObject(json, RMSBean::class.java)
        if (rmsBean.type == BTN) {
            rmsBtns.add(rmsBean) // 控件类
        } else {
            rmsPageApps.add(rmsBean) // 页面类
        }
    }

    // 拆分(控件类): 组合成 <key, rmsbean>
    val rmsBtnM = HashMap<String, RMSBean>()
    for (rmsl in rmsBtns) {
        // 获取到键
        val key = rmsl.Value
        // 是否包含
        if (rmsBtnM.containsKey(key)) {
            // 包含 - 取出曾经对象
            val rmsm: RMSBean = rmsBtnM[key] as RMSBean
            // 字段赋值
            rmsm.end_time = rmsl.end_time
            rmsm.Count = rmsm.Count + 1
        } else {
            // 没有包含 - 首次存入
            rmsl.id = UUID.randomUUID().toString().replace("-", "")
            rmsl.end_time = rmsl.start_time
            rmsl.Count = 1
            rmsBtnM[rmsl.Value] = rmsl
        }
    }

    // 拆分(时长类)
    val rmsPageM = HashMap<String, RMSBean>()
    // ------------- tempM: <key, List<RMSBean>>
    val tempM = HashMap<String, List<RMSBean>>()
    for (rmsp in rmsPageApps) {
        val key = rmsp.Value
        if (tempM.containsKey(key)) {
            val curl = tempM[key] as ArrayList<RMSBean>
            curl.add(rmsp)
        } else {
            val tempL = ArrayList<RMSBean>()
            tempL.add(rmsp)
            tempM[key] = tempL
        }
    }
    // ------------- entry: <key, RMSBean>
    for (entry in tempM) {
        val key = entry.key // key
        val rmsl = entry.value // List<RMSBean>
        // 算法: 得到有效时长
        rmsPageM[key] = doMagic(rmsl)
    }

    // 合并元素 - 转换json
    val rmsFins: ArrayList<RMSBean> = ArrayList()
    rmsFins.addAll(rmsBtnM.values)
    rmsFins.addAll(rmsPageM.values)
    val rmrs = list2Arr(rmsFins)
    val param = RMSReqParam()
    param.stat = rmrs
    val reqJson = JSONObject.toJSONString(param)
    // Log.i(TAG, "collectInfo: \n$reqJson")
    return reqJson
}

fun list2Arr(rmsl: ArrayList<RMSBean>): Array<RMSBean> {
    val rmr: Array<RMSBean> = Array(rmsl.size) { RMSBean() }
    for (idx in rmsl.indices) rmr[idx] = rmsl[idx]
    return rmr
}

/**
 * 算法: 计算有效时长
 * @param List<RMSBean>
 * @return RMSBean
 */
fun doMagic(rmsl: List<RMSBean>): RMSBean {

    // 简化为 [11111000111100110....] 格式
    val buf = StringBuffer()
    for (rms in rmsl) {
        if (rms.start_time > 0) buf.append("0")
        if (rms.end_time > 0) buf.append("1")
    }

    // 1111000111001....11100 最后补0是为了算法方便
    val chars = buf.append("0").toString().toCharArray()

    // 有效时间点索引数组的集合 {[3,15],[16,19].....}
    val finals: ArrayList<Array<Int>> = ArrayList()
    var ixa = -1
    var ixb = -1

    // 开始位循环
    for (i in chars.indices) {
        // 输出前提
        if (ixa > -1 && ixb > -1 && chars[i] == '0') {
            val arr = Array(2) { 0 }
            arr[0] = ixa
            arr[1] = ixb
            finals.add(arr)
            // 清零
            ixa = -1
            ixb = -1
        }

        // 索引递增
        if (chars[i] == '0') ixa = i
        if (chars[i] == '1' && ixa > -1) ixb = i
    }

    // 总时长
    var total_duration = 0
    for (final in finals) {
        val del: Int = Math.abs(rmsl[final[1]].end_time - rmsl[final[0]].start_time).toInt()
        total_duration += del
    }

    // 新对象
    val rms_new = RMSBean()
    rms_new.id = UUID.randomUUID().toString().replace("-", "")
    rms_new.source = rmsl[0].source
    rms_new.type = rmsl[0].type
    rms_new.duration = total_duration
    rms_new.start_time = rmsl[finals[0][0]].start_time
    rms_new.end_time = rmsl[finals[finals.size - 1][1]].end_time
    rms_new.Value = rmsl[0].Value
    rms_new.Count = finals.size

    // 返回
    return rms_new

}

/**
 * 是否处于栈顶
 */
fun isTopAc(cts: Context, ac_name: String): Boolean {
    val service: ActivityManager = cts.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val tasks = service.getRunningTasks(100)
    for (task in tasks) {
        if (task.topActivity!!.className.equals(ac_name, ignoreCase = true)) {
            return true
        }
    }
    return false
}

/**
 * 是否处于栈顶(Fragment)
 */
fun isTopFr(fr: Fragment): Boolean {
    return !fr.isHidden
}

