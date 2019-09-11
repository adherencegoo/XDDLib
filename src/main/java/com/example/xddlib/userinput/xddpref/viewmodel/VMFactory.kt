package com.example.xddlib.userinput.xddpref.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.xddlib.R
import com.example.xddlib.databinding.XddPrefDialogRadioElementBinding
import com.example.xddlib.databinding.XddPrefDialogToggleElementBinding
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData
import com.example.xddlib.userinput.xddpref.data.XddPrefBinaryData
import com.example.xddlib.userinput.xddpref.data.XddPrefPrimitiveEnumData

internal class VMFactory(private val application: Application,
                         private val xddPrefData: XddPrefAbstractData<*>,
                         private val dataBinding: ViewDataBinding) : ViewModelProvider.NewInstanceFactory() {
    companion object {
        @LayoutRes
        internal fun prefDataToLayoutRes(pref: XddPrefAbstractData<*>) = if (pref is XddPrefBinaryData) {
            R.layout.xdd_pref_dialog_toggle_element
        } else {
            R.layout.xdd_pref_dialog_radio_element
        }

        internal fun prefDataToVMClass(pref: XddPrefAbstractData<*>) = if (pref is XddPrefBinaryData) {
            ToggleElementVM::class.java
        } else {
            RadioElementVM::class.java
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass == RadioElementVM::class.java
                && xddPrefData is XddPrefPrimitiveEnumData
                && dataBinding is XddPrefDialogRadioElementBinding) {
            RadioElementVM(application, xddPrefData, dataBinding) as T
        } else if (modelClass == ToggleElementVM::class.java
                && xddPrefData is XddPrefBinaryData
                && dataBinding is XddPrefDialogToggleElementBinding) {
            ToggleElementVM(application, xddPrefData, dataBinding) as T
        } else {
            super.create(modelClass)
        }
    }
}