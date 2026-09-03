package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class SubjectInfo(
    val id: Int,
    val name: String,
    val professor: String,
    val discussions: List<DiscussionInfo> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if(other is SubjectInfo) {
            return id == other.id
        }
        return false
    }
}

