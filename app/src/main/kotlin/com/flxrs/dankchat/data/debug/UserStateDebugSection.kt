package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class UserStateDebugSection(
    private val userStateRepository: UserStateRepository,
) : DebugSection {
    override val order = 7
    override val baseTitle = "User State"

    override fun entries(): Flow<DebugSectionSnapshot> = userStateRepository.userState.map { state ->
        DebugSectionSnapshot(
            title = baseTitle,
            entries =
                listOf(
                    DebugEntry("Mod in", state.moderationChannels.joinToString().ifEmpty { "None" }),
                    DebugEntry("VIP in", state.vipChannels.joinToString().ifEmpty { "None" }),
                ),
        )
    }
}
