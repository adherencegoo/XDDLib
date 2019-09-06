package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import kotlin.reflect.KClass

open class XddPrefPrimitiveRangeData<T>(klass: KClass<T>,
                                        context: Context,
                                        key: String,
                                        internal val min: T,
                                        internal val max: T,
                                        internal val step: T)
    : XddPrefAbstractData<T>(klass, context, key, min)
        where T : Number, T : Comparable<T> {


    override fun isValueValid(valueAsTemplate: T): Boolean {
        return valueAsTemplate in min..max
    }
}