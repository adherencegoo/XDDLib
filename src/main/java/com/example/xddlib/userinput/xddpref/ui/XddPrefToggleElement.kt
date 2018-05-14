package com.example.xddlib.userinput.xddpref.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ToggleButton
import com.example.xddlib.R
import com.example.xddlib.XddInternalUtils
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData
import com.example.xddlib.userinput.xddpref.data.XddPrefBinaryData
import junit.framework.Assert

/**
 * Created by adher on 2017/7/21.
 */

internal class XddPrefToggleElement(context: Context, attributeSet: AttributeSet?) : XddPrefAbstractElement(context, attributeSet) {
    private lateinit var mToggleButton: ToggleButton

    override val uiValue: Any
        get() = mToggleButton.isChecked

    /**@param prefData must be [com.example.xddlib.xddpref.data.XddPrefBinaryData]
     */
    override fun init(prefData: XddPrefAbstractData<*>) {
        Assert.assertEquals(XddPrefBinaryData::class.java, prefData.javaClass)
        super.init(prefData)

        val binaryData = prefData as XddPrefBinaryData
        mToggleButton = findViewById(R.id.toggleButton)
        mToggleButton.textOn = binaryData.getDescription(true)
        mToggleButton.textOff = binaryData.getDescription(false)
        mToggleButton.setOnCheckedChangeListener { buttonView, isChecked -> onUiValueChanged(isChecked) }

        val binary = binaryData[false]
        mToggleButton.setTextColor(ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked/*unchecked*/)),
                intArrayOf(if (binary) XddPrefAbstractElement.Companion.sColorPrimary else mToggleButton.textColors.defaultColor, if (binary) mToggleButton.textColors.defaultColor else XddPrefAbstractElement.Companion.sColorPrimary)))

        resetToSharedValue()
        //manually call listener, because when ToggleButton is just created, it's false for default; setChecked(false) won't invoke listener
        onUiValueChanged(uiValue)
    }

    override fun resetToSharedValue() {
        mToggleButton.isChecked = (mPrefData as XddPrefBinaryData)[false]
    }

    companion object {

        fun inflate(parent: XddPrefContainer): XddPrefToggleElement {
            return XddInternalUtils.inflate(XddPrefToggleElement::class.java, R.layout.xdd_pref_dialog_toggle_element_constructor, parent)
        }
    }

}
