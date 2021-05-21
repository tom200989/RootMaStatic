package com.rootmastatic.rootmastatic.util

import android.os.Handler
import android.os.Looper
import java.util.*

/*
 * Created by Administrator on 2021/5/19.
 */
abstract class TimerRunner {

    var timerTask: TimerTask? = null;
    var timer: Timer? = null;
    var handler = Handler(Looper.getMainLooper())

    abstract fun doSometing()

    /**
     * 普通启动 + 轮训
     * @param period Long 间隔
     */
    open fun start(period: Long) {
        timer = Timer()
        object : TimerTask() {
            override fun run() {
                handler.post { doSometing() }
            }
        }.also { timerTask = it }
        timer?.schedule(timerTask, 0, period)
    }

    /**
     * 延迟启动 + 不轮训
     * @param delay Long 间隔
     */
    open fun startDelay(delay: Long) {
        timer = Timer()
        object : TimerTask() {
            override fun run() {
                handler.post { doSometing() }
            }
        }.also { timerTask = it }
        timer?.schedule(timerTask, delay)
    }

    /**
     * 延迟启动 + 轮训
     * @param delay Long 延迟
     * @param period Long 间隔
     */
    open fun start(delay: Long, period: Long) {
        timer = Timer()
        object : TimerTask() {
            override fun run() {
                handler.post { doSometing() }
            }
        }.also { timerTask = it }
        timer?.schedule(timerTask, delay, period)
    }

    /**
     * 停止
     */
    open fun stop() {
        timerTask?.cancel()
        timerTask = null
        timer?.cancel()
        timer?.purge()
        timer = null
    }

}
