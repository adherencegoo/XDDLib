package com.example.xddlib.userinput.xddpref.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

import com.example.xddlib.R
import com.example.xddlib.XddInternalUtils
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData
import com.example.xddlib.userinput.xddpref.data.XddPrefBinaryData

/**
 * Created by adher on 2017/7/20.
 */

class XddPrefContainer(context: Context, attributeSet: AttributeSet?) : LinearLayout(context, attributeSet) {

    companion object {

        fun inflate(context: Context, parent: ViewGroup?): XddPrefContainer {
            return XddInternalUtils.inflate(XddPrefContainer::class.java, R.layout.xdd_pref_dialog_container_constructor, context, parent)
        }
    }

    fun init(prefs: Map<String, XddPrefAbstractData<*>>): XddPrefContainer {
        for ((_, aPref) in prefs) {
            val layoutRes = if (aPref is XddPrefBinaryData) {
                R.layout.xdd_pref_dialog_toggle_element
            } else {
                R.layout.xdd_pref_dialog_radio_element
            }

            val dataBinding: ViewDataBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(context), layoutRes, this, true)
            (dataBinding.root as XddPrefAbstractElement).init(aPref, dataBinding)
        }
        return this
    }

    fun saveToNative() {
        for (childIndex in 0 until childCount) {
            (getChildAt(childIndex) as XddPrefAbstractElement).saveToNative()
        }
    }
}
