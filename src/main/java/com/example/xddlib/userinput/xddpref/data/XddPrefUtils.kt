package com.example.xddlib.userinput.xddpref.data

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

import com.example.xddlib.userinput.xddpref.ui.XddPrefContainer

import java.util.LinkedHashMap

/**
 * Created by adher on 2017/7/14.
 * Usage:</br>
 *
 *  1. Create data: Child class of [com.example.xddlib.userinput.xddpref.data.XddPrefAbstractData],
 *  1. Init: [.init]
 *  1. Mount UI: [.showDialog]
 *
 */

@Suppress("unused")
object XddPrefUtils {
    private val TAG = XddPrefUtils::class.java.simpleName

    //LinkedHashMap: make sure no elements with the same key
    private val sXddPrefs = LinkedHashMap<String, XddPrefAbstractData<*>>()

    internal val xddPrefs: Map<String, XddPrefAbstractData<*>>
        get() = sXddPrefs

    fun addPref(pref: XddPrefAbstractData<*>) {
        sXddPrefs[pref.key] = pref
    }

    fun removeAllPref(context: Context) {
        sXddPrefs.clear()
        NativePreferenceHelper.clearAll(context)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun showDialog(activity: Activity/*Dialog must be created via ActivityContext*/) {
        val dialogBody = XddPrefContainer.inflate(activity, null).init(sXddPrefs)

        val dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setTitle(TAG)
                .setPositiveButton(android.R.string.yes) { _, _ -> dialogBody.saveToNative() }
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(true)
                .setOnDismissListener { dialogBody.removeAllViews() }
                .setView(dialogBody)

        //show dialog
        activity.runOnUiThread { dialogBuilder.show() }
    }


}
