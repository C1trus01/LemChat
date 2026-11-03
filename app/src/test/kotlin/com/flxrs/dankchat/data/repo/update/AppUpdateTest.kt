package com.flxrs.dankchat.data.repo.update

import com.flxrs.dankchat.data.api.github.dto.GitHubAssetDto
import com.flxrs.dankchat.data.api.github.dto.GitHubReleaseDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AppUpdateTest {
    private val apk = GitHubAssetDto(name = "app.apk", downloadUrl = "https://example.com/app.apk", size = 10)

    @Test
    fun `newer versions are recognized by their numbers and not their text`() {
        assertTrue(isNewerVersion("4.0.48", "4.0.47"))
        assertTrue(isNewerVersion("4.0.100", "4.0.47"))
        assertTrue(isNewerVersion("4.1", "4.0.47"))
        assertTrue(isNewerVersion("v5.0.0", "4.0.47"))
        assertFalse(isNewerVersion("4.0.47", "4.0.47"))
        assertFalse(isNewerVersion("4.0.47.0", "4.0.47"))
        assertFalse(isNewerVersion("4.0.46", "4.0.47"))
    }

    @Test
    fun `a version that can't be read is not newer`() {
        assertFalse(isNewerVersion("latest", "4.0.47"))
        assertFalse(isNewerVersion("4.x.1", "4.0.47"))
        assertFalse(isNewerVersion("", "4.0.47"))
    }

    @Test
    fun `a newer release offers its apk`() {
        val release = GitHubReleaseDto(tagName = "v4.1.0", body = "  Notes\n", assets = listOf(GitHubAssetDto("mapping.txt", "https://example.com/m", 1), apk))

        assertEquals(AppUpdate(version = "4.1.0", notes = "Notes", apkUrl = apk.downloadUrl, apkSize = 10), release.toAppUpdate("4.0.47"))
    }

    @Test
    fun `no update without a newer version or something to install`() {
        assertNull(GitHubReleaseDto(tagName = "v4.0.47", assets = listOf(apk)).toAppUpdate("4.0.47"))
        assertNull(GitHubReleaseDto(tagName = "v4.1.0").toAppUpdate("4.0.47"))
    }

    @Test
    fun `an apk that is not on https is ignored`() {
        val release = GitHubReleaseDto(tagName = "v4.1.0", assets = listOf(apk.copy(downloadUrl = "http://example.com/app.apk")))

        assertNull(release.toAppUpdate("4.0.47"))
    }
}
