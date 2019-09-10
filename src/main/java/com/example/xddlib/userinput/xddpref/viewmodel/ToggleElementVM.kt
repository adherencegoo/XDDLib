package com.example.xddlib.userinput.xddpref.viewmodel

import android.R
import android.app.Application
import android.content.res.ColorStateList
import android.os.Build
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import com.example.xddlib.databinding.XddPrefDialogToggleElementBinding
import com.example.xddlib.userinput.xddpref.data.XddPrefBinaryData

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class ToggleElementVM(application: Application,
                      xddPrefData: XddPrefBinaryData,
                      private val dataBinding: XddPrefDialogToggleElementBinding)
    : BaseElementVM<Boolean, XddPrefBinaryData>(application, xddPrefData) {

    init {
        dataBinding.viewModel = this

        resetToSharedPrefValue()
//        onUiValueChanged(uiValue)//xdd needless?
    }

    override val uiValue: Any
        get() = dataBinding.toggleButton.isChecked

    override fun resetToSharedPrefValue() {
        dataBinding.toggleButton.isChecked = xddPrefData.get()
    }

    fun onToggleChanged(button: ToggleButton, isChecked: Boolean) {
        onUiValueChanged(isChecked)
    }

    fun getToggleTextColor(): ColorStateList {
        val defaultColor = dataBinding.toggleButton.textColors.defaultColor
        val binary = xddPrefData.get()

        return ColorStateList(
                arrayOf(intArrayOf(R.attr.state_checked), intArrayOf(-R.attr.state_checked/*unchecked*/)),
                intArrayOf(if (binary) sColorPrimary else defaultColor, if (binary) defaultColor else sColorPrimary))
    }
}