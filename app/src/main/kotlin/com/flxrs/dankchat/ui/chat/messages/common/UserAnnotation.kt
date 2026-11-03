package com.flxrs.dankchat.ui.chat.messages.common

data class UserAnnotation(
    val userId: String?,
    val userName: String,
    val displayName: String,
    val channel: String?,
)

fun parseUserAnnotation(annotation: String): UserAnnotation? {
    val parts = annotation.split("|")
    return when (parts.size) {
        4 -> {
            UserAnnotation(
                userId = parts[0].takeIf { it.isNotEmpty() },
                userName = parts[1],
                displayName = parts[2],
                channel = parts[3],
            )
        }

        3 -> {
            UserAnnotation(
                userId = parts[0].takeIf { it.isNotEmpty() },
                userName = parts[1],
                displayName = parts[2],
                channel = null,
            )
        }

        else -> {
            null
        }
    }
}
