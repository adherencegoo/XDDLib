@file:Suppress("unused")

package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import org.junit.Assert
import kotlin.reflect.KClass

/**
 * Created by adher on 2017/7/20.
 */

open class XddPrefEnumData<T : Any>(klass: KClass<T>,
                                    context: Context,
                                    key: String,
                                    internal val values: Set<T>) : XddPrefAbstractData<T>(klass, context, key, values.first()) {
    // This still doesn't work in Java code
//    constructor(klass: Class<T>,
//                context: Context,
//                key: String,
//                vararg args: T) : this(klass.kotlin, context, key, setOf(*args))

    init {
        if (klass == Boolean::class) {
            Assert.assertTrue(values.size == 2)
        } else {
            Assert.assertTrue(values.isNotEmpty())
        }
    }

    override fun isValueValid(valueAsTemplate: T): Boolean = values.contains(valueAsTemplate)
}