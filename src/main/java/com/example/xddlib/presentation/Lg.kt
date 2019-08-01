@file:Suppress("unused")

package com.example.xddlib.presentation

import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.xddlib.BuildConfig
import com.example.xddlib.XDD
import junit.framework.Assert
import java.util.*
import java.util.regex.Pattern

/**
 * Created by Owen_Chen on 2018/3/23.
 * Cut and pasted from XDD
 *
 * Abbreviation of Log,
 * FinalMsg: (Abc.java:123)->testMethod->[abc]->[def]: ABC, DEF
 * MethodTag(including CodeHyperlink): (Abc.java:123)->testMethod
 * PrioritizedMsg(excluding ": ") : ->[abc]->[def]
 */
@Suppress("KDocUnresolvedReference")
object Lg {
    val PRIMITIVE_LOG_TAG = XDD::class.java.simpleName + "D"//mutable
    const val LF = "\n"
    const val TAB = "\t"
    const val BECOME = " → "
    const val ASCEND = " ↗ "
    const val DESCEND = " ↘ "
    val DELIMITER_KILLER = this
    internal const val TAG_END = ": "
    private val ACCESS_METHOD_PATTERN = Pattern.compile("^access[$][0-9]+$")//->[ANYTHING]
    private const val MAX_PRIMITIVE_LOG_LENGTH = 3500
    internal val DEFAULT_INTERNAL_LG_TYPE = Type.V

    private val TYPES = arrayOf(Type.V, Type.D, Type.I, Type.W, Type.E)
    private val COLORS = intArrayOf(Color.WHITE, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED)

    enum class Type constructor(val mNativeType: Int, val mNativeFunction: ((String, String) -> Int)?) {
        V(Log.VERBOSE, Log::v),
        D(Log.DEBUG, Log::d),
        I(Log.INFO, Log::i),
        W(Log.WARN, Log::w),
        E(Log.ERROR, Log::e),
        NONE(0, null),
        UNKNOWN(-1, null)
    }

    internal enum class BracketType constructor(val mLeft: String, val mRight: String) {
        NONE("", ""),
        ROUND("(", ")"),
        BRACKET("[", "]"),
        CURLY("{", "}"),
        ANGLE("<", ">")
    }

    @JvmStatic
    fun types(idx: Int): Type {
        return TYPES[idx % TYPES.size]
    }

    @JvmStatic
    fun colors(idx: Int): Int {
        return COLORS[idx % COLORS.size]
    }

    @JvmStatic
    fun v(vararg objects: Any?): VarargParser {
        return _log(Type.V, *objects)
    }

    @JvmStatic
    fun d(vararg objects: Any?): VarargParser {
        return _log(Type.D, *objects)
    }

    @JvmStatic
    fun i(vararg objects: Any?): VarargParser {
        return _log(Type.I, *objects)
    }

    @JvmStatic
    fun w(vararg objects: Any?): VarargParser {
        return _log(Type.W, *objects)
    }

    @JvmStatic
    fun e(vararg objects: Any?): VarargParser {
        return _log(Type.E, *objects)
    }

    /** @param objects: must contain Lg.Type
     */
    @JvmStatic
    fun log(vararg objects: Any?): VarargParser {
        return _log(Type.UNKNOWN, *objects)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @JvmStatic
    @JvmOverloads
    fun<T : Any?> become(varName: String = "", before: T, after: T): VarargParser {
        val change = if (before is Number && after is Number) {
            val compareResult = when (after) {
                is Byte -> after.compareTo(before.toByte())
                is Double -> after.compareTo(before.toDouble())
                is Float -> after.compareTo(before.toFloat())
                is Int -> after.compareTo(before.toInt())
                is Long -> after.compareTo(before.toLong())
                is Short -> after.compareTo(before.toShort())
                else -> 0
            }

            val diff = when (after) {
                is Byte -> after.minus(before.toByte())
                is Double -> after.minus(before.toDouble())
                is Float -> after.minus(before.toFloat())
                is Int -> after.minus(before.toInt())
                is Long -> after.minus(before.toLong())
                is Short -> after.minus(before.toShort())
                else -> 0
            }

            when {
                compareResult > 0 -> ASCEND
                compareResult < 0 -> DESCEND
                else -> BECOME
            } + "(" + (if (diff == 0) "-" else diff) + ") "
        } else {
            BECOME
        }

        return getFinalNoTagMessage(if (varName.isEmpty()) "" else "$varName:",
                "[",
                before,
                change, DELIMITER_KILLER,
                if (before == after) "-".repeat(Objects.toString(before).length) else after,
                "]")
    }

    class VarargParser internal constructor(private val mSettings: Settings) {
        internal var mNeedMethodTag = mSettings.mNeedMethodTag
        private var mInsertMainMsgDelimiter = mSettings.mInsertFirstMainMsgDelimiter

        //parsed results =====================================================
        internal var mMethodTagSource: StackTraceElement? = null//cache the first found StackTraceElement
        private val mMainMsgBuilder = StringBuilder(120)
        @JvmField
        var mLgType = Type.UNKNOWN//cache the LAST found one

        internal enum class Settings constructor(internal val mNeedMethodTag: Boolean,
                                                 internal val mInsertFirstMainMsgDelimiter: Boolean,
                                                 internal val mDelimiter: String,
                                                 internal val mBracket: BracketType) {
            @Suppress("KDocUnresolvedReference")
            /** ->[a]->[b]->[c]  */
            PrioritizedMsg(false, true, "->", BracketType.BRACKET),
            @Suppress("KDocUnresolvedReference")
            /** ->[a]->[b]->[c]: a, b, c  */
            FinalMsgWithoutTag(false, false, ", ", BracketType.NONE),
            @Suppress("KDocUnresolvedReference")
            /** ClassName.methodName(FileName.java:LineNumber)->[a]->[b]->[c]: a, b, c  */
            FinalMsg(true, false, ", ", BracketType.NONE)
        }

        private fun reset(): VarargParser {
            mMethodTagSource = null
            mMainMsgBuilder.setLength(0)
            mLgType = Type.UNKNOWN

            return this
        }

        /** Ignore mMethodTagSource of another */
        private fun parseAnotherParser(another: VarargParser): VarargParser {
            val origNeedMethodTag = mNeedMethodTag
            mNeedMethodTag = false

            another.mMainMsgBuilder.takeIf { it.isNotEmpty() }?.let { parse(it) }
            another.mLgType.takeIf { it !== Type.UNKNOWN }?.let { parse(it) }

            mNeedMethodTag = origNeedMethodTag
            return this
        }

        fun parse(vararg objects: Any?): VarargParser {

            for (obj in objects) {
                //cache some info--------------------------------------
                if (mNeedMethodTag && mMethodTagSource == null && obj is StackTraceElement) {
                    mMethodTagSource = obj
                } else if (obj === DELIMITER_KILLER) {
                    mInsertMainMsgDelimiter = false
                } else if (obj is Collection<*>
                        && obj.isNotEmpty()
                        && obj.first() is Throwable) {//List<Throwable>
                    this.parse(*obj.toTypedArray())
                } else if (obj is Type) {
                    if (obj !== Type.UNKNOWN) {
                        mLgType = obj
                    }

                    //process the data======================================================
                } else if (obj is VarargParser) {
                    parseAnotherParser(obj)
                } else if (obj is Array<*>) {//recursively parse Object[] in Object[], including native with any class type
                    this.parse('[')
                    this.parse(*obj)
                    this.parse(']')
                } else {
                    //transform obj into string
                    //ArrayList is acceptable
                    val objStr: String = obj as? String
                            ?: if (obj != null && obj.javaClass.isArray) {//array with primitive type (array with class type has been processed in advance)
                                primitiveTypeArrayToString(obj)
                            } else if (obj is Throwable) {
                                "\n" + XDD.getSeparator("$obj", '-') +
                                        "\n" + Log.getStackTraceString(obj)
                            } else {//Can't be Object[] or array with native type
                                toSimpleString(obj)
                            }

                    if (objStr.isEmpty()) continue

                    //output the result
                    mInsertMainMsgDelimiter = mInsertMainMsgDelimiter and needDelimiterBasedOnPrefix(objStr)
                    if (mInsertMainMsgDelimiter) {
                        mMainMsgBuilder.append(mSettings.mDelimiter)
                    }
                    mMainMsgBuilder.append(mSettings.mBracket.mLeft)
                    mMainMsgBuilder.append(objStr)
                    mMainMsgBuilder.append(mSettings.mBracket.mRight)

                    mInsertMainMsgDelimiter = needDelimiterBasedOnPostfix(mMainMsgBuilder)
                }
            }

            if (mNeedMethodTag && mMethodTagSource == null) {
                mMethodTagSource = findDisplayedStackTraceElement()
            }

            return this
        }

        override fun toString(): String {
            val resultBuilder = StringBuilder(200)

            //MethodTag
            if (mNeedMethodTag) {
                resultBuilder.append(getMethodTag(mMethodTagSource!!))
                        .append(TAG_END)
            }

            resultBuilder.append(mMainMsgBuilder)

            return resultBuilder.toString()
        }

        companion object {

            /** When the previous ends with one of these chars, need no delimiter  */
            private val sDelimiterKillerPostfix: List<String> = listOf(":", "(", "[", "{", "\n", BECOME, ASCEND, DESCEND)

            private fun needDelimiterBasedOnPostfix(builder: StringBuilder) = sDelimiterKillerPostfix.none { builder.endsWith(it) }

            /** When the current string starts with one of these chars, need no delimiter  */
            private val sDelimiterKillerPrefix: List<String> = listOf(":", ")", "]", "}", BECOME, ASCEND, DESCEND)

            private fun needDelimiterBasedOnPrefix(currentString: String) = sDelimiterKillerPrefix.none { currentString.startsWith(it) }
        }

        fun printLog(type: Type = Type.UNKNOWN) {
            type.takeIf { it !== Type.UNKNOWN }?.let { mLgType = it }

            val nativeLogFunction = when (mLgType) {
                Type.NONE -> return
                Type.UNKNOWN -> {
                    Assert.fail("$PRIMITIVE_LOG_TAG$TAG_END[UsageError] Unknown Lg.Type: $mLgType")
                    return
                }
                else -> mLgType.mNativeFunction!!
            }

            val remainingString = StringBuilder(toString())
            var iteration = 0
            while (remainingString.isNotEmpty()) {
                val end: Int = if (remainingString.length > MAX_PRIMITIVE_LOG_LENGTH) {
                    val lastLineFeed = remainingString.lastIndexOf("\n", MAX_PRIMITIVE_LOG_LENGTH)
                    if (lastLineFeed == -1) MAX_PRIMITIVE_LOG_LENGTH else lastLineFeed + 1
                } else {
                    remainingString.length
                }

                val shownString = (if (iteration == 0) "" else "<Continuing ($iteration)...>\n") + remainingString.substring(0, end)
                remainingString.delete(0, end)
                nativeLogFunction.invoke(PRIMITIVE_LOG_TAG, shownString)
                iteration++
            }
        }

        @JvmOverloads
        fun printStackTrace(type: Type = Type.UNKNOWN) {
            type.takeIf { it !== Type.UNKNOWN }?.let { mLgType = it }

            val funName = "printStackTrace"
            if (mLgType === Type.UNKNOWN) {
                val prefix = "\t at "
                Exception((PRIMITIVE_LOG_TAG + TAG_END + this)
                        .replace("\n", "\n$prefix\t$funName: ")).printStackTrace()
                Log.v("System.err", prefix + XDD.getSeparator("$funName end", '^'))
            } else if (mLgType !== Type.NONE) {//avoid redundant process
                parse(Exception(funName)).printLog()
            }
        }
    }

    /**@param type: if unknown, use the result parsed from objects; if still unknown, assertion fails
     */
    @Suppress("FunctionName")
    private fun _log(type: Type, vararg objects: Any?): VarargParser
            = VarargParser(VarargParser.Settings.FinalMsg).apply {
        parse(*objects)
        printLog(type)
    }

    /** @return true if toString method of the any is not ever overridden
     */
    private fun isToStringFromObjectClass(any: Any): Boolean {
        try {
            return (any !is CharSequence //including String
                    && any !is Throwable //including Exception
                    && any !is AbstractCollection<*> //including ArrayList, LinkedList ...
                    && any.javaClass.getMethod("toString").declaringClass.canonicalName == Any::class.java.canonicalName)
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }

        return true
    }

    /** @return equivalent call to NON-OVERRIDDEN Object.toString without package name
     */
    @JvmStatic
    fun toNativeSimpleString(any: Any?): String {
        return if (any == null) "null" else removePackageNameIfNeeded(any.javaClass.name + "@" + Integer.toHexString(any.hashCode()))

    }

    /** @return toString without package name if toString is not overridden
     */
    @JvmStatic
    fun toSimpleString(any: Any?): String {
        if (any == null) return "null"

        var objStr = any.toString()
        if (isToStringFromObjectClass(any)) {
            objStr = removePackageNameIfNeeded(objStr)
        }
        return objStr
    }

    private fun removePackageNameIfNeeded(string: String): String {
        val dotPos: Int = string.lastIndexOf('.')
        return if (dotPos != -1) {
            string.substring(dotPos + 1)//OuterClass$InnerClass
        } else {
            string
        }
    }

    private fun primitiveTypeArrayToString(obj: Any?): String {
        if (obj == null) return "null"
        Assert.assertTrue(obj.javaClass.isArray)
        val componentType = obj.javaClass.componentType.kotlin
        return when (componentType) {
            Byte::class -> Arrays.toString(obj as ByteArray?)
            Short::class -> Arrays.toString(obj as ShortArray?)
            Int::class -> Arrays.toString(obj as IntArray?)
            Long::class -> Arrays.toString(obj as LongArray?)
            Float::class -> Arrays.toString(obj as FloatArray?)
            Double::class -> Arrays.toString(obj as DoubleArray?)
            Char::class -> Arrays.toString(obj as CharArray?)
            Boolean::class -> Arrays.toString(obj as BooleanArray?)
            else -> throw UnsupportedOperationException(PRIMITIVE_LOG_TAG + TAG_END
                    + VarargParser::class.java.canonicalName
                    + "." + object : Any() {}.javaClass.enclosingMethod.name
                    + "(): can't parse native array with primitive type yet: "
                    + componentType + "[]")
        }
    }

    @JvmStatic
    fun getPrioritizedMessage(vararg messages: Any?): VarargParser {
        return VarargParser(VarargParser.Settings.PrioritizedMsg).parse(*messages)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    fun getFinalNoTagMessage(vararg messages: Any?): VarargParser {
        return VarargParser(VarargParser.Settings.FinalMsgWithoutTag).parse(*messages)
    }

    /**
     * Special strings for AndroidMonitor filtering: [.PRIMITIVE_LOG_TAG], "`printStackTrace`", "`at`"
     * @param objects if containing Lg.Type and it's not UNKNOWN, print stack trace according to that Lg.Type using Lg.log; else print stack trace normally
     */
    @JvmStatic
    fun printStackTrace(vararg objects: Any?): VarargParser
            = VarargParser(VarargParser.Settings.FinalMsg).apply {
        parse(*objects)
        printStackTrace()
    }

    @JvmStatic
    fun vPrintStackTrace(vararg objects: Any?) = printStackTrace(Type.V, *objects)
    @JvmStatic
    fun dPrintStackTrace(vararg objects: Any?) = printStackTrace(Type.D, *objects)
    @JvmStatic
    fun iPrintStackTrace(vararg objects: Any?) = printStackTrace(Type.I, *objects)
    @JvmStatic
    fun wPrintStackTrace(vararg objects: Any?) = printStackTrace(Type.W, *objects)
    @JvmStatic
    fun ePrintStackTrace(vararg objects: Any?) = printStackTrace(Type.E, *objects)


    internal fun findDisplayedStackTraceElement(): StackTraceElement {
        return Thread.currentThread().stackTrace.first { // Smaller index, called more recently
            !it.className.startsWith("dalvik.system.VMStack")
                    && !it.className.startsWith("java.lang.Thread")
                    && !it.className.startsWith(BuildConfig.APPLICATION_ID)
                    && !ACCESS_METHOD_PATTERN.matcher(it.methodName).matches() // Skip access method like "access$000"
        }
    }

    /** (FileName.java:LineNumber)->OuterClass$InnerClass.MethodName  */
    private fun getMethodTag(targetElement: StackTraceElement): String {
        val string = targetElement.toString()
        //remove package name
        //ex: packageName.ClassName.methodName(FileName.java:LineNumber)
        var dotIdx = string.length
        for (idx in 0..2) {//find the 3rd dot starting from the end
            dotIdx = string.lastIndexOf('.', dotIdx - 1)
        }
        return string.substring(dotIdx + 1)
    }
}