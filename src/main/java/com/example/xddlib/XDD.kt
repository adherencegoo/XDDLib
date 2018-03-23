package com.example.xddlib

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.provider.MediaStore
import android.support.annotation.ColorInt
import android.text.TextUtils

import com.example.xddlib.xddpref.data.NativePreferenceHelper

import junit.framework.Assert

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** Created by Owen_Chen on 2017/3/15.  */

@Suppress("unused")
object XDD {
    private const val DEFAULT_REPEAT_COUNT = 30

    internal lateinit var mMainHandler: Handler

    val isMainThread: Boolean
        get() = if (Build.VERSION.SDK_INT >= 23) {
            Looper.getMainLooper().isCurrentThread
        } else {
            Looper.getMainLooper() == Looper.myLooper()
        }

    private val sIsActionDialogShowing = AtomicBoolean(false)

    @JvmStatic
    fun init(context: Context) {
        mMainHandler = Handler(context.mainLooper)
        NativePreferenceHelper.init(context)
    }

    @JvmStatic
    fun drawCross(bitmap: Bitmap, @ColorInt color: Int, msg: String?): Bitmap {

        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        //draw a cross
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.strokeWidth = 3f
        paint.color = color
        canvas.drawLine(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat(), paint)
        canvas.drawLine(0f, imageHeight.toFloat(), imageWidth.toFloat(), 0f, paint)
        //draw text
        if (msg != null && !msg.isEmpty()) {
            paint.textSize = 22f
            canvas.drawText(msg, (imageWidth / 2).toFloat(), (imageHeight / 4).toFloat(), paint)
        }
        return bitmap
    }

    @JvmStatic
    fun saveBitmap(context: Context, bitmap: Bitmap?, fileName: String,
                   vararg objects: Any?) {
        val tag = object : Any() {

        }.javaClass.enclosingMethod.name

        //produce full path for file
        var fileFullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        if (!fileFullPath.endsWith("/") && !fileName.startsWith("/")) {
            fileFullPath += "/"
        }
        fileFullPath += fileName
        if (!fileName.endsWith(".jpg")) {
            fileFullPath += ".jpg"
        }

        if (bitmap != null) {
            //create folders if needed
            val folderName = fileFullPath.substring(0, fileFullPath.lastIndexOf('/'))
            val folder = File(folderName)
            if (!folder.isDirectory) {//folder not exist
                Assert.assertTrue(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END
                        + "Error in creating folder:[" + folderName + "]", folder.mkdirs())
            }

            var os: OutputStream? = null
            try {
                os = FileOutputStream(fileFullPath)
            } catch (e: FileNotFoundException) {
                Lg.e("FileNotFoundException: filePath:$fileFullPath", e)
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)

            val stackTraceElement = Lg.findInvokerOfDeepestInnerElementWithOffset(0)
            //Note: Must scan file instead of folder
            MediaScannerConnection.scanFile(context, arrayOf(fileFullPath), null) { path, uri ->
                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, stackTraceElement, tag, "onScanCompleted",
                        "Bitmap saved", "path:$path", "Uri:$uri", objects)
            }
        } else {
            Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, tag, "bitmap==null", fileFullPath, objects)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun getSeparator(message: String?, separator: Char, count: Int = DEFAULT_REPEAT_COUNT): String {
        val stringBuilder = StringBuilder(count * 2 + 4 + (message?.length ?: 0))
        val halfSeparator = stringRepeat(count, separator.toString())
        stringBuilder.append(halfSeparator)
        if (message != null && !message.isEmpty()) {
            stringBuilder.append(' ')
            stringBuilder.append(message)
            stringBuilder.append(' ')
        }
        stringBuilder.append(halfSeparator)
        return stringBuilder.toString()
    }

    @JvmStatic
    fun stringRepeat(str: String): String {
        return stringRepeat(DEFAULT_REPEAT_COUNT, str)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    fun stringRepeat(count: Int, str: String): String {
        return TextUtils.join("", Collections.nCopies(count, str))
    }

    @JvmStatic
    fun dbUpdate(context: Context,
                 uri: Uri,
                 values: ContentValues,
                 id: Long) {
        val whereClause = if (id > 0) MediaStore.Video.Media._ID + " = ?" else null
        val whereContent = if (id > 0) arrayOf(java.lang.Long.toString(id)) else null
        val rowCount = context.contentResolver.update(uri, values, whereClause, whereContent)
        Lg.d("updated row count:$rowCount")
    }

    @JvmStatic
    fun argbIntToHexString(@ColorInt color: Int): String {
        return String.format("#%08x", color)
    }

    @JvmStatic
    fun showActionDialog(activity: Activity, //can't be ApplicationContext
                         action: Runnable,
                         vararg objects: Any) {
        val kInnerMethodTag = Lg.getPrioritizedMessage(
                object : Any() {

                }.javaClass.enclosingMethod.name,
                "timestamp:" + System.currentTimeMillis())

        try {
            //if sIsActionDialogShowing is false originally: return true and update it to true
            if (sIsActionDialogShowing.compareAndSet(false, true)) {
                //parse objects
                val kParsedObjects = Lg.VarargParser(Lg.VarargParser.Settings.FinalMsg).parse(*objects)
                val kOuterMethodTagSource = kParsedObjects.mMethodTagSource
                kParsedObjects.mNeedMethodTag = false
                val kDialogMessage = kParsedObjects.toString()

                //log for starting
                Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kOuterMethodTagSource, kInnerMethodTag, kParsedObjects)

                //build dialog
                val dialogBuilder = AlertDialog.Builder(activity)
                dialogBuilder.setTitle(Lg.PRIMITIVE_LOG_TAG)
                        .setMessage(kDialogMessage)
                        .setPositiveButton(android.R.string.yes) { dialog, which -> action.run() }
                        .setNegativeButton(android.R.string.no, null)
                        .setCancelable(true)
                        .setOnDismissListener {
                            //log for ending
                            Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, kInnerMethodTag,
                                    kOuterMethodTagSource,
                                    "sIsActionDialogShowing=false due to dialog dismissed")
                            sIsActionDialogShowing.set(false)
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

    class StackTraceElementDescription
    /** All illegal -> fail! (At least one element must be legal)
     * @param fileName will auto add postfix ".java" if missing
     * @param partialClassName true if StackTraceElement.getClassName CONTAINS it
     */
    @JvmOverloads constructor(fileName: String?,
                              internal val kPartialClassName: String?,
                              internal val kMethodName: String?,
                              internal val kLineNumber: Int = -1) {
        internal val kFileName: String?

        constructor(fileName: String?,
                    klass: Class<*>,
                    methodName: String?) : this(fileName, klass.name, methodName)

        constructor(fileName: String?,
                    klass: Class<*>,
                    methodName: String?,
                    lineNumber: Int) : this(fileName, klass.name, methodName, lineNumber)

        init {
            //All illegal -> fail! (At least one element must be legal)
            Assert.assertFalse(fileName == null && kPartialClassName == null && kMethodName == null && kLineNumber <= 0)

            kFileName = if (fileName != null && !fileName.endsWith(".java")) "$fileName.java" else fileName
        }

        internal fun isMatched(element: StackTraceElement): Boolean {
            var matched = true
            if (/*found && */kFileName != null) matched = element.fileName == kFileName
            if (matched && kPartialClassName != null) matched = element.className.contains(kPartialClassName)
            if (matched && kMethodName != null) matched = element.methodName == kMethodName
            if (matched && kLineNumber > 0) matched = element.lineNumber == kLineNumber
            return matched
        }
    }

    /**@return true if all descriptions are matched in current stack trace
     */
    @JvmStatic
    fun isInvokedFrom(vararg descriptions: StackTraceElementDescription): Boolean {
        val kUnmatchedDescriptions = ArrayList(Arrays.asList(*descriptions))

        val kElements = Thread.currentThread().stackTrace
        for (kElement in kElements) {
            for (i in kUnmatchedDescriptions.indices) {
                if (kUnmatchedDescriptions[i].isMatched(kElement)) {
                    kUnmatchedDescriptions.removeAt(i)
                    break//it's impossible to match 2 descriptions using the same element, so break to next element
                }
            }

            if (kUnmatchedDescriptions.size == 0) return true
        }
        return false
    }

    @JvmStatic
    fun vibrate(context: Context, ms: Long) {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(ms)
    }
}
