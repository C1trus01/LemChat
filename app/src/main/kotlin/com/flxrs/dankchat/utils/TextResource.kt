package com.flxrs.dankchat.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed interface TextResource {
    @Immutable
    data class Plain(
        val value: String,
    ) : TextResource

    @Immutable
    data class Res(
        @param:StringRes val id: Int,
        val args: ImmutableList<Any> = persistentListOf(),
    ) : TextResource

    @Immutable
    data class PluralRes(
        @param:PluralsRes val id: Int,
        val quantity: Int,
        val args: ImmutableList<Any> = persistentListOf(),
    ) : TextResource
}

@Composable
fun TextResource.resolve(): String = when (this) {
    is TextResource.Plain -> {
        value
    }

    is TextResource.Res -> {
        val resolvedArgs =
            args.map { arg ->
                when (arg) {
                    is TextResource -> arg.resolve()
                    else -> arg
                }
            }
        stringResource(id, *resolvedArgs.toTypedArray())
    }

    is TextResource.PluralRes -> {
        val resolvedArgs =
            args.map { arg ->
                when (arg) {
                    is TextResource -> arg.resolve()
                    else -> arg
                }
            }
        pluralStringResource(id, quantity, *resolvedArgs.toTypedArray())
    }
}
