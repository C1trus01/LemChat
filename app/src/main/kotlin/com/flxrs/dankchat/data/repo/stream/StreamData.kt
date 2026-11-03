package com.flxrs.dankchat.data.repo.stream

import com.flxrs.dankchat.data.UserName

data class StreamData(
    val channel: UserName,
    val formattedData: String,
    val formattedDataCompact: String,
    val viewerCount: Int = 0,
    val startedAt: String = "",
    val category: String? = null,
    val title: String? = null,
)
