package com.example.xddlib.userinput.xddpref.viewmodel

import android.app.Application
import android.os.Build
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import com.example.xddlib.databinding.XddPrefDialogRadioElementBinding
import com.example.xddlib.userinput.xddpref.data.XddPrefPrimitiveEnumData

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadioElementVM<T : Any>(application: Application,
                                       xddPrefData: XddPrefPrimitiveEnumData<T>,
                                       private val dataBinding: XddPrefDialogRadioElementBinding)
    : BaseElementVM<T, XddPrefPrimitiveEnumData<T>>(application, xddPrefData) {
    init {
        dataBinding.viewModel = this

        val defaultPref = xddPrefData.get()
        //create radio buttons based on values in pref
        val buttons = xddPrefData.values.map { data ->
            RadioButton(application).apply {
                text = data.toString()

                if (defaultPref == data) {
                    setTextColor(sColorPrimary)
                }
            }
        }

        with(dataBinding.radioGroup) {
            removeAllViews()
            buttons.forEach {
                addView(it)
            }
        }

        resetToSharedPrefValue()
//        onUiValueChanged(uiValue)//xdd needless?
    }

    override val uiValue: Any
        get() = (dataBinding.root.findViewById(dataBinding.radioGroup.checkedRadioButtonId) as RadioButton).text

    override fun resetToSharedPrefValue() {
        val radioGroup = dataBinding.radioGroup
        for (i in 0 until radioGroup.childCount) {
            val button = radioGroup.getChildAt(i) as RadioButton
            if (xddPrefData.sharedValueIsEqualTo(button.text)) {
                button.isChecked = true
                break
            }
        }
    }

    fun onCheckedChanged(radioGroup: RadioGroup, checkedId: Int) {
        onUiValueChanged(radioGroup.findViewById<RadioButton>(checkedId).text)
    }
}