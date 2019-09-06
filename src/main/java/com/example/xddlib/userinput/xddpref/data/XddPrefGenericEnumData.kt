package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.reflect.KClass

@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.N)
open class XddPrefGenericEnumData<G : Any, P : Any>(primitiveClass: KClass<P>,
                                                    context: Context,
                                                    key: String,
                                                    private val genericToPrimitive: (G) -> P,
                                                    private val primitiveToGeneric: (P) -> G,
                                                    values: Set<G>)
    : XddPrefPrimitiveEnumData<P>(primitiveClass, context, key, values.map { genericToPrimitive.invoke(it) }.toSet()) {

    @JvmOverloads
    fun getAsGeneric(showLog: Boolean = false): G = primitiveToGeneric.invoke(get(showLog))
}