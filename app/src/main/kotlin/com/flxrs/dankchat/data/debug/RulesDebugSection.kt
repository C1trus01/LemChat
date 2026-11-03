package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.repo.HighlightsRepository
import com.flxrs.dankchat.data.repo.IgnoresRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single

@Single
class RulesDebugSection(
    private val highlightsRepository: HighlightsRepository,
    private val ignoresRepository: IgnoresRepository,
) : DebugSection {
    override val order = 8
    override val baseTitle = "Rules"

    override fun entries(): Flow<DebugSectionSnapshot> = combine(
        highlightsRepository.messageHighlights,
        highlightsRepository.userHighlights,
        highlightsRepository.badgeHighlights,
        highlightsRepository.blacklistedUsers,
        ignoresRepository.messageIgnores,
    ) { msgHighlights, userHighlights, badgeHighlights, blacklisted, msgIgnores ->
        DebugSectionSnapshot(
            title = baseTitle,
            entries =
                listOf(
                    DebugEntry("Message highlights", "${msgHighlights.size}"),
                    DebugEntry("User highlights", "${userHighlights.size}"),
                    DebugEntry("Badge highlights", "${badgeHighlights.size}"),
                    DebugEntry("Blacklisted users", "${blacklisted.size}"),
                    DebugEntry("Message ignores", "${msgIgnores.size}"),
                ),
        )
    }
}
