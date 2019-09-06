package com.example.xddlib

import android.content.SharedPreferences
import org.junit.Assert
import java.io.InvalidClassException
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

@Suppress("unused")
object GenericType {
    private class PrimitiveType<T : Any> internal constructor(private val klass: KClass<T>,
                                                              internal val arrayToString: (Any) -> String,
                                                              private val fromString: (String) -> Any) {
        companion object {
            private val TYPE_MAP = mutableMapOf<KClass<*>?, PrimitiveType<*>>()

            @Suppress("UNCHECKED_CAST")
            internal operator fun <T : Any> get(klass: KClass<T>?): PrimitiveType<T> = TYPE_MAP.getOrElse(klass) {
                throw InvalidClassException(klass?.qualifiedName)
            } as PrimitiveType<T>
        }

        init {
            TYPE_MAP += klass to this
        }

        internal fun toGenericType(any: Any): T = klass.cast(
                if (any::class == klass) {
                    any
                } else {
                    fromString.invoke(any.toString())
                })
    }

    private val PRIMITIVE_BYTE = PrimitiveType(Byte::class, { (it as ByteArray).contentToString() }, { it.toByte() })
    private val PRIMITIVE_CHAR = PrimitiveType(Char::class, { (it as CharArray).contentToString() }, { it[0] })
    private val PRIMITIVE_SHORT = PrimitiveType(Short::class, { (it as ShortArray).contentToString() }, { it.toShort() })
    private val PRIMITIVE_INT = PrimitiveType(Int::class, { (it as IntArray).contentToString() }, { it.toInt() })
    private val PRIMITIVE_LONG = PrimitiveType(Long::class, { (it as LongArray).contentToString() }, { it.toLong() })
    private val PRIMITIVE_FLOAT = PrimitiveType(Float::class, { (it as FloatArray).contentToString() }, { it.toFloat() })
    private val PRIMITIVE_DOUBLE = PrimitiveType(Double::class, { (it as DoubleArray).contentToString() }, { it.toDouble() })
    private val PRIMITIVE_BOOLEAN = PrimitiveType(Boolean::class, { (it as BooleanArray).contentToString() }, { it.toBoolean() })

    private class PreferenceType<T : Any>(kClass: KClass<T>,
                                          internal val setter: (SharedPreferences.Editor, String, T) -> Unit,
                                          internal val getter: (SharedPreferences, String, T) -> T) {
        companion object {
            private val TYPE_MAP = mutableMapOf<KClass<*>?, PreferenceType<*>>()

            @Suppress("UNCHECKED_CAST")
            internal operator fun <T : Any> get(klass: KClass<out T>?): PreferenceType<T> = TYPE_MAP.getOrElse(klass) {
                throw InvalidClassException(klass?.qualifiedName)
            } as PreferenceType<T>
        }

        init {
            TYPE_MAP += kClass to this
        }
    }

    private val PREFERENCE_BOOLEAN = PreferenceType(Boolean::class,
            { editor, key, value -> editor.putBoolean(key, value) },
            { preferences, key, default -> preferences.getBoolean(key, default) })
    private val PREFERENCE_FLOAT = PreferenceType(Float::class,
            { editor, key, value -> editor.putFloat(key, value) },
            { preferences, key, default -> preferences.getFloat(key, default) })
    private val PREFERENCE_INT = PreferenceType(Int::class,
            { editor, key, value -> editor.putInt(key, value) },
            { preferences, key, default -> preferences.getInt(key, default) })
    private val PREFERENCE_LONG = PreferenceType(Long::class,
            { editor, key, value -> editor.putLong(key, value) },
            { preferences, key, default -> preferences.getLong(key, default) })
    private val PREFERENCE_STRING = PreferenceType(String::class,
            { editor, key, value -> editor.putString(key, value) },
            { preferences, key, default -> preferences.getString(key, default)!! })


    fun primitiveArrayToString(obj: Any?): String {
        if (obj == null) return "null"
        Assert.assertTrue(obj.javaClass.isArray)
        return PrimitiveType[obj.javaClass.componentType?.kotlin].arrayToString.invoke(obj)
    }

    fun <T : Any> toPrimitiveGenericType(targetClass: KClass<out T>, value: Any): T {
        return if (targetClass == String::class) {
            targetClass.cast(value.toString())
        } else {
            PrimitiveType[targetClass].toGenericType(value)
        }
    }

    fun <T : Any> setPreference(klass: KClass<T>,
                                editor: SharedPreferences.Editor,
                                key: String,
                                value: T)
            = PreferenceType[klass].setter.invoke(editor, key, value)

    fun <T : Any> getPreference(klass: KClass<T>,
                                preferences: SharedPreferences,
                                key: String,
                                defaultValue: T)
            = PreferenceType[klass].getter.invoke(preferences, key, defaultValue)
}