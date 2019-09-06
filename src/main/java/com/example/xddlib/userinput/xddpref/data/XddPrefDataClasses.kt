@file:Suppress("unused")

package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.xddlib.presentation.Lg

class XddPrefIntData constructor(context: Context,
                                 key: String,
                                 vararg values: Int) : XddPrefPrimitiveEnumData<Int>(Int::class, context, key, values.toSet())

class XddPrefStringData constructor(context: Context,
                                    key: String,
                                    vararg values: String) : XddPrefPrimitiveEnumData<String>(String::class, context, key, values.toSet())

class XddPrefFloatData constructor(context: Context,
                                   key: String,
                                   vararg values: Float) : XddPrefPrimitiveEnumData<Float>(Float::class, context, key, values.toSet())

class XddPrefLongData constructor(context: Context,
                                  key: String,
                                  vararg values: Long) : XddPrefPrimitiveEnumData<Long>(Long::class, context, key, values.toSet())

@RequiresApi(Build.VERSION_CODES.N)
class XddPrefLgTypeData(context: Context,
                        key: String,
                        defaultType: Lg.Type)
    : XddPrefGenericEnumData<Lg.Type, String>(String::class, context, key,
        Lg.Type::toString,
        Lg.Type.Companion::fromString,
        Lg.Type.values().filter { it !== Lg.Type.UNKNOWN }.sortedWith(Comparator { type1, type2 ->
            when {
                type1 === defaultType -> -1
                type2 === defaultType -> 1
                else -> type1.ordinal.compareTo(type2.ordinal)
            }
        }).toSet())

class XddPrefIntRangeData(context: Context,
                          key: String,
                          min: Int,
                          max: Int,
                          step: Int) : XddPrefPrimitiveRangeData<Int>(Int::class, context, key, min, max, step)