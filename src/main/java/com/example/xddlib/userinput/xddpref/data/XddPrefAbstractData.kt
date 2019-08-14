package com.example.xddlib.userinput.xddpref.data

import android.content.Context
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
        return convertToTemplateType(valueAsObject) == get()
    }

    private fun convertToTemplateType(value: Any): T {
        var valueMutable = value
        /* Ex:
        * valueMutable(before):    String instance ("0") pointed by Object pointer
        * valueMutable(after):     Integer instance (0) pointed by Object pointer
        * return:           Integer instance (0) pointed by Integer pointer
        */

        val kClass = mDefaultValue::class
        //convert input Object to Boolean/Integer/Float/Long/String via method: valueOf
        if (kClass != valueMutable::class) {
            val stringValue: String = valueMutable.toString()
            valueMutable = when (kClass) {
                Boolean::class -> stringValue.toBoolean()
                Float::class -> stringValue.toFloat()
                Int::class -> stringValue.toInt()
                Long::class -> stringValue.toLong()
                String::class -> stringValue
                else -> throw IllegalArgumentException("Wrong type: $kClass, expected: ${NativePreferenceHelper.sValidKClasses}")
            }
        }

        return kClass.java.cast(valueMutable)!!
    }

    internal fun setUsingAny(valueAsObject: Any) = set(convertToTemplateType(valueAsObject))
}
