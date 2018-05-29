package com.example.xddlib

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
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
import com.example.xddlib.presentation.Lg
import com.example.xddlib.userinput.xddpref.data.NativePreferenceHelper
import junit.framework.Assert
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

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

    @JvmStatic
    fun checkDatabaseEquality(fullPath1: String, fullPath2: String, vararg tables: String): Boolean {
        Assert.assertTrue(tables.isNotEmpty())

        val tagDb = Lg.getPrioritizedMessage(Lg.DEFAULT_INTERNAL_LG_TYPE)
        Lg.log(tagDb, "Comparing databases:",
                Lg.LF + Lg.TAB, fullPath1,
                Lg.LF + Lg.TAB, fullPath2)

        var db1: SQLiteDatabase? = null
        var db2: SQLiteDatabase? = null
        try {
            db1 = SQLiteDatabase.openDatabase(fullPath1, null, SQLiteDatabase.OPEN_READONLY)
            db2 = SQLiteDatabase.openDatabase(fullPath2, null, SQLiteDatabase.OPEN_READONLY)

            tables.forEach { table ->
                val tagTable = Lg.getPrioritizedMessage(tagDb, "table:$table")
                Lg.log(tagTable, "Comparing ...")

                var cursor1: Cursor? = null
                var cursor2: Cursor? = null
                try {
                    cursor1 = db1.query(table, null, null, null, null, null, null)
                    cursor2 = db2.query(table, null, null, null, null, null, null)

                    if (cursor1 == null || cursor2 == null) {
                        Lg.e(tagTable, "cursor1:$cursor1", "cursor2:$cursor2")
                        return false
                    } else if (cursor1.columnCount != cursor2.columnCount) {// Column count
                        Lg.e(tagTable, "Column counts mismatch!",
                                Lg.LF + Lg.TAB, cursor1.columnNames,
                                Lg.LF + Lg.TAB, cursor2.columnNames)
                        return false
                    } else if (!(cursor1.columnNames contentDeepEquals cursor2.columnNames)) {
                        Lg.e(tagTable, "Column names mismatches",
                                Lg.LF + Lg.TAB, cursor1.columnNames,
                                Lg.LF + Lg.TAB, cursor2.columnNames)
                        return false
                    } else if (cursor1.count != cursor2.count) {// Row count
                        Lg.e(tagTable, "Row counts mismatch!",
                                "${cursor1.count} v.s. ${cursor2.count}")
                        return false
                    } else {// Row content
                        val values1 = ContentValues()
                        val values2 = ContentValues()
                        while (cursor1.moveToNext() and cursor2.moveToNext()) {// For each row
                            val tagRow = Lg.getPrioritizedMessage(tagTable,
                                    "Row contents not equal!",
                                    "rowIndex:${cursor1.position}")

                            DatabaseUtils.cursorRowToContentValues(cursor1, values1)
                            DatabaseUtils.cursorRowToContentValues(cursor2, values2)

                            /* Extract blob(byte array) form Cursor and compare them,
                            Remove blob from ContentValues*/
                            for (colIdx in 0 until cursor1.columnCount) {
                                if (cursor1.getType(colIdx) == Cursor.FIELD_TYPE_BLOB
                                        && cursor2.getType(colIdx) == Cursor.FIELD_TYPE_BLOB) {
                                    val colName = cursor1.getColumnName(colIdx)
                                    if (cursor1.getBlob(colIdx) contentEquals cursor2.getBlob(colIdx)) {
                                        values1.remove(colName)
                                        values2.remove(colName)
                                    } else {
                                        Lg.e(tagRow, "byteArray! colName:$colName")
                                        return false
                                    }
                                }
                            }

                            if (values1 != values2) {// Doesn't handle byte array equality
                                Lg.e(tagRow,
                                        Lg.LF + Lg.TAB, values1,
                                        Lg.LF + Lg.TAB, values2)
                                return false
                            }
                        }
                    }
                } catch (e: SQLiteException) {
                    Lg.e(tagDb, e)
                    return false
                } finally {
                    cursor1?.close()
                    cursor2?.close()
                }
            }
        } catch (e: SQLiteException) {
            Lg.e(tagDb, e)
            return false
        } finally {
            db1?.close()
            db2?.close()

            Lg.log(tagDb, "All done")
        }

        return true
    }
}
