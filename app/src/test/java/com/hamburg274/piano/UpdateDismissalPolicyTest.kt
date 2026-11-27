package com.hamburg274.piano

import com.hamburg274.piano.update.UpdateDismissalPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDismissalPolicyTest {

    @Test
    fun showsPromptWhenNeverDismissed() {
        val show = UpdateDismissalPolicy.shouldShowPrompt(
            availableVersionCode = 3,
            isDownloaded = false,
            manualCheck = false,
            dismissedVersionCode = -1,
            dismissedAtMillis = 0L,
            nowMillis = 10_000L
        )
        assertTrue(show)
    }

    @Test
    fun suppressesSameVersionWithin24HourCooldown() {
        val base = 1_000_000_000L
        val twelveHoursLater = base + (12L * 60L * 60L * 1000L)
        val show = UpdateDismissalPolicy.shouldShowPrompt(
            availableVersionCode = 3,
            isDownloaded = false,
            manualCheck = false,
            dismissedVersionCode = 3,
            dismissedAtMillis = base,
            nowMillis = twelveHoursLater
        )
        assertFalse(show)
    }

    @Test
    fun showsAgainAfter24HoursPass() {
        val base = 1_000_000_000L
        val twentyFiveHoursLater = base + (25L * 60L * 60L * 1000L)
        val show = UpdateDismissalPolicy.shouldShowPrompt(
            availableVersionCode = 3,
            isDownloaded = false,
            manualCheck = false,
            dismissedVersionCode = 3,
            dismissedAtMillis = base,
            nowMillis = twentyFiveHoursLater
        )
        assertTrue(show)
    }

    @Test
    fun bypassesCooldownForNewerVersionOrDownloadedOrManualCheck() {
        val base = 1_000_000_000L
        val oneMinuteLater = base + 60_000L

        assertTrue(
            "Newer versionCode must bypass cooldown",
            UpdateDismissalPolicy.shouldShowPrompt(4, false, false, 3, base, oneMinuteLater)
        )
        assertTrue(
            "Already-downloaded update must bypass cooldown",
            UpdateDismissalPolicy.shouldShowPrompt(3, true, false, 3, base, oneMinuteLater)
        )
        assertTrue(
            "Manual check must bypass cooldown",
            UpdateDismissalPolicy.shouldShowPrompt(3, false, true, 3, base, oneMinuteLater)
        )
    }
}
