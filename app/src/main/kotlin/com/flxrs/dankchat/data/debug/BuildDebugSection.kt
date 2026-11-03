package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.annotation.Single

@Single
class BuildDebugSection : DebugSection {
    override val order = 0
    override val baseTitle = "Build"

    override fun entries(): Flow<DebugSectionSnapshot> = flowOf(
        DebugSectionSnapshot(
            title = baseTitle,
            entries =
                listOf(
                    DebugEntry("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
                    DebugEntry("Build type", BuildConfig.BUILD_TYPE),
                ),
        ),
    )
}
