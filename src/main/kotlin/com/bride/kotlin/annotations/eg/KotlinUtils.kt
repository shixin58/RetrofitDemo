package com.bride.kotlin.annotations.eg

import java.util.*

/**
 * 用不了android Log API。因此封装了println，便于查看时间和线程
 */
fun log(msg: Any) {
    println("${Date()} ${Thread.currentThread().name} $msg")
}