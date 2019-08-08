package com.example.xddlib.userinput.xddpref.data

import android.content.Context
import org.junit.Assert
import java.util.*

/**
 * Created by adher on 2017/7/20.
 */

open class XddPrefEnumData<T : Any>
/**"defaultValue", "value2", "otherValues": duplicated values will be ignored */
(context: Context,
 key: String,
 defaultValue: T,
 value2: T,
 vararg otherValues: T) : XddPrefAbstractData<T>(context, key, defaultValue) {
    val values: LinkedHashSet<T>//LinkedHashSet: no duplicated values, persist original order

    init {
        if (defaultValue is Boolean) {
            Assert.assertTrue(otherValues.isEmpty())
            Assert.assertTrue(defaultValue != value2)
        }

        //prepare list of values
        values = LinkedHashSet(otherValues.size + 2)
        values.add(defaultValue)
        values.add(value2)
        if (otherValues.isNotEmpty()) values.addAll(listOf(*otherValues))
    }
}