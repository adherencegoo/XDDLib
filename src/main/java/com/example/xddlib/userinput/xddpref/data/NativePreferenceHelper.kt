package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import com.example.xddlib.GenericType
import com.example.xddlib.presentation.Lg
import kotlin.reflect.KClass

/**
 * Created by adher on 2017/7/20.
 */

internal object NativePreferenceHelper {
    private val sValidKClasses = listOf(Boolean::class, Float::class, Int::class, Long::class, String::class)

    private fun getSharedPreference(context: Context)
            = context.getSharedPreferences(NativePreferenceHelper::class.java.simpleName, Context.MODE_PRIVATE)

    fun checkClassValid(kClass: KClass<*>) {
        if (!sValidKClasses.contains(kClass)) {
            throw IllegalArgumentException("Wrong type: $kClass, expected: $sValidKClasses")
        }
    }

    operator fun <T : Any> get(klass: KClass<T>, context: Context?, key: String, defaultValue: T): T {
        if (context == null) {
            Lg.wPrintStackTrace("null context, return defaultValue:$defaultValue", "key:$key")
            return defaultValue
        }

        return GenericType.getPreference(
                klass,
                getSharedPreference(context),
                key,
                defaultValue)
    }

    operator fun <T : Any> set(klass: KClass<T>, context: Context?, key: String, defaultValue: T) {
        if (context == null) {
            Lg.wPrintStackTrace("null Context, do nothing")
            return
        }

        val editor = getSharedPreference(context).edit()
        GenericType.setPreference(
                klass,
                editor,
                key,
                defaultValue)
        editor.apply()
    }

    fun clearAll(context: Context) {
        getSharedPreference(context).edit().clear().apply()
    }
}