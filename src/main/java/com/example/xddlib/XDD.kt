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
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.Vibrator
import android.provider.MediaStore
import android.support.annotation.ColorInt
import android.text.TextUtils
import com.example.xddlib.analysis.XddException
import com.example.xddlib.presentation.Lg
import com.example.xddlib.userinput.xddpref.data.NativePreferenceHelper
import junit.framework.Assert
import java.io.File
import java.io.FileNotFoundException
import java.security.InvalidParameterException
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
    fun dumpCheckDatabaseQualityResult(differences: Map<String, Map<Int, Pair<ContentValues, ContentValues>>>): String {
        return differences.entries.joinToString(separator = ";\n") {
            "\ntable:${it.key} --> \n" + it.value.entries.joinToString(separator = ";\n") {
                "\trowIndex:${it.key} --> \n" +
                        "\t\tvalue1: ${it.value.first}\n" +
                        "\t\tvalue2: ${it.value.second}"
            }
        }
    }

    @JvmStatic
    fun checkDatabaseEquality(fullPath1: String,
                              fullPath2: String,
                              tables: List<String>)
            : Map<String, Map<Int, Pair<ContentValues, ContentValues>>> {
        return checkDatabaseEquality(fullPath1, fullPath2, tables.map { it to null }.toMap())
    }

    /**@param tableDetailList key: tableName, value: interesting column names (take all columns if null)
     * @return key: tableName, value: pairs of different [ContentValues]s indexed with rowIndex*/
    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    @Throws(SQLiteException::class, XddException::class)
    fun checkDatabaseEquality(fullPath1: String,
                              fullPath2: String,
                              tableDetailList: Map<String, Collection<String>?>)
            : Map<String/*tableName*/, Map<Int/*rowIndex*/, Pair<ContentValues, ContentValues>>> {
        if (tableDetailList.isEmpty()) throw XddException(InvalidParameterException("empty tables").message)

        listOf(fullPath1, fullPath2).forEach {
            if (!File(it).exists()) throw XddException(FileNotFoundException(it).message)
        }

        val tagDb = Lg.getPrioritizedMessage(Lg.DEFAULT_INTERNAL_LG_TYPE)
        Lg.log(tagDb, "Comparing databases:",
                Lg.LF + Lg.TAB, fullPath1,
                Lg.LF + Lg.TAB, fullPath2)

        val differences = mutableMapOf<String, MutableMap<Int, Pair<ContentValues, ContentValues>>>()
        if (fullPath1 == fullPath2) {
            Lg.w(tagDb, "*** same db file***")
            return differences
        }

        var db1: SQLiteDatabase? = null
        var db2: SQLiteDatabase? = null
        try {
            db1 = SQLiteDatabase.openDatabase(fullPath1, null, SQLiteDatabase.OPEN_READONLY)
            db2 = SQLiteDatabase.openDatabase(fullPath2, null, SQLiteDatabase.OPEN_READONLY)

            tableDetailList.forEach { tableDetail ->
                val tagTable = Lg.getPrioritizedMessage(tagDb, "tableDetail:$tableDetail")
                Lg.log(tagTable, "Comparing ...")

                var cursor1: Cursor? = null
                var cursor2: Cursor? = null
                try {
                    val columns = tableDetail.value?.toTypedArray() // Nullable
                    cursor1 = db1.query(tableDetail.key, columns, null, null, null, null, null)
                    cursor2 = db2.query(tableDetail.key, columns, null, null, null, null, null)

                    if (cursor1 == null || cursor2 == null) {
                        throw XddException(tagTable, "null cursor",
                                "cursor1:$cursor1", "cursor2:$cursor2")

                    } else if (cursor1.columnCount != cursor2.columnCount) {// Column count
                        throw XddException(tagTable, "Column counts mismatch",
                                Lg.LF + Lg.TAB, cursor1.columnNames,
                                Lg.LF + Lg.TAB, cursor2.columnNames)

                    } else if (!(cursor1.columnNames contentDeepEquals cursor2.columnNames)) {
                        throw XddException(tagTable, "Column names mismatch",
                                Lg.LF + Lg.TAB, cursor1.columnNames,
                                Lg.LF + Lg.TAB, cursor2.columnNames)

                    } else if (cursor1.count != cursor2.count) {// Row count
                        throw XddException(tagTable, "Row counts mismatch",
                                "${cursor1.count} v.s. ${cursor2.count}")

                    } else {// Row content
                        var checkColType = true
                        val columnIndices = IntArray(cursor1.columnCount) { it }

                        val values1 = ContentValues()
                        val values2 = ContentValues()
                        while (cursor1.moveToNext() and cursor2.moveToNext()) {// For each row
                            if (checkColType) {
                                checkColType = !checkColType
                                // Cursor.getType must be invoked after moveToFist
                                if (columnIndices.map { cursor1.getType(it) == cursor2.getType(it) }
                                                .contains(false)) {// Column type
                                    throw XddException(tagTable, "Column types mismatch",
                                            Lg.LF + Lg.TAB, "cursor1 column type:", columnIndices.map(cursor1::getType),
                                            Lg.LF + Lg.TAB, "cursor2 column type:", columnIndices.map(cursor2::getType))
                                }
                            }

                            DatabaseUtils.cursorRowToContentValues(cursor1, values1)
                            DatabaseUtils.cursorRowToContentValues(cursor2, values2)

                            var diffValue1: ContentValues? = null
                            var diffValue2: ContentValues? = null

                            /* Extract blob(byte array) form Cursor and compare them,
                            Remove blob from ContentValues*/
                            for (indexedName in cursor1.columnNames.withIndex()) {
                                if (cursor1.getType(indexedName.index) == Cursor.FIELD_TYPE_BLOB) {
                                    values1.remove(indexedName.value)
                                    values2.remove(indexedName.value)

                                    val blob1 = cursor1.getBlob(indexedName.index)
                                    val blob2 = cursor2.getBlob(indexedName.index)
                                    if (!(blob1 contentEquals blob2)) {
                                        if (diffValue1 == null) diffValue1 = ContentValues()
                                        if (diffValue2 == null) diffValue2 = ContentValues()
                                        diffValue1.put(indexedName.value, blob1)
                                        diffValue2.put(indexedName.value, blob2)
                                    }
                                }
                            }

                            if (values1 != values2) {// Doesn't handle byte array equality
                                if (diffValue1 == null) diffValue1 = ContentValues()
                                if (diffValue2 == null) diffValue2 = ContentValues()

                                // Only show different value
                                cursor1.columnNames.forEach {
                                    val string1 = values1.getAsString(it)
                                    val string2 = values2.getAsString(it)
                                    if (string1 != string2) {
                                        diffValue1.put(it, string1)
                                        diffValue2.put(it, string2)
                                    }
                                }
                            }

                            if (diffValue1 != null && diffValue2 != null) {
                                differences.getOrPut(tableDetail.key) { mutableMapOf() }[cursor1.position] = Pair(diffValue1, diffValue2)
                            }
                        }
                    }
                } finally {
                    cursor1?.close()
                    cursor2?.close()
                }
            }
        } finally {
            db1?.close()
            db2?.close()

            Lg.log(tagDb, "All done")
        }

        return differences
    }
}
