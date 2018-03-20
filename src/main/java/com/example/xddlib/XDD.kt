package com.example.xddlib

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import android.support.annotation.IntRange
import android.text.TextUtils
import android.util.Log
import android.widget.Toast

import com.example.xddlib.xddpref.data.NativePreferenceHelper

import junit.framework.Assert

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.AbstractCollection
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/** Created by Owen_Chen on 2017/3/15.  */

//@SuppressWarnings("unused")
object XDD {
    private val DEFAULT_REPEAT_COUNT = 30

    private var mMainHandler: Handler? = null

    val isMainThread: Boolean
        get() = if (Build.VERSION.SDK_INT >= 23) {
            Looper.getMainLooper().isCurrentThread
        } else {
            Looper.getMainLooper() == Looper.myLooper()
        }

    private val sIsActionDialogShowing = AtomicBoolean(false)

    fun init(context: Context) {
        mMainHandler = Handler(context.mainLooper)
        NativePreferenceHelper.init(context)
    }

    private enum class BracketType private constructor(val mLeft: String, val mRight: String) {
        NONE("", ""),
        ROUND("(", ")"),
        BRACKET("[", "]"),
        CURLY("{", "}"),
        ANGLE("<", ">")
    }

    enum class CtrlKey {
        TEST;

        private var mValue: Any? = null

        /** Reset the value after invoking getter  */
        private val value: Any?
            get() {
                val returned = mValue
                mValue = null
                return returned
            }

        /**@return the enum itself
         */
        fun setValue(value: Any?): CtrlKey {
            mValue = value
            return this
        }
    }

    /**
     * Abbreviation of Log,
     * FinalMsg: (Abc.java:123)->testMethod->[abc]->[def]: ABC, DEF
     * MethodTag(including CodeHyperlink): (Abc.java:123)->testMethod
     * PrioritizedMsg(excluding “: ”) : ->[abc]->[def]
     */
    object Lg {
        val PRIMITIVE_LOG_TAG = XDD::class.java.simpleName + "D"//mutable
        val LF = "\n"
        val TAB = "\t"
        private val TAG_END = ": "
        private val PRIORITIZED_MSG_PATTERN = Pattern.compile("^->\\[.+\\]$")//->[ANYTHING]
        private val ACCESS_METHOD_PATTERN = Pattern.compile("^access[$][0-9]+$")//->[ANYTHING]
        private val MAX_PRIMITIVE_LOG_LENGTH = 3500
        private val DEFAULT_INTERNAL_LG_TYPE = Type.V

        private val TYPES = arrayOf(Type.V, Type.D, Type.I, Type.W, Type.E)
        private val COLORS = intArrayOf(Color.WHITE, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED)

        private var sRefCachedToast: WeakReference<Toast>? = null

        enum class Type private constructor(val mValue: Int) {
            V(Log.VERBOSE), D(Log.DEBUG), I(Log.INFO), W(Log.WARN), E(Log.ERROR),
            NONE(0), UNKNOWN(-1)
        }

        fun types(idx: Int): Type {
            return TYPES[idx % TYPES.size]
        }

        fun colors(idx: Int): Int {
            return COLORS[idx % COLORS.size]
        }

        fun v(vararg objects: Any): ObjectArrayParser {
            return _log(Type.V, *objects)
        }

        fun d(vararg objects: Any): ObjectArrayParser {
            return _log(Type.D, *objects)
        }

        fun i(vararg objects: Any): ObjectArrayParser {
            return _log(Type.I, *objects)
        }

        fun w(vararg objects: Any): ObjectArrayParser {
            return _log(Type.W, *objects)
        }

        fun e(vararg objects: Any): ObjectArrayParser {
            return _log(Type.E, *objects)
        }

        /** @param objects: must contain Lg.Type
         */
        fun log(vararg objects: Any): ObjectArrayParser {
            return _log(Type.UNKNOWN, *objects)
        }

        class ObjectArrayParser private constructor(//settings =====================================================
                private val mSettings: Settings) {
            private var mNeedMethodTag: Boolean = false
            private var mInsertMainMsgDelimiter: Boolean = false

            //parsed results =====================================================
            private var mMethodTagSource: StackTraceElement? = null//cache the first found StackTraceElement
            private var mTrArray: ArrayList<Throwable>? = null
            private var mPrioritizedMsgBuilder: StringBuilder? = null
            private var mMainMsgBuilder: StringBuilder? = null
            var mLgType = Type.UNKNOWN//cache the LAST found one

            //others =====================================================
            private var mIsParsed = false
            private var mShouldOutputNull = true
            private val mPrimitiveLogReturn = -1

            private enum class Settings private constructor(private val mNeedMethodTag: Boolean,
                                                            private val mInsertFirstMainMsgDelimiter: Boolean,
                                                            private val mDelimiter: String,
                                                            private val mBracket: BracketType) {
                /** ->[a]->[b]->[c]  */
                PrioritizedMsg(false, true, "->", BracketType.BRACKET),
                /** ->[a]->[b]->[c]: a, b, c  */
                FinalMsgWithoutTag(false, false, ", ", BracketType.NONE),
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

            fun parse(vararg objects: Any): ObjectArrayParser {

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
                    } else if (obj is Array<Any>) {//recursively parse Object[] in Object[], including native with any class type
                        val origOutputNull = mShouldOutputNull
                        mShouldOutputNull = true
                        this.parse('[')
                        this.parse(*obj)
                        this.parse(']')
                        mShouldOutputNull = origOutputNull
                    } else if (obj !is CtrlKey) {
                        //transform obj into string
                        //ArrayList is acceptable
                        val objStr: String
                        if (obj is String) {
                            objStr = obj
                        } else if (obj != null && obj.javaClass.isArray) {//array with primitive type (array with class type has been processed in advance)
                            objStr = primitiveTypeArrayToString(obj)
                        } else {//Can't be Object[] or array with native type
                            objStr = toSimpleString(obj)
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
                if (mTrArray != null && mTrArray!!.size != 0) {
                    resultBuilder.append('\n')
                    var idx = 0
                    for (tr in mTrArray!!) {
                        resultBuilder.append(getSeparator("[" + idx + "] " + tr.toString(), '-')).append('\n')
                        resultBuilder.append(Log.getStackTraceString(tr))
                        idx++
                    }
                    resultBuilder.append(getSeparator("Throwable end", '='))
                }

                //tag must be at the beginning
                return resultBuilder.toString()
            }

            companion object {

                /** When the previous ends with one of these chars, need no delimiter  */
                private val sDelimiterKillerPostfix = ArrayList(Arrays.asList(':', '(', '[', '{'))

                private fun needDelimiterBasedOnPostfix(builder: StringBuilder?): Boolean {
                    return (builder != null
                            && builder.length != 0
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
        private fun _log(type: Type, vararg objects: Any): ObjectArrayParser {
            val parser = ObjectArrayParser(ObjectArrayParser.Settings.FinalMsg).parse(*objects)
            parser.mLgType = if (type == Type.UNKNOWN) parser.mLgType else type

            val remainingString = StringBuilder(parser.toString())
            var iteration = 0
            while (remainingString.length > 0) {
                val end: Int
                if (remainingString.length > MAX_PRIMITIVE_LOG_LENGTH) {
                    val lastLineFeed = remainingString.lastIndexOf("\n", MAX_PRIMITIVE_LOG_LENGTH)
                    end = if (lastLineFeed == -1) MAX_PRIMITIVE_LOG_LENGTH else lastLineFeed + 1
                } else {
                    end = remainingString.length
                }

                val shownString = (if (iteration == 0) "" else "<Continuing ($iteration)...>\n") + remainingString.substring(0, end)
                remainingString.delete(0, end)
                when (parser.mLgType) {
                    XDD.Lg.Type.V -> parser.mPrimitiveLogReturn = Log.v(PRIMITIVE_LOG_TAG, shownString)
                    XDD.Lg.Type.D -> parser.mPrimitiveLogReturn = Log.d(PRIMITIVE_LOG_TAG, shownString)
                    XDD.Lg.Type.I -> parser.mPrimitiveLogReturn = Log.i(PRIMITIVE_LOG_TAG, shownString)
                    XDD.Lg.Type.W -> parser.mPrimitiveLogReturn = Log.w(PRIMITIVE_LOG_TAG, shownString)
                    XDD.Lg.Type.E -> parser.mPrimitiveLogReturn = Log.e(PRIMITIVE_LOG_TAG, shownString)
                    XDD.Lg.Type.NONE -> {
                        parser.mPrimitiveLogReturn = -1
                        remainingString.setLength(0)
                    }
                    else -> {
                        Assert.fail(PRIMITIVE_LOG_TAG + TAG_END + "[UsageError] Unknown Lg.Type: " + parser.mLgType)
                        parser.mPrimitiveLogReturn = -1
                    }
                }
                iteration++
            }
            return parser
        }

        /** @return true if toString method of the object is not ever overridden
         */
        private fun isToStringFromObjectClass(`object`: Any): Boolean {
            try {
                return //add some common cases to avoid the last condition
                (`object` !is CharSequence //including String

                        && `object` !is Throwable //including Exception

                        && `object` !is AbstractCollection<*> //including ArrayList, LinkedList ...

                        && `object`.javaClass.getMethod("toString").declaringClass
                        .canonicalName == Any::class.java.canonicalName)
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }

            return true
        }

        /** @return equivalent call to NON-OVERRIDDEN Object.toString without package name
         */
        fun toNativeSimpleString(`object`: Any?): String {
            return if (`object` == null) "null" else removePackageNameIfNeeded(`object`.javaClass.name + "@" + Integer.toHexString(`object`.hashCode()))

        }

        /** @return toString without package name if toString is not overridden
         */
        fun toSimpleString(`object`: Any?): String {
            if (`object` == null) return "null"

            var objStr = `object`.toString()
            if (isToStringFromObjectClass(`object`)) {
                objStr = removePackageNameIfNeeded(objStr)
            }
            return objStr
        }

        private fun removePackageNameIfNeeded(`in`: String): String {
            val dotPos: Int
            var out = `in`
            if ((dotPos = `in`.lastIndexOf('.')) != -1) {
                out = out.substring(dotPos + 1)//OuterClass$InnerClass
            }
            return out
        }

        fun primitiveTypeArrayToString(obj: Any?): String {
            if (obj == null) return "null"
            Assert.assertTrue(obj.javaClass.isArray)
            val componentType = obj.javaClass.componentType
            when (componentType.toString()) {
                "byte" -> return Arrays.toString(obj as ByteArray?)
                "short" -> return Arrays.toString(obj as ShortArray?)
                "int" -> return Arrays.toString(obj as IntArray?)
                "long" -> return Arrays.toString(obj as LongArray?)
                "float" -> return Arrays.toString(obj as FloatArray?)
                "double" -> return Arrays.toString(obj as DoubleArray?)
                "char" -> return Arrays.toString(obj as CharArray?)
                "boolean" -> return Arrays.toString(obj as BooleanArray?)
            }
            throw UnsupportedOperationException(PRIMITIVE_LOG_TAG + TAG_END
                    + ObjectArrayParser::class.java.canonicalName
                    + "." + object : Any() {

            }.javaClass.enclosingMethod.name
                    + "(): can't parse native array with primitive type yet: "
                    + componentType + "[]")
        }

        fun getPrioritizedMessage(vararg messages: Any): ObjectArrayParser {
            return ObjectArrayParser(ObjectArrayParser.Settings.PrioritizedMsg).parse(*messages)
        }

        fun getFinalNoTagMessage(vararg messages: Any): ObjectArrayParser {
            return ObjectArrayParser(ObjectArrayParser.Settings.FinalMsgWithoutTag).parse(*messages)
        }

        /**
         * Special strings for AndroidMonitor filtering: [.PRIMITIVE_LOG_TAG], "`printStackTrace`", "`at`"
         * @param objects if containing Lg.Type and it's not UNKNOWN, print stack trace according to that Lg.Type using Lg.log; else print stack trace normally
         */
        fun printStackTrace(vararg objects: Any) {
            val self = object : Any() {

            }.javaClass.enclosingMethod.name
            val parser = ObjectArrayParser(ObjectArrayParser.Settings.FinalMsg)
                    .parse(objects, "\n\tdirect invoker: at " + getMethodTag(findInvokerOfDeepestInnerElementWithOffset(1)))

            if (parser.mLgType == Type.UNKNOWN) {
                val prefix = "\t at "
                Exception((PRIMITIVE_LOG_TAG + TAG_END + parser)
                        .replace("\n", "\n$prefix\t$self: ")).printStackTrace()
                Log.v("System.err", prefix + getSeparator("$self end", '^'))
            } else if (parser.mLgType != Type.NONE) {//avoid redundant process
                Lg.log(parser, Exception(self))
            }
        }

        fun showToast(context: Context, vararg objects: Any) {
            val parser = getFinalNoTagMessage(DEFAULT_INTERNAL_LG_TYPE,
                    "(" + Throwable().stackTrace[0].methodName + ")",
                    objects)
            log(parser)

            mMainHandler!!.post {
                var toast: Toast
                if (sRefCachedToast != null && (toast = sRefCachedToast!!.get()) != null) toast.cancel()
                toast = Toast.makeText(context, PRIMITIVE_LOG_TAG + TAG_END + parser, Toast.LENGTH_LONG)
                toast.show()
                sRefCachedToast = WeakReference(toast)
            }
        }

        private fun findInvokerOfDeepestInnerElementWithOffset(offset: Int): StackTraceElement {
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

    /** Abbreviation of Time */
    object Tm {

        private val sManager = TimelineManager()

        private class Timing private constructor(private val mId: Any, private val mInfo: Lg.ObjectArrayParser) {
            private val t1 = java.lang.Long.MAX_VALUE
            private val t2 = java.lang.Long.MIN_VALUE

            private val internalElapsedTime: Long
                get() {
                    val result = t2 - t1
                    Assert.assertTrue(result >= 0)
                    return result
                }

            override fun toString(): String {
                return "internal: " + internalElapsedTime + "ms, " + mInfo
            }

            private fun subtract(past: Timing): Long {
                val result = t1 - past.t2
                Assert.assertTrue(result >= 0)
                return result
            }
        }

        private class Timeline private constructor(private val mId: Any) {
            private val mTimings = ArrayList<Timing>()

            private fun tick(info: Lg.ObjectArrayParser): Timing {
                val timing = Timing(mId, info)
                mTimings.add(timing)
                return timing
            }

            /**Excluding the time of my own procedure */
            private fun calculateInterestingElapsedTime(): Long {
                Assert.assertTrue(mTimings.size >= 2)

                var result: Long = 0
                var previous: Timing? = null
                for (current in mTimings) {
                    if (previous != null) result += current.subtract(previous)
                    previous = current
                }

                Assert.assertTrue(result >= 0)
                return result
            }

            /**Including the time of my own procedure */
            private fun calculateRealElapsedTime(): Long {
                Assert.assertTrue(mTimings.size >= 2)
                val result = mTimings[mTimings.size - 1].t2 - mTimings[0].t1
                Assert.assertTrue(result >= 0)
                return result
            }

            private fun calculateInternalElapsedTime(): Long {
                val result = calculateRealElapsedTime() - calculateInterestingElapsedTime()
                Assert.assertTrue(result >= 0)
                return result
            }

            override fun toString(): String {
                Assert.assertTrue(mTimings.size >= 2)
                val builder = StringBuilder(mTimings.size * 80)

                var previous: Timing? = null
                var idx = 0
                for (current in mTimings) {
                    if (previous != null) {
                        builder.append("\n\t\t↓ ").append(current.subtract(previous)).append("ms")
                    }
                    builder.append("\n[").append(idx).append("] ").append(current)

                    previous = current
                    idx++
                }

                val interestingElapsed = calculateInterestingElapsedTime()
                val realElapsed = calculateRealElapsedTime()
                val internalElapsed = realElapsed - interestingElapsed
                builder.append("\nTotal elapsed time: ").append(interestingElapsed).append("ms")

                //test
                builder.append("; [TEST] Real total elapsed time: ").append(realElapsed)
                        .append("ms, internal elapsed time: ").append(internalElapsed).append("ms")

                return builder.toString()
            }

            private fun clear() {
                mTimings.clear()
            }

            private fun size(): Int {
                return mTimings.size
            }
        }

        private class TimelineManager {
            private val mTimelines = HashMap<Any, Timeline>(3)

            /**
             * If id==null:<br></br>
             * If number of timelines is 1, return it;<br></br>
             * else: assert fail<br></br>
             * else:<br></br>
             * If target found: return it<br></br>
             * else: create a new timeline using given id */
            private fun getTargetTimeline(id: Any?, asNew: Boolean): Timeline {
                if (asNew) Assert.assertTrue(id != null)

                var target: Timeline? = null
                if (id == null) {
                    if (mTimelines.size == 1) {//get the only timeline
                        target = mTimelines.values.iterator().next()
                    } else {
                        Assert.fail(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END + "There are " + mTimelines.size + " Timelines, but no id is given")
                    }
                } else {
                    target = mTimelines[id]
                    if (target == null) {//Create a timeline using given id
                        target = Timeline(id)
                        mTimelines[id] = target
                    }
                }

                if (asNew) {
                    target!!.clear()
                } else {
                    Assert.assertTrue(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END + "Should not be empty, but it is actually", target!!.size() != 0)
                }

                return target
            }

            private fun remove(id: Any) {
                val removed = mTimelines.remove(id)
                removed?.clear()
            }
        }

        //======================================================================

        /**
         * @param id used as the identifier of new timeline, leave null to use current timestamp for default
         * @return id
         */
        fun begin(id: Any?, vararg objects: Any): Any {
            val t1 = System.currentTimeMillis()

            val timeline = sManager.getTargetTimeline(id ?: t1, true)
            val timing = timeline.tick(Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "start timer~", objects))

            timing.t1 = t1
            timing.t2 = System.currentTimeMillis()
            return timing.mId
        }

        /**
         * @param id used to identify which timeline to use, leave null to<br></br>
         * if there is exactly only one, use it<br></br>
         * else, throw exception
         * @return id
         */
        fun tick(id: Any?, vararg objects: Any): Any {
            val t1 = System.currentTimeMillis()

            val timeline = sManager.getTargetTimeline(id, false)
            val timing = timeline.tick(Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "timer ticks", objects))

            timing.t1 = t1
            timing.t2 = System.currentTimeMillis()
            return timing.mId
        }

        /**
         * @param id see [.tick]
         */
        fun end(id: Any?, vararg objects: Any): Lg.ObjectArrayParser {
            val t1 = System.currentTimeMillis()

            val timeline = sManager.getTargetTimeline(id, false)
            val timing = timeline.tick(//need not output log at this moment
                    Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg)
                            .parse(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "end timer!", objects))

            timing.t1 = t1
            timing.t2 = System.currentTimeMillis()

            //about 1ms for the following actions
            val `object` = Lg.log(timing.mInfo.mLgType/*reuse*/, timing.mInfo.mMethodTagSource/*reuse*/, timeline)//output the elapsed time
            sManager.remove(timeline.mId)
            return `object`
        }

        fun sleep(ms: Long, vararg objects: Any) {
            sleepManyTimes(ms, 1, *objects)
        }

        fun sleepManyTimes(ms: Long,
                           @IntRange(from = 1, to = Integer.MAX_VALUE.toLong()) count: Int,
                           vararg objects: Any) {
            val timestamp = System.currentTimeMillis()

            val parser = Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg)
                    .parse(Lg.DEFAULT_INTERNAL_LG_TYPE, "timestamp:$timestamp", objects)

            try {
                for (i in 0 until count) {
                    Lg.log(parser, String.format(Locale.getDefault(), "[%d/%d] go to sleep %d ms", i, count, ms))
                    Thread.sleep(ms)
                }
            } catch (e: InterruptedException) {
                Lg.e(e)
            }

            Lg.log(parser, "wake up")
        }
    }

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

    fun saveBitmap(context: Context, bitmap: Bitmap?, fileName: String,
                   vararg objects: Any) {
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

    fun stringRepeat(str: String): String {
        return stringRepeat(DEFAULT_REPEAT_COUNT, str)
    }

    fun stringRepeat(count: Int, str: String): String {
        return TextUtils.join("", Collections.nCopies(count, str))
    }

    fun dbUpdate(context: Context,
                 uri: Uri,
                 values: ContentValues,
                 id: Long) {
        val whereClause = if (id > 0) MediaStore.Video.Media._ID + " = ?" else null
        val whereContent = if (id > 0) arrayOf(java.lang.Long.toString(id)) else null
        val rowCount = context.contentResolver.update(uri, values, whereClause, whereContent)
        Lg.d("updated row count:$rowCount")
    }

    fun argbIntToHexString(@ColorInt color: Int): String {
        return String.format("#%08x", color)
    }

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
                val kParsedObjects = Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg).parse(*objects)
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
                    methodName: String?) : this(fileName, klass.name, methodName) {
        }

        constructor(fileName: String?,
                    klass: Class<*>,
                    methodName: String?,
                    lineNumber: Int) : this(fileName, klass.name, methodName, lineNumber) {
        }

        init {
            //All illegal -> fail! (At least one element must be legal)
            Assert.assertFalse(fileName == null && kPartialClassName == null && kMethodName == null && kLineNumber <= 0)

            kFileName = if (fileName != null && !fileName.endsWith(".java")) "$fileName.java" else fileName
        }

        private fun isMatched(element: StackTraceElement): Boolean {
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

    fun vibrate(context: Context, ms: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator?.vibrate(ms)
    }
}
