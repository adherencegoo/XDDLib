package com.example.xddlib

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
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

/** Created by Owen_Chen on 2017/3/15.  */

@Suppress("unused")
object XDD {
    val isMainThread: Boolean
        get() = if (Build.VERSION.SDK_INT >= 23) {
            Looper.getMainLooper().isCurrentThread
        } else {
            Looper.getMainLooper() == Looper.myLooper()
        }

    @JvmStatic
    fun init(context: Context) {
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
        if (!TextUtils.isEmpty(msg)) {
            paint.textSize = 22f
            canvas.drawText(msg, (imageWidth / 2).toFloat(), (imageHeight / 4).toFloat(), paint)
        }
        return bitmap
    }

    @JvmStatic
    fun saveBitmap(context: Context, bitmap: Bitmap?, fileName: String,
                   vararg objects: Any?) {
        val tag = "saveBitmap"

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
    fun getSeparator(message: String, separator: Char, count: Int = 30): String {
        val stringBuilder = StringBuilder(count * 2 + 4 + (message.length))
        val halfSeparator = stringRepeat(count, separator.toString())
        stringBuilder.append(halfSeparator)
        if (!message.isEmpty()) {
            stringBuilder.append(' ')
            stringBuilder.append(message)
            stringBuilder.append(' ')
        }
        stringBuilder.append(halfSeparator)
        return stringBuilder.toString()
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

    class StackTraceElementDescription
    /** All illegal -> fail! (At least one element must be legal)
     * @param kFileName without extension
     * @param kPartialClassName true if StackTraceElement.getClassName CONTAINS it
     */
    @JvmOverloads constructor(private val kFileName: String?,
                              private val kPartialClassName: String?,
                              private val kMethodName: String?,
                              private val kLineNumber: Int = -1) {

        constructor(fileName: String?,
                    klass: Class<*>,
                    methodName: String?) : this(fileName, klass.name, methodName)

        init {
            //All illegal -> fail! (At least one element must be legal)
            Assert.assertFalse(kFileName == null && kPartialClassName == null && kMethodName == null && kLineNumber <= 0)
        }

        internal fun isMatched(element: StackTraceElement): Boolean {
            var matched = true
            if (/*found && */kFileName != null) matched = element.fileName.startsWith(kFileName)
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
            for (element in kUnmatchedDescriptions.withIndex()) {
                if (element.value.isMatched(kElement)) {
                    kUnmatchedDescriptions.removeAt(element.index)
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
