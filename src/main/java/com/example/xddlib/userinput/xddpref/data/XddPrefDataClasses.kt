@file:Suppress("unused")

package com.example.xddlib.userinput.xddpref.data

import android.content.Context

class XddPrefIntData constructor(context: Context,
                                 key: String,
                                 vararg values: Int) : XddPrefEnumData<Int>(Int::class, context, key, values.toSet())

class XddPrefStringData constructor(context: Context,
                                    key: String,
                                    vararg values: String) : XddPrefEnumData<String>(String::class, context, key, values.toSet())

class XddPrefFloatData constructor(context: Context,
                                   key: String,
                                   vararg values: Float) : XddPrefEnumData<Float>(Float::class, context, key, values.toSet())

class XddPrefLongData constructor(context: Context,
                                  key: String,
                                  vararg values: Long) : XddPrefEnumData<Long>(Long::class, context, key, values.toSet())