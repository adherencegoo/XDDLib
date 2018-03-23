@file:Suppress("unused")

package com.example.xddlib

import android.support.annotation.IntRange
import junit.framework.Assert
import java.util.*

/**
 * Created by Owen_Chen on 2018/3/23.
 */
private class Timing internal constructor(internal val mId: Any, internal val mInfo: Lg.ObjectArrayParser) {
    internal var t1 = java.lang.Long.MAX_VALUE
    internal var t2 = java.lang.Long.MIN_VALUE

    private val internalElapsedTime: Long
        get() {
            val result = t2 - t1
            Assert.assertTrue(result >= 0)
            return result
        }

    override fun toString(): String {
        return "internal: " + internalElapsedTime + "ms, " + mInfo
    }

    internal fun subtract(past: Timing): Long {
        return (t1 - past.t2).also { Assert.assertTrue(it >= 0) }
    }
}

private class Timeline internal constructor(internal val mId: Any) {
    private val mTimings = ArrayList<Timing>()

    internal fun tick(info: Lg.ObjectArrayParser): Timing {
        return Timing(mId, info).also { mTimings.add(it) }
    }

    /**Excluding the time of my own procedure */
    private fun calculateInterestingElapsedTime(): Long {
        Assert.assertTrue(mTimings.size >= 2)

        var result: Long = 0
        var previous: Timing? = null
        for (current in mTimings) {
            previous?.let { result += current.subtract(it) }
            previous = current
        }

        Assert.assertTrue(result >= 0)
        return result
    }

    /**Including the time of my own procedure */
    private fun calculateRealElapsedTime(): Long {
        Assert.assertTrue(mTimings.size >= 2)
        return (mTimings[mTimings.size - 1].t2 - mTimings[0].t1).also { Assert.assertTrue(it >= 0) }
    }

    private fun calculateInternalElapsedTime(): Long {
        return (calculateRealElapsedTime() - calculateInterestingElapsedTime()).also { Assert.assertTrue(it >= 0) }
    }

    override fun toString(): String {
        Assert.assertTrue(mTimings.size >= 2)
        val builder = StringBuilder(mTimings.size * 80)

        var previous: Timing? = null
        for ((idx, current) in mTimings.withIndex()) {
            previous?.let { builder.append("\n\t\t↓ ").append(current.subtract(it)).append("ms") }
            builder.append("\n[").append(idx).append("] ").append(current)

            previous = current
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

    internal fun clear() {
        mTimings.clear()
    }

    internal fun size(): Int {
        return mTimings.size
    }
}

private object TimelineManager {
    private val mTimelineCollection = HashMap<Any, Timeline>(3)

    /**
     * If id==null:<br></br>
     * If number of timelines is 1, return it;<br></br>
     * else: assert fail<br></br>
     * else:<br></br>
     * If target found: return it<br></br>
     * else: create a new timeline using given id */
    internal fun getTargetTimeline(id: Any?, asNew: Boolean): Timeline {
        if (asNew) Assert.assertTrue(id != null)

        var target: Timeline? = null
        if (id == null) {
            if (mTimelineCollection.size == 1) {//get the only timeline
                target = mTimelineCollection.values.iterator().next()
            } else {
                Assert.fail(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END + "There are " + mTimelineCollection.size + " Timelines, but no id is given")
            }
        } else {
            target = mTimelineCollection[id]
            if (target == null) {//Create a timeline using given id
                target = Timeline(id)
                mTimelineCollection[id] = target
            }
        }

        if (asNew) {
            target!!.clear()
        } else {
            Assert.assertTrue(Lg.PRIMITIVE_LOG_TAG + Lg.TAG_END + "Should not be empty, but it is actually", target!!.size() != 0)
        }

        return target
    }

    internal fun remove(id: Any) {
        mTimelineCollection.remove(id)?.clear()
    }
}

//======================================================================

object Tm {
    /**
     * @param id used as the identifier of new timeline, leave null to use current timestamp for default
     * @return id
     */
    @JvmStatic
    fun begin(id: Any?, vararg objects: Any?): Any {
        val t1 = System.currentTimeMillis()

        val timeline = TimelineManager.getTargetTimeline(id ?: t1, true)
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
    @JvmStatic
    fun tick(id: Any?, vararg objects: Any?): Any {
        val t1 = System.currentTimeMillis()

        val timeline = TimelineManager.getTargetTimeline(id, false)
        val timing = timeline.tick(Lg.log(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "timer ticks", objects))

        timing.t1 = t1
        timing.t2 = System.currentTimeMillis()
        return timing.mId
    }

    /**
     * @param id see [.tick]
     */
    @JvmStatic
    fun end(id: Any?, vararg objects: Any?): Lg.ObjectArrayParser {
        val t1 = System.currentTimeMillis()

    val timeline = TimelineManager.getTargetTimeline(id, false)
    val timing = timeline.tick(//need not output log at this moment
            Lg.ObjectArrayParser(Lg.ObjectArrayParser.Settings.FinalMsg)
                    .parse(Lg.DEFAULT_INTERNAL_LG_TYPE, Lg.getPrioritizedMessage("id:" + timeline.mId), "end timer!", objects))

        timing.t1 = t1
        timing.t2 = System.currentTimeMillis()

    //about 1ms for the following actions
    val parser = Lg.log(timing.mInfo.mLgType/*reuse*/, timing.mInfo.mMethodTagSource/*reuse*/, timeline)//output the elapsed time
    TimelineManager.remove(timeline.mId)
    return parser
}

    @JvmStatic
    fun sleep(ms: Long, vararg objects: Any?) {
        sleepManyTimes(ms, 1, *objects)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    fun sleepManyTimes(ms: Long,
                       @IntRange(from = 1, to = Integer.MAX_VALUE.toLong()) count: Int,
                       vararg objects: Any?) {
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

    Lg.log(parser, "wake up")}
}