package com.example.xddlib

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import androidx.annotation.ColorInt
import android.text.TextUtils
import androidx.annotation.RequiresApi
import com.example.xddlib.presentation.Lg
import org.junit.Assert
import java.util.*
import java.util.function.Predicate

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
    fun drawCross(bitmap: Bitmap, @ColorInt color: Int, msg: String): Bitmap {

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
        if (msg.isNotEmpty()) {
            paint.textSize = 22f
            canvas.drawText(msg, (imageWidth / 2).toFloat(), (imageHeight / 4).toFloat(), paint)
        }
        return bitmap
    }

    @JvmStatic
    @JvmOverloads
    fun getSeparator(message: String, separator: Char, count: Int = 30): String {
        val stringBuilder = StringBuilder(count * 2 + 4 + (message.length))
        val halfSeparator = stringRepeat(count, separator.toString())
        stringBuilder.append(halfSeparator)
        if (message.isNotEmpty()) {
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
        val whereContent = if (id > 0) arrayOf(id.toString()) else null
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
        val kUnmatchedDescriptions = ArrayList(listOf(*descriptions))

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

    /**
     * @param msOnOff durationOn, durationOff, durationOn, durationOff, ...*/
    @JvmStatic
    fun vibrate(context: Context, vararg msOnOff: Long) {
        if (msOnOff.isEmpty()) return

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                    if (msOnOff.size == 1) VibrationEffect.createOneShot(msOnOff[0], VibrationEffect.DEFAULT_AMPLITUDE)
                    else VibrationEffect.createWaveform(longArrayOf(0, *msOnOff), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(msOnOff[0])
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @JvmStatic
    fun<T> getConstantFieldNames(ownerClass: Class<*>,
                                 fieldClass: Class<T>,
                                 fieldNameFilter: Predicate<String>): Map<T, String> {
        return ownerClass.declaredFields
                .filter {
                    it.isAccessible = true
                    fieldNameFilter.test(it.name) && fieldClass.isInstance(it.get(ownerClass))
                }.map {
                    @Suppress("UNCHECKED_CAST")
                    it.get(ownerClass) as T to it.name
                }.toMap()
    }

    private val testPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    @JvmStatic
    @JvmOverloads
    fun decorateCanvas(canvas: Canvas,
                       bounds: Rect,
                       boundsColor: Int = Color.GREEN,
                       axisColor: Int = Color.RED) {
        testPaint.color = boundsColor
        canvas.drawRect(bounds, testPaint)
        canvas.drawLine(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), testPaint)
        canvas.drawLine(bounds.right.toFloat(), bounds.top.toFloat(), bounds.left.toFloat(), bounds.bottom.toFloat(), testPaint)

        testPaint.color = axisColor
        val centerX = bounds.width() / 2
        val centerY = bounds.height() / 2
        canvas.drawLine(centerX.toFloat(), centerY.toFloat(), centerX.toFloat(), (centerY + 100).toFloat(), testPaint)
        canvas.drawLine(centerX.toFloat(), centerY.toFloat(), (centerX + 100).toFloat(), centerY.toFloat(), testPaint)
    }

    @JvmStatic
    fun dumpDimension(left: Float, top: Float, width: Float, height: Float): String {
        return "${width}x${height}, center:(${left + width / 2}, ${top + height / 2}), w/h:${width / height}"
    }

    @JvmStatic
    fun dumpRect(rect: Rect?): String {
        return rect?.let {
            Lg.getFinalMessage(Lg.VarargParser.Control.KILL_METHOD_TAG, rect,
                    dumpDimension(it.left.toFloat(), it.top.toFloat(), it.width().toFloat(), it.height().toFloat())).toString()
        } ?: "null"
    }

    @JvmStatic
    fun dumpRect(rect: RectF?): String {
        return rect?.let {
            Lg.getFinalMessage(Lg.VarargParser.Control.KILL_METHOD_TAG, rect,
                    dumpDimension(it.left, it.top, it.width(), it.height())).toString()
        } ?: "null"
    }

    @JvmStatic
    fun dumpBitmap(bitmap: Bitmap?): String {
        return bitmap?.let {
            Lg.getFinalMessage(Lg.VarargParser.Control.KILL_METHOD_TAG, bitmap,
                    dumpDimension(0f, 0f, it.width.toFloat(), it.height.toFloat())).toString()
        } ?: "null"
    }

    @JvmStatic
    fun dumpDrawable(drawable: Drawable?): String {
        return drawable?.let {
            Lg.getFinalMessage(Lg.VarargParser.Control.KILL_METHOD_TAG,
                    drawable,
                    "intrinsic", dumpDimension(0f, 0f, it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat()),
                    "minimum", dumpDimension(0f, 0f, it.minimumWidth.toFloat(), it.minimumHeight.toFloat())).toString()
        } ?: "null"
    }
}
