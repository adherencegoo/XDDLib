package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import com.example.xddlib.presentation.Lg

import java.util.*
import kotlin.reflect.KClass

/**
 * Created by adher on 2017/7/20.
 */

internal object NativePreferenceHelper {
    internal val sValidKClasses = Collections.unmodifiableList(ArrayList<KClass<*>>(Arrays.asList(
            Boolean::class,
            Float::class,
            Int::class,
            Long::class,
            String::class)))

    private fun getSharedPreference(context: Context)
            = context.getSharedPreferences(NativePreferenceHelper::class.java.simpleName, Context.MODE_PRIVATE)

    fun checkClassValid(kClass: KClass<*>) {
        if (!sValidKClasses.contains(kClass)) {
            throw IllegalArgumentException("Wrong type: $kClass, expected: $sValidKClasses")
        }
    }

    operator fun <T : Any> get(context: Context?, key: String, defaultValue: T): T {
        if (context == null) {
            Lg.wPrintStackTrace("null context, return defaultValue:$defaultValue", "key:$key")
            return defaultValue
        }

        val preferences = getSharedPreference(context)

        return when (val kClass = defaultValue::class) {
            Boolean::class -> kClass.java.cast(preferences.getBoolean(key, defaultValue as Boolean))!!
            Float::class -> kClass.java.cast(preferences.getFloat(key, defaultValue as Float))!!
            Int::class -> kClass.java.cast(preferences.getInt(key, defaultValue as Int))!!
            Long::class -> kClass.java.cast(preferences.getLong(key, defaultValue as Long))!!
            String::class -> kClass.java.cast(preferences.getString(key, defaultValue as String))!!
            else -> throw IllegalArgumentException("Wrong type: $kClass, expected: $sValidKClasses")
        }
    }

    operator fun <T : Any> set(context: Context?, key: String, defaultValue: T) {
        if (context == null) {
            Lg.wPrintStackTrace("null Context, do nothing")
            return
        }

        val jClass = defaultValue::class.java
        val editor = getSharedPreference(context).edit()
        when (jClass.kotlin) {
            Boolean::class -> editor.putBoolean(key, defaultValue as Boolean)
            Float::class -> editor.putFloat(key, defaultValue as Float)
            Int::class -> editor.putInt(key, defaultValue as Int)
            Long::class -> editor.putLong(key, defaultValue as Long)
            String::class -> editor.putString(key, defaultValue as String)
            else -> throw IllegalArgumentException("Wrong type: $jClass, expected: $sValidKClasses")
        }
        editor.apply()
    }

    fun clearAll(context: Context) {
        getSharedPreference(context).edit().clear().apply()
    }
}