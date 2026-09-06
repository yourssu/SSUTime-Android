package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class AttachmentInfo(
    val id: Int,
    val uuid: String,
    val folder_id: String,
    val display_name: String,
    val file_name: String? = "",
    val content_type: String? = "",
    val url: String,
    val size: Long,
    val thumbnail_url: String? = "",
    val created_at: String,
    val updated_at: String,
    val modified_at: String,
    val mime_class: String
)
