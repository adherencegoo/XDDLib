package com.example.xddlib

import org.junit.Assert
import java.io.InvalidClassException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

@Suppress("unused")
object PrimitiveTypeConverter {
    private class Type<T : Any> internal constructor(private val klass: KClass<T>,
                                                     internal val arrayToString: (Any) -> String,
                                                     private val fromString: (String) -> Any) {
        init {
            CLASS_MAP += klass to this
        }

        internal fun toGenericType(any: Any): T = klass.cast(
                if (any::class == klass) {
                    any
                } else {
                    fromString.invoke(any.toString())
                })

        companion object {
            private val CLASS_MAP = mutableMapOf<KClass<*>?, Type<*>>()

            @Suppress("UNCHECKED_CAST")
            internal operator fun <T : Any> get(klass: KClass<T>?): Type<T> = CLASS_MAP.getOrElse(klass) {
                throw InvalidClassException(klass?.qualifiedName)
            } as Type<T>
        }
    }

    private val BYTE = Type(Byte::class, { Arrays.toString(it as ByteArray) }, { it.toByte() })
    private val CHAR = Type(Char::class, { Arrays.toString(it as CharArray) }, { it[0] })
    private val SHORT = Type(Short::class, { Arrays.toString(it as ShortArray) }, { it.toShort() })
    private val INT = Type(Int::class, { Arrays.toString(it as IntArray) }, { it.toInt() })
    private val LONG = Type(Long::class, { Arrays.toString(it as LongArray) }, { it.toLong() })
    private val FLOAT = Type(Float::class, { Arrays.toString(it as FloatArray) }, { it.toFloat() })
    private val DOUBLE = Type(Double::class, { Arrays.toString(it as DoubleArray) }, { it.toDouble() })
    private val BOOLEAN = Type(Boolean::class, { Arrays.toString(it as BooleanArray) }, { it.toBoolean() })


    fun arrayToString(obj: Any?): String {
        if (obj == null) return "null"
        Assert.assertTrue(obj.javaClass.isArray)
        return Type[obj.javaClass.componentType?.kotlin].arrayToString.invoke(obj)
    }

    fun <T : Any> toGenericType(targetClass: KClass<T>, value: Any): T {
        return if (targetClass == String::class) {
            targetClass.cast(value.toString())
        } else {
            Type[targetClass].toGenericType(value)
        }
    }

}