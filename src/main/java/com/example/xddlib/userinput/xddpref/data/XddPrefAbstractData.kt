package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import com.example.xddlib.PrimitiveTypeConverter
import com.example.xddlib.presentation.Lg
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by adher on 2017/7/28.
 */

abstract class XddPrefAbstractData<T : Any>
internal constructor(context: Context,
                     val key: String,
                     private val mDefaultValue: T) {
    init {
        NativePreferenceHelper.checkClassValid(mDefaultValue::class)
        XddPrefUtils.addPref(this)
    }

    private val refContext = WeakReference(context)

    override fun toString(): String {
        return String.format(Locale.getDefault(), "key:%s, %s, SharedValue:%s",
                key,
                mDefaultValue::class,
                NativePreferenceHelper[refContext.get(), key, mDefaultValue])
    }

    @JvmOverloads
    fun get(showLog: Boolean = false): T {
        if (showLog) Lg.printStackTrace(this)
        return NativePreferenceHelper[refContext.get(), key, mDefaultValue]
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun set(valueAsTemplate: T) {
        if (isValueValid(valueAsTemplate)) {
            NativePreferenceHelper[refContext.get(), key] = valueAsTemplate
        }
    }

    protected abstract fun isValueValid(valueAsTemplate: T): Boolean

    internal fun sharedValueIsEqualTo(valueAsObject: Any): Boolean {
        return PrimitiveTypeConverter.toGenericType(mDefaultValue::class, valueAsObject) == get()
    }

    internal fun setUsingAny(valueAsObject: Any) = set(PrimitiveTypeConverter.toGenericType(mDefaultValue::class, valueAsObject))
}
