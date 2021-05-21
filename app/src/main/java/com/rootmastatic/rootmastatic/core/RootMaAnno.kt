package com.rootmastatic.rootmastatic.core

/*
 * Created by Administrator on 2021/5/18 0018.
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RootMaAnno(
    val module: String = "default_module",// 模块
    val page: String = "default_page"// 页面
)
