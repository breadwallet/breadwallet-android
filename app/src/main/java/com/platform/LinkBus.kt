package com.platform

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import java.lang.Exception

object LinkBus {
    private val messageChannel = BroadcastChannel<LinkMessage>(Channel.BUFFERED)

    fun sendMessage(message: LinkMessage) {
        messageChannel.offer(message)
    }

    fun requests() =
        messageChannel.asFlow().filterIsInstance<LinkRequestMessage>()

    fun results() =
        messageChannel.asFlow().filterIsInstance<LinkResultMessage>()
}

sealed class LinkMessage

data class LinkRequestMessage(
    val url: String,
    val jsonRequest: String? = null
) : LinkMessage()

sealed class LinkResultMessage : LinkMessage() {
    object LinkSuccess : LinkResultMessage()
    data class LinkFailure(val exception: Exception) : LinkResultMessage()
}

