package com.example.xddlib.analysis

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.example.xddlib.presentation.Lg
import java.io.File
import java.io.FileNotFoundException
import java.security.InvalidParameterException

@Suppress("unused")
object XddDatabase {
    @JvmStatic
    fun dumpCheckEqualityResult(differences: Map<String, Map<Int, Pair<ContentValues, ContentValues>>>): String {
        return differences.entries.joinToString(separator = ";\n") {
            "\ntable:${it.key} --> \n" + it.value.entries.joinToString(separator = ";\n") {
                "\trowIndex:${it.key} --> \n" +
                        "\t\tvalue1: ${it.value.first}\n" +
                        "\t\tvalue2: ${it.value.second}"
            }
        }
    }

    @JvmStatic
    fun checkEquality(fullPath1: String,
                              fullPath2: String,
                              tables: List<String>)
            : Map<String, Map<Int, Pair<ContentValues, ContentValues>>> {
        return checkEquality(fullPath1, fullPath2, tables.map { it to null }.toMap())
    }

    /**@param tableDetailList key: tableName, value: interesting column names (take all columns if null)
     * @return key: tableName, value: pairs of different [ContentValues]s indexed with rowIndex*/
    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    @Throws(SQLiteException::class, XddException::class)
    fun checkEquality(fullPath1: String,
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
                var tableTheSame = true

                var cursor1: Cursor? = null
                var cursor2: Cursor? = null
                try {
                    val columns = tableDetail.value?.toTypedArray() // Nullable
                    columns?.let { Lg.w(tagTable, "Not comparing all columns, only:\n", it) }
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
                                tableTheSame = false
                            }
                        }
                    }
                } finally {
                    cursor1?.close()
                    cursor2?.close()
                }

                Lg.log(tagTable,
                        if (tableTheSame) Lg.Type.I else Lg.Type.W,
                        "table comparison: ${if (tableTheSame) "same" else "different"}")
            }
        } finally {
            db1?.close()
            db2?.close()

            Lg.log(tagDb, "All done")
        }

        return differences
    }
}