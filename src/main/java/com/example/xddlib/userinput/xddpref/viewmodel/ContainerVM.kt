package com.example.xddlib.userinput.xddpref.viewmodel

import android.app.Application
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProviders
import com.example.xddlib.userinput.xddpref.data.XddPrefUtils

internal class ContainerVM(application: Application) : AndroidViewModel(application) {

    private val uiElementVMs = mutableListOf<BaseElementVM<*, *>>()

    internal fun saveToNative() = uiElementVMs.forEach(BaseElementVM<*, *>::saveToNative)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun inflateUiElements(fragment: DialogFragment, parent: ViewGroup) {
        fragment.dialog.view

        val viewModels = XddPrefUtils.xddPrefs.values.map {
            val layoutRes = VMFactory.prefDataToLayoutRes(it)
            val dataBinding: ViewDataBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context), layoutRes, parent, true)
            val vmFactory = VMFactory(getApplication(), it, dataBinding)
            val vmClass = VMFactory.prefDataToVMClass(it)

            ViewModelProviders.of(fragment, vmFactory).get(vmClass)
        }

        uiElementVMs.clear()
        uiElementVMs.addAll(viewModels)
    }
}