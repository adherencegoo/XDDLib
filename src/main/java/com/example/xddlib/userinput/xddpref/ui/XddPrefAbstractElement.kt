package com.example.xddlib.userinput.xddpref.ui

import android.content.Context
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.widget.Button
import android.widget.HorizontalScrollView
import com.example.xddlib.R
import com.example.xddlib.XddInternalUtils
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData

/**
 * Created by adher on 2017/7/20.
 */

internal abstract class XddPrefAbstractElement(context: Context, attributeSet: AttributeSet?) : HorizontalScrollView(context, attributeSet) {

    lateinit var mPrefData: XddPrefAbstractData<*>
    private lateinit var mResetButton: Button

    internal abstract val uiValue: Any

    init {
        if (sColorPrimary == -1) {
            sColorPrimary = XddInternalUtils.getColorAttr(context, android.R.attr.colorAccent)
        }
    }

    internal open fun init(prefData: XddPrefAbstractData<*>) {
        mPrefData = prefData

        mResetButton = findViewById(R.id.key)
        mResetButton.text = prefData.key
        mResetButton.setOnClickListener { resetToSharedValue() }
    }

    /**@return true if UiValue == SharedValue
     */
    fun onUiValueChanged(uiValue: Any): Boolean {
        val equalToShared = mPrefData.sharedValueIsEqualTo(uiValue)
        mResetButton.isEnabled = !equalToShared
        return equalToShared
    }

    internal abstract fun resetToSharedValue()

    fun saveToNative() {
        mPrefData.saveToNativePreference(uiValue)
    }

    companion object {
        @ColorInt
        var sColorPrimary = -1
    }
}
