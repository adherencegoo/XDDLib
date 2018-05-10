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

    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    fun getMessages(handler: Handler): List<Message> {
        val messageList = mutableListOf<Message>()
        var message = mFieldMessageFromMessageQueue.get(mFieldMessageQueueFromLooper.get(handler.looper)) as? Message
        while (message != null) {
            message = message.let {
                messageList.add(it)
                mFieldNextMessage.get(it) as? Message
            }
        }
        return messageList
    }

    @JvmStatic
    fun dump(handler: Handler) {
        Lg.v(Lg.getPrioritizedMessage(handler),
                "\nMessages in looper:\n",
                getMessages(handler)
                        .withIndex()
                        .joinToString(separator = ",\n", prefix = "[ ", postfix = " ]") {"${it.index}: ${it.value}"})
    }
}