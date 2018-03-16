package com.example.xddlib.xddpref.data

import com.example.xddlib.XDD
import java.util.*

/**
 * Created by adher on 2017/7/28.
 */

abstract class XddPrefAbstractData<out T : Any>
internal constructor(val key: String,
                     private val mDefaultValue: T) {
    init {
        NativePreferenceHelper.checkClassValid(mDefaultValue::class)
        XddPrefUtils.addPref(this)
    }

    override fun toString(): String {
        return String.format(Locale.getDefault(), "key:%s, %s, SharedValue:%s",
                key, mDefaultValue::class, NativePreferenceHelper[key, mDefaultValue])
    }

    @JvmOverloads
    operator fun get(showLog: Boolean = true): T {
        if (showLog) XDD.Lg.printStackTrace(this)
        return NativePreferenceHelper[key, mDefaultValue]
    }

    fun sharedValueIsEqualTo(valueAsObject: Any): Boolean {
        return convertToTemplateType(valueAsObject) == get(false)
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

        return kClass.java.cast(valueMutable)
    }

    fun saveToNativePreference(valueAsObject: Any) {
        NativePreferenceHelper[key] = convertToTemplateType(valueAsObject)
    }
}
