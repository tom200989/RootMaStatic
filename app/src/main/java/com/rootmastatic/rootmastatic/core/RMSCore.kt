package com.rootmastatic.rootmastatic.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Fragment
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.content.pm.PermissionInfoCompat
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.util.TypeUtils
import com.rootmastatic.rootmastatic.*
import com.rootmastatic.rootmastatic.bean.RMSBean
import com.rootmastatic.rootmastatic.bean.RMSEnum
import com.rootmastatic.rootmastatic.util.*
import org.xutils.common.Callback
import org.xutils.http.RequestParams
import org.xutils.x
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.LinkedHashMap

/*
 * Created by Administrator on 2021/5/18 0018.
 */
internal class RMSCore {

    private var app: Application? = null // 域
    private var dirPath: String? = null // 统计目录(路径)
    private var isAttach = false // 是否application配置数据源
    private var isPermit = false // 是否允许后续统计(数据源未正确, 文件未创建, 不允许统计)
    private var isPermit2 = false // 是否允许后续统计(未在对应页面init, 不允许统计)
    private var source: String? = null // 默认数据源
    private var module: String? = null // 模块
    private var page: String? = null // 页面
    private var activity: Activity? = null // 临时变量
    private var fragment: Fragment? = null // 临时变量
    private var logTimer: TimerRunner? = null// 日志定时器
    private var dikTimer: TimerRunner? = null // 细粒度定时器(用于解决APP强制杀死的情况)
    private var reportTimer: TimerRunner? = null // 上报定时器

    companion object {
        @JvmStatic
        private var dikMap: LinkedHashMap<String, Array<Boolean>> = LinkedHashMap()
    }


    /**
     * 配置数据源(在application中使用)
     * @param cts Application 域
     * @param src String 数据源
     */
    @Synchronized
    fun attach(cts: Application, src: String) {
        app = cts
        context_h = cts
        source = src


        // 判断是否配置(项目名)数据源(含正则校验)
        isAttach = checkSource(source!!) { putInfo("attach: $it") }
        // 符合条件
        if (isAttach) {
            putVerbose("attach(): 数据源校验正确")
            // 创建日志目录
            createTodayLog()
            // 创建统计目录
            dirPath = cts.getExternalFilesDir(null)!!.absolutePath + static_dir
            val dir = File(dirPath!!)
            /* 目录存在 - 搜索今天的文件是否存在 */
            if (dir.exists() and dir.isDirectory) {
                putVerbose("attach(): 目录存在")

                /* 
                * 补偿上报机制
                * 
                * <1> 上报时, 记录<文件名, 时间(此处称utime)>到shareperence, 
                * <2> 获取sp的文件名并到对应的目录查找该文件
                * <3> 如果找到该文件就读取文件中的最后1个json, 并获取end_time (此处称etime)
                * <4> 如果etime > utime ,那么就先上报一次, 然后再执行attach的其他操作
                * <5> attach完毕后, 启动定时器, 1小时上报1次今天的文件, 并记录到shareperence中
                * 
                *  */
                fixReport(dir)

                // 获取今天的时间戳范围
                val today = getRangeToday()
                val startToday: Int = today[0]
                val endToday: Int = today[1]
                putVerbose("attach(): 今天时间范围[$startToday , $endToday]")
                // 遍历
                val files = dir.listFiles()
                /* 文件夹不为空 - 查找是否有今天的文件 */
                if (files!!.isNotEmpty()) {
                    putVerbose("attach(): 目录不为空, 准备查询今天统计文件")
                    var isExists = false
                    for (file in files) {
                        // MT42,159212345688; LINKHUB, 16845564658
                        val fs = file.name.split(split)
                        val curSource = fs[0]
                        val curTime = fs[1].replace(exf, "").toInt()
                        // 项目名匹配 - 进入时间范围判断
                        if (curSource == source) {
                            if (curTime in startToday..endToday) {// 在范围内
                                putVerbose("attach(): 查找到今天范围内的统计文件, 文件名{${file.name}}")
                                // 返回文件对象
                                staticFileo = file
                                isPermit = true
                                isExists = true
                                break
                            }
                        }
                    }

                    /* 找不到当前项目今天的统计文件 - 创建 */
                    if (!isExists) {
                        putVerbose("attach(): 未找到今天统计文件, 即将创建今天文件")
                        // 创建今天文件 - 并返回文件对象
                        createTodayFile()
                    }

                } else {/* 文件夹为空(首次统计) */
                    // 创建今天文件
                    createTodayFile()
                }

            } else {/* 目录不存在(首次统计) */
                putVerbose("attach(): 目录不存在(首次统计),即将创建统计目录")
                // 创建目录
                dir.mkdir()
                // 创建今天文件
                createTodayFile()
            }

            // 启动日志定时器
            startLogTimer()
            // 启动上报间隔定时器
            startReportTimer()

        } else {// 不符合条件 - 返回
            isPermit = false
            throwExcp("attach(): 数据源配置不正确(检查是否为空或是否匹配正则规矩)")
        }
    }

    /**
     * 初始化(Activity)
     * @param ac Activity 域
     */
    @Synchronized
    fun init(ac: Activity) {
        activity = ac
        // 初始校验
        if (!isAttach) {
            throwExcp("init: 未在application初始化成功\n请调用RootMaStatic.attach(<工程名>)并检查源名称是否合法")
            return
        }

        // 得到类头注解
        if (ac.javaClass.isAnnotationPresent(RootMaAnno::class.java)) {
            val anno = ac.javaClass.getAnnotation(RootMaAnno::class.java)
            module = anno?.module
            page = anno?.page

            putInfo("init: module: $module , page: $page")
        }

        // 标记通过
        if (!TextUtils.isEmpty(module) && !TextUtils.isEmpty(page)) isPermit2 = true
        // 判断埋点合规
        checkTF()
    }

    /**
     * 初始化(Fragment)
     * @param fr Fragment 域
     */
    @Synchronized
    fun init(fr: Fragment) {
        fragment = fr
        // 初始校验
        if (!isAttach) {
            throwExcp("init: 未在application初始化成功\n请调用RootMaStatic.attach(<工程名>)并检查源名称是否合法")
            return
        }

        // 得到类头注解
        if (fr.javaClass.isAnnotationPresent(RootMaAnno::class.java)) {
            val anno = fr.javaClass.getAnnotation(RootMaAnno::class.java)
            module = anno?.module
            page = anno?.page

            putInfo("init: module: $module , page: $page")
        }

        // 标记通过
        if (!TextUtils.isEmpty(module) && !TextUtils.isEmpty(page)) isPermit2 = true
        // 判断埋点合规
        checkTF()
    }

    /**
     * 统计点击
     * @param value String 字符KEY
     */
    @Synchronized
    fun click(key: String) {
        // 初始化过滤
        if (!isPermit or !isPermit2) {
            throwExcp("未调用初始化方法init()或者数据源不正确")
            return
        }
        // 正则过滤
        if (!Pattern.compile("^[A-Z0-9_]+\$").matcher(key).matches()) {
            throwExcp("click: 当前点击定义的字符不匹配整齐, 只允许大写字母、数字、下划线")
            return
        }
        // 记录日志
        putVerbose("CLICK: KEY($key)")
        // 录入统计 - 每一行都是一个Json
        staticList.add(merBtnInfo(key))
    }

    /**
     * 统计时长(页面)
     * @param rmsEnum enum 起始枚举
     */
    @Synchronized
    fun page(rmsEnum: RMSEnum) {
        // 初始化过滤
        if (!isPermit or !isPermit2) {
            throwExcp("未调用初始化方法init()或者数据源不正确")
            return
        }
        // 正则过滤
        if (!Pattern.compile("^[A-Z0-9_]+\$").matcher(page!!).matches()) {
            throwExcp("page: 当前点击定义的字符不匹配整齐, 只允许大写字母、数字、下划线")
            return
        }
        // 情况识别
        when (rmsEnum) {
            RMSEnum.START -> {
                // 日志
                putVerbose("PAGE START: KEY($page)")
                // 写出
                staticList.add(merPageInfo(page!!, PAGE, rmsEnum))
                // 启动细粒度定时器
                startDikTimer()
                // 修改对应的START为true
                dikMap[page!!]!![0] = true
            }
            RMSEnum.END -> {
                // 日志
                putVerbose("PAGE END: KEY($page)")
                // 写出
                staticList.add(merPageInfo(page!!, PAGE, rmsEnum))
                // 停止定时器
                stopDikTimer()
                // 修改对应的END为true
                dikMap[page!!]!![1] = true
            }
        }
    }

    /**
     * 上报
     */
    @Synchronized
    fun report() {
        // 获取今天上报内容
        val reqJson = collectInfo(staticFileo!!)
        // 异步上报
        toHttp(staticFileo!!.name, reqJson)
    }


    /* ------------------------------------------------------------ private---------------------------------------------------------- */

    /**
     * 补偿上报
     */
    private fun fixReport(dir: File) {
        // 读取到上次缓存的 <文件名,时间>
        val lastest_update: String? = ShareUtils.getIt(app!!).getString(LASTEST_UPDATE, "default#-1")
        val infos = lastest_update?.split("#")
        val last_file_name: String? = infos?.get(0)
        val last_update_time: Long? = infos?.get(1)?.toLong() // utime
        if (last_update_time == -1.toLong()) return
        // 查找对应文件
        var tempF: File? = null
        val listFiles = dir.listFiles()
        if (listFiles!!.isEmpty()) return
        for (listFile in listFiles) {
            if (listFile.name == last_file_name) {
                tempF = listFile
                break
            }
        }

        if (tempF == null) return
        if (TextUtils.isEmpty(tempF.readText())) return

        // 获取上报内容
        val reqJson = collectInfo(tempF)
        // 异步上报
        toHttp(tempF.name, reqJson)
        Log.i(TAG, "fixReport: 补偿上报\nfilename: ${tempF.name}\n内容为: \n$reqJson")

        // // 取出最后一行数据 TOAT 暂时不要删除以下代码
        // if (tempF != null) {
        //     val readLines = tempF.readLines()
        //     val last_file_json = readLines[readLines.size - 1].replace("#", "").replace("\n", "")
        //     val last_file_rms = JSONObject.parseObject(last_file_json, RMSBean::class.java)
        //     val etime = last_file_rms.end_time
        //     if (etime > last_update_time!!) {// etime > utime
        //         // 获取上报内容
        //         val reqJson = collectInfo(tempF)
        //         // 异步上报
        //         toHttp(tempF.name, reqJson)
        //         Log.i(TAG, "fixReport: 补偿上报\nendtime: $etime\nlast_update_time:$last_update_time\n内容为: \n$reqJson")
        //     }
        // }
    }

    /**
     * 上报间隔定时器
     */
    private fun startReportTimer() {
        reportTimer?.stop()
        reportTimer = object : TimerRunner() {
            override fun doSometing() {
                report()
            }
        }
        reportTimer?.start(REPORT_PERIOD.toLong(), REPORT_PERIOD.toLong())
    }

    /**
     * 启动细粒度定时器
     * (防止APP被杀而没有被记录到的情况)
     */
    private fun startDikTimer() {
        dikTimer?.stop()
        dikTimer = object : TimerRunner() {
            override fun doSometing() {// 30秒一到
                putVerbose("当前计数时间30 sec")
                // 写出end_time
                staticList.add(merPageInfo(page!!, PAGE, RMSEnum.END))
            }
        }
        dikTimer?.start(5 * 1000, 5 * 1000)
    }

    /**
     * 停止细粒度定时器
     * (END时停止, 如果代码不合规(START和END没有成对出现),
     * 则通过下一个页面来throw异常停止)
     */
    private fun stopDikTimer() {
        dikTimer?.stop()
        dikTimer = null
    }

    /**
     * 判断埋点是否合规
     * 如果存在[T,F][F,T]这两种情况, 则抛出.
     * 原因是开发者未正确配置起始或终止埋点
     */
    private fun checkTF() {
        // 添加到map里
        if (!dikMap.containsKey(page)) {
            dikMap[page!!] = Array(2) { false }
        }
        // 判断其他存在的page是否正确配置[T,T]
        if (dikMap.isNotEmpty()) {
            for (entry in dikMap) {
                // 当前page - 跳过
                if (entry.key == page) continue
                // 判断其他page是否正确配置
                val arr = entry.value
                val condi_1: Boolean = arr[0] && !arr[1] // [T,F]
                val condi_2: Boolean = !arr[0] && arr[1] // [F,T]
                if (condi_1) throwExcp("页面 {${entry.key}} 未正确配置END埋点")
                if (condi_2) throwExcp("页面 {${entry.key}} 未正确配置START埋点")
            }
        }
    }

    /**
     * 拼接(页面)
     * @param key String 字段
     * @param type Int 页面、应用
     * @param rmsEnum enum 起始枚举
     * @return String json
     */
    @Synchronized
    private fun merPageInfo(key: String, type: Int, rmsEnum: RMSEnum): String {

        // 存入
        val rmsBean = RMSBean()
        rmsBean.source = source!!
        rmsBean.module = module!!
        rmsBean.type = type
        rmsBean.Value = key
        when (rmsEnum) {
            RMSEnum.START -> rmsBean.start_time = System.currentTimeMillis() / 1000
            RMSEnum.END -> rmsBean.end_time = System.currentTimeMillis() / 1000
        }
        // 禁用Fastjson自动大小写
        TypeUtils.compatibleWithFieldName = true
        TypeUtils.compatibleWithJavaBean = true
        return JSONObject.toJSONString(rmsBean)

    }

    /**
     * 拼接(控件)
     * @param key String 字段
     * @param type Int 控件
     * @return String json
     */
    @Synchronized
    private fun merBtnInfo(key: String): String {
        val rmsBean = RMSBean()
        rmsBean.source = source!!
        rmsBean.module = module!!
        rmsBean.type = BTN
        rmsBean.Value = key
        rmsBean.Count = 1
        rmsBean.start_time = System.currentTimeMillis() / 1000
        // 禁用Fastjson自动大小写
        TypeUtils.compatibleWithFieldName = true
        TypeUtils.compatibleWithJavaBean = true
        return JSONObject.toJSONString(rmsBean)
    }

    /**
     * 启动定时器
     */
    private fun startLogTimer() {
        // 先停止
        stopTimer()
        // 后创建
        if (logTimer == null) {
            logTimer = object : TimerRunner() {
                override fun doSometing() {
                    writeLog(app as Context)// 写入日志
                    writeStatic(app as Context)// 写入统计
                }
            }
        }
        logTimer?.start(1000)
    }

    /**
     * 停止定时器
     */
    private fun stopTimer() {
        logTimer?.stop()
        logTimer = null
    }

    /**
     * 创建今天的统计文件
     */
    private fun createTodayFile() {
        // com.xx.xx/all_in_one_sc/MT42_15922266889.txt 统计文件
        val filePath = dirPath + File.separator + source + split + System.currentTimeMillis() / 1000 + exf
        staticFileo = File(filePath)
        val isCreateSuccess = staticFileo!!.createNewFile()
        isPermit = isCreateSuccess
        if (isCreateSuccess)
            putInfo("createTodayFile(): 创建今天文件成功, 允许统计今天数据{$isPermit}")
        else
            putErr("createTodayFile(): 创建今天文件失败, 不可统计今天数据{$isPermit}")
    }

    /**
     * 创建今天日志文件
     */
    private fun createTodayLog() {
        // com.xx.xx/all_in_one_sc_log 日志目录
        val log_dir_path = app?.getExternalFilesDir(null)?.absolutePath + log_dir
        val logDir = File(log_dir_path)
        // 不存在 - 创建
        if (!logDir.exists() or !logDir.isDirectory) {
            putVerbose("createTodayLog(): 目录不存在(首次统计),即将创建日志目录")
            logDir.mkdir()
        }

        // 查找今天的日志文件 
        val today = getRangeToday()
        val startToday = today[0]
        val endToday = today[1]
        putVerbose("createTodayLog(): 今天时间范围[$startToday , $endToday]")
        val logFiles = logDir.listFiles()
        /* 目录不为空 */
        if (logFiles!!.isNotEmpty()) {
            // 遍历
            var isExists = false
            for (logFile in logFiles) {
                val ls = logFile.name.split(split)
                val curSource = ls[0]
                val curTime = ls[1].replace(exf, "").toInt()
                if (curSource == source) {
                    if (curTime in startToday..endToday) {
                        putVerbose("createTodayLog(): 查找到今天范围内的日志文件, 文件名{${logFile.name}}")
                        logFileo = logFile // 赋值
                        isExists = true
                        break
                    }
                }
            }

            /* 找不到当前项目今天的统计文件 - 创建 */
            if (!isExists) {
                putVerbose("createTodayLog(): 未找到今天统计文件, 即将创建今天日志文件")
                // 创建今天日志文件 - 并返回文件对象
                toCreatLog(log_dir_path)
            }

        } else {/* 目录为空(首次使用) */
            toCreatLog(log_dir_path)
        }
    }

    /**
     * 去创建日志文件
     * @param log_dir_path String 日志目录
     */
    private fun toCreatLog(log_dir_path: String) {
        // com.xx.xx/all_in_one_sc_log/MT42_15922266889.txt 
        val logPath = log_dir_path + File.separator + source + split + System.currentTimeMillis() / 1000 + exf
        val logFile = File(logPath)
        val isCreateSuccess = logFile.createNewFile()
        if (isCreateSuccess) {
            logFileo = logFile // 赋值
            putInfo("toCreatLog(): 创建今天日志文件成功 {${logFile.name}}")
        } else {
            putErr("toCreatLog(): 创建今天日志文件失败")
        }
    }


    /**
     * 获取今天的时间戳范围
     */
    @SuppressLint("SimpleDateFormat")
    private fun getRangeToday(): Array<Int> {
        // 得到当前时间
        val sif = SimpleDateFormat("HH:mm:ss")
        val arr = sif.format(Date()).split(":")
        val hr = arr[0].toInt()
        val min = arr[1].toInt()
        val sec = arr[2].toInt()
        // 倒退计算
        val del = hr * 3600 + min * 60 + sec
        // 综合范围
        val start: Int = (System.currentTimeMillis() / 1000 - del + 1).toInt() // 从第1秒开始算 0:00:01
        val end: Int = start + 24 * 3600 - 2 // 倒数第1秒截止 23:59:59
        return arrayOf(start, end)
    }

    /**
     * 条件打印式
     * @param source String 入参
     * @param printIt 逻辑
     */
    private fun checkSource(source: String, printIt: (String) -> Unit): Boolean =
        if (!TextUtils.isEmpty(source)) {// 数据源非空 - 正确

            // 检查正则 - 大小写字母、数字、破折线(-)
            regex(source) {
                printIt(it)
            }

        } else {// 数据源为空 - 错误
            printIt("数据源为空未正确配置")
            false
        }

    /**
     * 正则表达式检查(仅限定source)
     * @param source String 源
     * @param reg String 正则规矩
     * @param printIt  逻辑
     * @return Boolean 是否匹配
     */
    private fun regex(source: String, reg: String = "^[A-Z0-9-]+$", printIt: (String) -> Unit): Boolean {
        // 正则表达式检查
        val pattern = Pattern.compile(reg)
        val matcher = pattern.matcher(source)
        if (matcher.matches()) {
            printIt("正则已正确配置")
            return true
        } else {
            printIt("正则格式不正确, 只可包含大写字母、数字、破折线(-)")
            return false
        }
    }
}
