package com.mh.chat

/** A single chat message. role is "user", "assistant" or "system". */
data class ChatMessage(
    val role: String,
    val content: String,
    val thinking: Boolean = false
)
