package com.example.xddlib.xddpref.data

import android.content.Context
import android.content.SharedPreferences

import junit.framework.Assert
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

    private lateinit var sSingleton : SharedPreferences

    fun init(context: Context) {
        sSingleton = context.getSharedPreferences(NativePreferenceHelper::class.java.simpleName, Context.MODE_PRIVATE)
    }

    fun checkClassValid(kClass: KClass<*>) {
        if (!sValidKClasses.contains(kClass)) {
            throw IllegalArgumentException("Wrong type: $kClass, expected: $sValidKClasses")
        }
    }

    operator fun <T : Any> get(key: String, defaultValue: T): T {
        Assert.assertNotNull(sSingleton)

        val kClass = defaultValue::class
        return when (kClass) {
            Boolean::class -> kClass.java.cast(sSingleton.getBoolean(key, defaultValue as Boolean))
            Float::class -> kClass.java.cast(sSingleton.getFloat(key, defaultValue as Float))
            Int::class -> kClass.java.cast(sSingleton.getInt(key, defaultValue as Int))
            Long::class -> kClass.java.cast(sSingleton.getLong(key, defaultValue as Long))
            String::class -> kClass.java.cast(sSingleton.getString(key, defaultValue as String))
            else -> throw IllegalArgumentException("Wrong type: $kClass, expected: $sValidKClasses")
        }
    }

    operator fun <T : Any> set(key: String, defaultValue: T) {
        Assert.assertNotNull(sSingleton)
        val jClass = defaultValue::class.java
        val editor = sSingleton.edit()
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

    fun clearAll() {
        sSingleton.edit().clear().apply()
    }
}