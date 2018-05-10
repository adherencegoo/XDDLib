package com.example.xddlib

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.MessageQueue

@Suppress("unused")
object LooperDumper {
    private val mFieldMessageQueueFromLooper = Looper::class.java.getDeclaredField("mQueue").also { it.isAccessible = true }
    private val mFieldMessageFromMessageQueue = MessageQueue::class.java.getDeclaredField("mMessages").also { it.isAccessible = true }
    private val mFieldNextMessage = Message::class.java.getDeclaredField("next").also { it.isAccessible = true }

    @JvmStatic
    fun dump(handler: Handler) {
        val parser = Lg.getFinalNoTagMessage(Lg.getPrioritizedMessage(handler))
        parser.parse(Lg.LF, "Messages in looper:")

        var index = 0
        var messages = mFieldMessageFromMessageQueue.get(mFieldMessageQueueFromLooper.get(handler.looper))
        while (messages != null) {
            messages = messages.let {
                parser.parse(Lg.LF, "[${index++}]:", it)
                mFieldNextMessage.get(it)
            }
        }

        Lg.v(parser)
    }
}