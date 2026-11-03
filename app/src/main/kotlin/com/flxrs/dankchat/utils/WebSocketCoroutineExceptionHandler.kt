package com.flxrs.dankchat.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler

private val logger = KotlinLogging.logger("WebSocket")

fun webSocketCoroutineExceptionHandler(tag: String): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    logger.warn(throwable) { "[$tag] unhandled websocket coroutine exception" }
}
