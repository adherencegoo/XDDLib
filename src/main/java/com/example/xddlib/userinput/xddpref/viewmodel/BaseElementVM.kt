package com.example.xddlib.userinput.xddpref.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.xddlib.XddInternalUtils
import com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal abstract class BaseElementVM<T : Any, Pref : XddPrefAbstractData<T>>(
        application: Application,
        internal val xddPrefData: Pref) : AndroidViewModel(application) {

    companion object {
        @ColorInt
        var sColorPrimary = -1
    }

    init {
        if (sColorPrimary == -1) {
            sColorPrimary = XddInternalUtils.getColorAttr(application, android.R.attr.colorAccent)
        }
    }

    private val _isUiDifferentFromSharedPref = MutableLiveData<Boolean>()

    val isUiDifferentFromSharedPref: LiveData<Boolean>
        get() = _isUiDifferentFromSharedPref

    protected abstract val uiValue: Any

    abstract fun resetToSharedPrefValue()

    fun onUiValueChanged(uiValue: Any) {
        _isUiDifferentFromSharedPref.value = !xddPrefData.sharedValueIsEqualTo(uiValue)
    }

    fun saveToNative() {
        xddPrefData.setUsingAny(uiValue)
    }
}