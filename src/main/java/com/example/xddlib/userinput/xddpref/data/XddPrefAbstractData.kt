package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import com.example.xddlib.GenericType
import com.example.xddlib.presentation.Lg
import java.lang.ref.WeakReference
import java.util.*
import kotlin.reflect.KClass

/**
 * Created by adher on 2017/7/28.
 */

abstract class XddPrefAbstractData<T : Any>
internal constructor(private val klass: KClass<T>,
                     context: Context,
                     val key: String,
                     private val mDefaultValue: T) {
    init {
        NativePreferenceHelper.checkClassValid(klass)
        XddPrefUtils.addPref(this)
    }

    private val refContext = WeakReference(context)

    override fun toString(): String {
        return String.format(Locale.getDefault(), "key:%s, %s, SharedValue:%s",
                key,
                klass,
                NativePreferenceHelper[klass, refContext.get(), key, mDefaultValue])
    }

    @JvmOverloads
    fun get(showLog: Boolean = false): T {
        if (showLog) Lg.printStackTrace(this)
        return NativePreferenceHelper[klass, refContext.get(), key, mDefaultValue]
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun set(valueAsTemplate: T) {
        if (isValueValid(valueAsTemplate)) {
            NativePreferenceHelper[klass, refContext.get(), key] = valueAsTemplate
        }
    }

    protected abstract fun isValueValid(valueAsTemplate: T): Boolean

    internal fun sharedValueIsEqualTo(valueAsObject: Any): Boolean {
        return GenericType.toPrimitiveGenericType(klass, valueAsObject) == get()
    }

    internal fun setUsingAny(valueAsObject: Any) = set(GenericType.toPrimitiveGenericType(klass, valueAsObject))
}
