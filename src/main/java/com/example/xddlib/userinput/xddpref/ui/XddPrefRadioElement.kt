package com.example.xddlib.userinput.xddpref.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.xddlib.R
import com.example.xddlib.XddInternalUtils
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData
import com.example.xddlib.userinput.xddpref.data.XddPrefEnumData
import junit.framework.Assert

/**
 * Created by adher on 2017/7/21.
 */

internal class XddPrefRadioElement(context: Context, attributeSet: AttributeSet?) : XddPrefAbstractElement(context, attributeSet) {
    private lateinit var mRadioGroup: RadioGroup

    override val uiValue: Any
        get() = (findViewById<View>(mRadioGroup.checkedRadioButtonId) as RadioButton).text

    override fun init(prefData: XddPrefAbstractData<*>) {
        Assert.assertTrue(prefData is XddPrefEnumData)
        super.init(prefData)

        val enumData = prefData as XddPrefEnumData<*>

        mRadioGroup = findViewById(R.id.radioGroup)
        mRadioGroup.removeAllViews()
        mRadioGroup.setOnCheckedChangeListener { group, checkedId -> onUiValueChanged((group.findViewById<View>(checkedId) as RadioButton).text) }

        //create radio buttons based on values in pref
        val prefValueAsString = mPrefData[false].toString()
        for (oneEnum in enumData.values) {
            val radioButton = RadioButton(this.context)
            mRadioGroup.addView(radioButton) //the RadioButtons are added to the radioGroup instead of the layout
            radioButton.text = oneEnum.toString()

            if (prefValueAsString == radioButton.text) {
                radioButton.isChecked = true
                radioButton.setTextColor(XddPrefAbstractElement.Companion.sColorPrimary)
            }
        }
    }

    override fun resetToSharedValue() {
        for (i in 0 until mRadioGroup.childCount) {
            val button = mRadioGroup.getChildAt(i) as RadioButton
            if (mPrefData.sharedValueIsEqualTo(button.text)) {
                button.isChecked = true
                break
            }
        }
    }

    companion object {

        fun inflate(parent: XddPrefContainer): XddPrefRadioElement {
            return XddInternalUtils.inflate(XddPrefRadioElement::class.java, R.layout.xdd_pref_dialog_radio_element_constructor, parent)
        }
    }
}
