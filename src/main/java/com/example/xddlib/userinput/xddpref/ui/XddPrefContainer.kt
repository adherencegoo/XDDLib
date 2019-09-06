package com.example.xddlib.userinput.xddpref.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi

import com.example.xddlib.R
import com.example.xddlib.XddInternalUtils
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData
import com.example.xddlib.userinput.xddpref.data.XddPrefBinaryData
import com.example.xddlib.userinput.xddpref.data.XddPrefPrimitiveRangeData

/**
 * Created by adher on 2017/7/20.
 */

class XddPrefContainer(context: Context, attributeSet: AttributeSet?) : LinearLayout(context, attributeSet) {

    companion object {

        fun inflate(context: Context, parent: ViewGroup?): XddPrefContainer {
            return XddInternalUtils.inflate(XddPrefContainer::class.java, R.layout.xdd_pref_dialog_container_constructor, context, parent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun init(prefs: Map<String, XddPrefAbstractData<*>>): XddPrefContainer {
        for ((_, aPref) in prefs) {
            when (aPref) {
                is XddPrefBinaryData -> XddPrefToggleElement.inflate(this).init(aPref)
                is XddPrefPrimitiveRangeData<*> -> XddPrefSeekBarElement.inflate(this).init(aPref)
                else -> XddPrefRadioElement.inflate(this).init(aPref)
            }
        }
        return this
    }

    fun saveToNative() {
        for (childIndex in 0 until childCount) {
            (getChildAt(childIndex) as XddPrefAbstractElement).saveToNative()
        }
    }
}
