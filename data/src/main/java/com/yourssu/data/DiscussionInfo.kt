package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class DiscussionAttachment(
    val id: Int = 0,
    val name: String = "",
    val url: String = "",
)

@Serializable
data class DiscussionInfo(
    val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val url: String = "",
    val published: Boolean = true,
    val readState: String = "",
    val createdAt: String = "",
    val author: String = "",
    val attachments: List<DiscussionAttachment> = emptyList(),
)
