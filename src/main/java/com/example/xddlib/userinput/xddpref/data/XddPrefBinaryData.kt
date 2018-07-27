package com.example.xddlib.userinput.xddpref.data

/**
 * Created by adher on 2017/7/20.
 */

class XddPrefBinaryData @JvmOverloads constructor(key: String,
                                                  trueDescription: String = key,
                                                  falseDescription: String = "Not $trueDescription",
                                                  defaultBoolean: Boolean = false)
    : XddPrefEnumData<Boolean>(key, defaultBoolean, !defaultBoolean) {
    private val kFalseDescription: String = "(False) $falseDescription"
    private val kTrueDescription: String = "(True) $trueDescription"

    fun getDescription(wanted: Boolean): String {
        return if (wanted) kTrueDescription else kFalseDescription
    }
}
