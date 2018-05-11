package com.example.xddlib

import android.content.Context
import android.os.Handler
import android.widget.Toast
import java.lang.ref.WeakReference

@Suppress("unused")
object XddToast {
    private var sRefCachedToast: WeakReference<Toast>? = null

    @JvmStatic
    fun show(context: Context, vararg objects: Any?) {
        val parser = Lg.getFinalNoTagMessage(Lg.DEFAULT_INTERNAL_LG_TYPE,
                "(" + Throwable().stackTrace[0].methodName + ")",
                objects)
        Lg.log(parser)

        Handler(context.mainLooper).post {
            sRefCachedToast?.get()?.cancel()
            sRefCachedToast = WeakReference(
                    Toast.makeText(context, Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END + parser, Toast.LENGTH_LONG).apply { show() })
        }
    }
}