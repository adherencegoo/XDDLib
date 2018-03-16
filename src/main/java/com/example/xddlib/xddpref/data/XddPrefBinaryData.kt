package com.example.xddlib.xddpref.data

/**
 * Created by adher on 2017/7/20.
 */

class XddPrefBinaryData @JvmOverloads constructor(key: String,
                                                  trueDescription: String = key,
                                                  falseDescription: String = "Not $trueDescription")
    : XddPrefEnumData<Boolean>(key, false, true) {
    private val kFalseDescription: String = "(False) $falseDescription"
    private val kTrueDescription: String = "(True) $trueDescription"

    fun getDescription(wanted: Boolean): String {
        return if (wanted) kTrueDescription else kFalseDescription
    }
}
