package com.example.xddlib.analysis

import java.lang.reflect.Method

@Suppress("unused")
object LockHelper {
    private val methodThreadHoldsLock: Method by lazy {
        Thread::class.java.getDeclaredMethod("nativeHoldsLock", Object::class.java).apply { isAccessible = true }
    }

    @JvmStatic
    fun findLockHolder(lock: Any): Thread? {
        return Thread.getAllStackTraces().keys.find { methodThreadHoldsLock.invoke(it, lock) as Boolean}
    }
}