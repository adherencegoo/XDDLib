package com.example.xddlib.userinput

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import com.example.xddlib.presentation.Lg
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
object XddActionDialog {
    private val sIsActionDialogShowing = AtomicBoolean(false)

    @JvmStatic
    fun show(activity: Activity,
             action: Runnable?,
             vararg messages: Any) {
        val kInnerMethodTag = Lg.getPrioritizedMessage(
                "showActionDialog",
                "timestamp:" + System.currentTimeMillis())

        try {
            //if sIsActionDialogShowing is false originally: return true and update it to true
            if (sIsActionDialogShowing.compareAndSet(false, true)) {
                //parse objects
                val kParsedObjects = Lg.VarargParser(Lg.VarargParser.Settings.FinalMsg).parse(*messages)
                val kOuterMethodTagSource = kParsedObjects.mMethodTagSource
                kParsedObjects.mMethodTagSource = null
                val kDialogMessage = kParsedObjects.toString()

                //log for starting
                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kOuterMethodTagSource, kInnerMethodTag, kParsedObjects)

                //build dialog
                val dialogBuilder = AlertDialog.Builder(activity)
                dialogBuilder.setTitle(Lg.PRIMITIVE_LOG_TAG)
                        .setMessage(kDialogMessage)
                        .setNegativeButton(android.R.string.no, null)
                        .setCancelable(true)
                action?.let {
                    dialogBuilder.setPositiveButton(android.R.string.yes) { _, _ -> it.run() }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    dialogBuilder.setOnDismissListener {
                        //log for ending
                        Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kInnerMethodTag,
                                kOuterMethodTagSource,
                                "sIsActionDialogShowing=false due to dialog dismissed")
                        sIsActionDialogShowing.set(false)
                    }
                }

                //show dialog
                activity.runOnUiThread { dialogBuilder.show() }
            } else {//sIsActionDialogShowing is true
                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kInnerMethodTag, "Confirm dialog is showing, skip this request")
            }
        } catch (e: Exception) {
            //log for ending
            Lg.e(kInnerMethodTag, "sIsActionDialogShowing=false due to exception", e)
            sIsActionDialogShowing.set(false)
        }
    }
}