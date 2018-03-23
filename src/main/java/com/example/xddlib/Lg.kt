@file:Suppress("unused")

package com.example.xddlib

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import junit.framework.Assert
import java.lang.ref.WeakReference
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
    internal const val TAG_END = ": "
    private val PRIORITIZED_MSG_PATTERN = Pattern.compile("^->\\[.+\\]$")//->[ANYTHING]
    private val ACCESS_METHOD_PATTERN = Pattern.compile("^access[$][0-9]+$")//->[ANYTHING]
    private const val MAX_PRIMITIVE_LOG_LENGTH = 3500
    internal val DEFAULT_INTERNAL_LG_TYPE = Type.V

    private val TYPES = arrayOf(Type.V, Type.D, Type.I, Type.W, Type.E)
    private val COLORS = intArrayOf(Color.WHITE, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED)

    private var sRefCachedToast: WeakReference<Toast>? = null

    enum class Type constructor(val mNativeType: Int, val mNativeFunction: ((String, String) -> Int)?) {
        V(Log.VERBOSE, Log::v),
        D(Log.DEBUG, Log::d),
        I(Log.INFO, Log::i),
        W(Log.WARN, Log::w),
        E(Log.ERROR, Log::e),
        NONE(0, null),
        UNKNOWN(-1, null)
    }

    private enum class BracketType constructor(val mLeft: String, val mRight: String) {
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
    fun v(vararg objects: Any?): ObjectArrayParser {
        return _log(Type.V, *objects)
    }

    @JvmStatic
    fun d(vararg objects: Any?): ObjectArrayParser {
        return _log(Type.D, *objects)
    }

    @JvmStatic
    fun i(vararg objects: Any?): ObjectArrayParser {
        return _log(Type.I, *objects)
    }

    @JvmStatic
    fun w(vararg objects: Any?): ObjectArrayParser {
        return _log(Type.W, *objects)
    }

    @JvmStatic
    fun e(vararg objects: Any?): ObjectArrayParser {
        return _log(Type.E, *objects)
    }

    /** @param objects: must contain Lg.Type
     */
    @JvmStatic
    fun log(vararg objects: Any?): ObjectArrayParser {
        return _log(Type.UNKNOWN, *objects)
    }

    class ObjectArrayParser internal constructor(private val mSettings: Settings) {
        internal var mNeedMethodTag: Boolean = false
        private var mInsertMainMsgDelimiter: Boolean = false

        //parsed results =====================================================
        internal var mMethodTagSource: StackTraceElement? = null//cache the first found StackTraceElement
        private var mTrArray: ArrayList<Throwable>? = null
        private var mPrioritizedMsgBuilder: StringBuilder? = null
        private var mMainMsgBuilder: StringBuilder? = null
        @JvmField
        var mLgType = Type.UNKNOWN//cache the LAST found one

        //others =====================================================
        private var mIsParsed = false
        private var mShouldOutputNull = true
        internal var mPrimitiveLogReturn = -1

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

        init {
            mNeedMethodTag = mSettings.mNeedMethodTag
            mInsertMainMsgDelimiter = mSettings.mInsertFirstMainMsgDelimiter
        }

        private fun reset(): ObjectArrayParser {
            mMethodTagSource = null
            if (mTrArray != null) mTrArray!!.clear()
            if (mPrioritizedMsgBuilder != null) mPrioritizedMsgBuilder!!.setLength(0)
            if (mMainMsgBuilder != null) mMainMsgBuilder!!.setLength(0)
            mLgType = Type.UNKNOWN

            mIsParsed = false
            return this
        }

        /** Ignore mMethodTagSource of another */
        private fun parseAnotherParser(another: ObjectArrayParser): ObjectArrayParser {
            val origNeedMethodTag = mNeedMethodTag
            val origOutputNull = mShouldOutputNull
            mNeedMethodTag = false
            mShouldOutputNull = false
            parse(if (another.mTrArray == null) null else another.mTrArray!!.toTypedArray(),
                    another.mPrioritizedMsgBuilder,
                    another.mMainMsgBuilder,
                    if (another.mLgType == Type.UNKNOWN) null else another.mLgType)
            mNeedMethodTag = origNeedMethodTag
            mShouldOutputNull = origOutputNull
            return this
        }

        fun parse(vararg objects: Any?): ObjectArrayParser {

            for (obj in objects) {
                if (obj == null && !mShouldOutputNull) continue

                //cache some info--------------------------------------
                if (mNeedMethodTag && mMethodTagSource == null && obj is StackTraceElement) {
                    mMethodTagSource = obj
                } else if (obj is Throwable) {
                    if (mTrArray == null) mTrArray = ArrayList()
                    mTrArray!!.add(obj)
                } else if (obj is List<*>
                        && obj.size > 0
                        && obj[0] is Throwable) {//List<Throwable>
                    this.parse(*obj.toTypedArray())
                } else if (obj is Type) {
                    if (obj !== Type.UNKNOWN) {
                        mLgType = obj
                    }

                    //process the data======================================================
                } else if (obj is ObjectArrayParser) {
                    parseAnotherParser(obj)
                } else if (obj is Array<*>) {//recursively parse Object[] in Object[], including native with any class type
                    val origOutputNull = mShouldOutputNull
                    mShouldOutputNull = true
                    this.parse('[')
                    this.parse(*obj)
                    this.parse(']')
                    mShouldOutputNull = origOutputNull
                } else {
                    //transform obj into string
                    //ArrayList is acceptable
                    val objStr: String = obj as? String
                            ?: if (obj != null && obj.javaClass.isArray) {//array with primitive type (array with class type has been processed in advance)
                                primitiveTypeArrayToString(obj)
                            } else {//Can't be Object[] or array with native type
                                toSimpleString(obj)
                            }

                    if (objStr.isEmpty()) continue

                    if (PRIORITIZED_MSG_PATTERN.matcher(objStr).matches()) {
                        if (mPrioritizedMsgBuilder == null) mPrioritizedMsgBuilder = StringBuilder(30)
                        mPrioritizedMsgBuilder!!.append(objStr)
                    } else {//normal string
                        if (mMainMsgBuilder == null) mMainMsgBuilder = StringBuilder(120)

                        //output the result
                        mInsertMainMsgDelimiter = mInsertMainMsgDelimiter and needDelimiterBasedOnPrefix(objStr)
                        if (mInsertMainMsgDelimiter) {
                            mMainMsgBuilder!!.append(mSettings.mDelimiter)
                        }
                        mMainMsgBuilder!!.append(mSettings.mBracket.mLeft)
                        mMainMsgBuilder!!.append(objStr)
                        mMainMsgBuilder!!.append(mSettings.mBracket.mRight)

                        mInsertMainMsgDelimiter = needDelimiterBasedOnPostfix(mMainMsgBuilder)
                    }
                }
            }

            if (mNeedMethodTag && mMethodTagSource == null) {
                mMethodTagSource = findInvokerOfDeepestInnerElementWithOffset(0)
            }

            mIsParsed = true
            return this
        }

        override fun toString(): String {
            Assert.assertTrue(mIsParsed)
            val resultBuilder = StringBuilder(200)

            //MethodTag
            if (mNeedMethodTag) {
                resultBuilder.append(getMethodTag(mMethodTagSource!!))
            }

            if (mPrioritizedMsgBuilder != null) resultBuilder.append(mPrioritizedMsgBuilder)
            if (mNeedMethodTag) resultBuilder.append(TAG_END)
            if (mMainMsgBuilder != null) resultBuilder.append(mMainMsgBuilder)

            //tr must be at the end
            if (mTrArray?.isNotEmpty() == true) {
                resultBuilder.append('\n')
                for ((idx, tr) in mTrArray!!.withIndex()) {
                    resultBuilder.append(XDD.getSeparator("[" + idx + "] " + tr.toString(), '-')).append('\n')
                    resultBuilder.append(Log.getStackTraceString(tr))
                }
                resultBuilder.append(XDD.getSeparator("Throwable end", '='))
            }

            //tag must be at the beginning
            return resultBuilder.toString()
        }

        companion object {

            /** When the previous ends with one of these chars, need no delimiter  */
            private val sDelimiterKillerPostfix = ArrayList(Arrays.asList(':', '(', '[', '{'))

            private fun needDelimiterBasedOnPostfix(builder: StringBuilder?): Boolean {
                return (builder != null
                        && builder.isNotEmpty()
                        && !sDelimiterKillerPostfix.contains(builder[builder.length - 1]))
            }

            /** When the current string starts with one of these chars, need no delimiter  */
            private val sDelimiterKillerPrefix = ArrayList(Arrays.asList(':', ')', ']', '}'))

            private fun needDelimiterBasedOnPrefix(currentString: String): Boolean {
                return !currentString.isEmpty() && !sDelimiterKillerPrefix.contains(currentString[0])
            }
        }
    }

    /**@param type: if unknown, use the result parsed from objects; if still unknown, assertion fails
     */
    @Suppress("FunctionName")
    private fun _log(type: Type, vararg objects: Any?): ObjectArrayParser {
        val parser = ObjectArrayParser(ObjectArrayParser.Settings.FinalMsg).parse(*objects)
        parser.mLgType = if (type == Type.UNKNOWN) parser.mLgType else type

        val remainingString = StringBuilder(parser.toString())
        var iteration = 0
        while (remainingString.isNotEmpty()) {
            val end: Int
            end = if (remainingString.length > MAX_PRIMITIVE_LOG_LENGTH) {
                val lastLineFeed = remainingString.lastIndexOf("\n", MAX_PRIMITIVE_LOG_LENGTH)
                if (lastLineFeed == -1) MAX_PRIMITIVE_LOG_LENGTH else lastLineFeed + 1
            } else {
                remainingString.length
            }

            val shownString = (if (iteration == 0) "" else "<Continuing ($iteration)...>\n") + remainingString.substring(0, end)
            remainingString.delete(0, end)
            parser.mPrimitiveLogReturn = parser.mLgType.mNativeFunction?.invoke(PRIMITIVE_LOG_TAG, shownString) ?: when (parser.mLgType) {
                Type.NONE -> {
                    remainingString.setLength(0)
                    -1
                }
                else -> {
                    Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + "[UsageError] Unknown Lg.Type: " + parser.mLgType)
                    -1
                }
            }
            iteration++
        }
        return parser
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
                    + ObjectArrayParser::class.java.canonicalName
                    + "." + object : Any() {}.javaClass.enclosingMethod.name
                    + "(): can't parse native array with primitive type yet: "
                    + componentType + "[]")
        }
    }

    @JvmStatic
    fun getPrioritizedMessage(vararg messages: Any?): ObjectArrayParser {
        return ObjectArrayParser(ObjectArrayParser.Settings.PrioritizedMsg).parse(*messages)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    fun getFinalNoTagMessage(vararg messages: Any?): ObjectArrayParser {
        return ObjectArrayParser(ObjectArrayParser.Settings.FinalMsgWithoutTag).parse(*messages)
    }

    /**
     * Special strings for AndroidMonitor filtering: [.PRIMITIVE_LOG_TAG], "`printStackTrace`", "`at`"
     * @param objects if containing Lg.Type and it's not UNKNOWN, print stack trace according to that Lg.Type using Lg.log; else print stack trace normally
     */
    @JvmStatic
    fun printStackTrace(vararg objects: Any?) {
        val self = object : Any() {

        }.javaClass.enclosingMethod.name
        val parser = ObjectArrayParser(ObjectArrayParser.Settings.FinalMsg)
                .parse(objects, "\n\tdirect invoker: at " + getMethodTag(findInvokerOfDeepestInnerElementWithOffset(1)))

        if (parser.mLgType == Type.UNKNOWN) {
            val prefix = "\t at "
            Exception((PRIMITIVE_LOG_TAG + TAG_END + parser)
                    .replace("\n", "\n$prefix\t$self: ")).printStackTrace()
            Log.v("System.err", prefix + XDD.getSeparator("$self end", '^'))
        } else if (parser.mLgType != Type.NONE) {//avoid redundant process
            log(parser, Exception(self))
        }
    }

    @JvmStatic
    fun showToast(context: Context, vararg objects: Any?) {
        val parser = getFinalNoTagMessage(DEFAULT_INTERNAL_LG_TYPE,
                "(" + Throwable().stackTrace[0].methodName + ")",
                objects)
        log(parser)

        XDD.mMainHandler.post {
            sRefCachedToast?.get()?.cancel()
            sRefCachedToast = WeakReference(
                    Toast.makeText(context, PRIMITIVE_LOG_TAG + TAG_END + parser, Toast.LENGTH_LONG).apply { show() })
        }
    }

    internal fun findInvokerOfDeepestInnerElementWithOffset(offset: Int): StackTraceElement {
        Assert.assertTrue(offset >= 0)
        val elements = Thread.currentThread().stackTrace//smaller index, called more recently

        for (idx in elements.indices.reversed()) {//search from the farthest to the recent
            if (elements[idx].className.startsWith(BuildConfig.APPLICATION_ID)) {
                for (jdx in idx + 1 + offset until elements.size) {
                    if (!ACCESS_METHOD_PATTERN.matcher(elements[jdx].methodName).matches()) {//skip access method like "access$000"
                        return elements[jdx]
                    }
                }
            }
        }

        Assert.fail("$PRIMITIVE_LOG_TAG$TAG_END Internal method failed!")
        return elements[0]//unreachable
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