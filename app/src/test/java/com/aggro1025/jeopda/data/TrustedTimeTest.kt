package com.aggro1025.jeopda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrustedTimeTest {
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun clockMovedForwardInSameBootIsIgnored() {
        val anchor = TrustedTime.Anchor(wall = 1_000_000L, elapsed = 5_000L, boot = 7, fromNetwork = false)
        // 10초 뒤, 폰 시계는 하루 앞으로 돌림
        val now = TrustedTime.estimate(anchor, elapsedNow = 15_000L, bootNow = 7, deviceNow = 1_010_000L + day)
        assertEquals(1_010_000L, now)
    }

    @Test
    fun afterRebootUsesDeviceClock() {
        val anchor = TrustedTime.Anchor(1_000_000L, 5_000L, boot = 7, fromNetwork = true)
        assertEquals(42L, TrustedTime.estimate(anchor, elapsedNow = 100L, bootNow = 8, deviceNow = 42L))
    }

    @Test
    fun networkTimeAlwaysBecomesAnchor() {
        val current = TrustedTime.Anchor(1L, 1L, boot = 7, fromNetwork = false)
        val next = TrustedTime.nextAnchor(current, network = 500L, deviceNow = 999L, elapsedNow = 10L, bootNow = 7)
        assertEquals(TrustedTime.Anchor(500L, 10L, 7, true), next)
    }

    @Test
    fun deviceTimeAnchorsOnlyOncePerBoot() {
        val current = TrustedTime.Anchor(1L, 1L, boot = 7, fromNetwork = false)
        assertNull(TrustedTime.nextAnchor(current, network = null, deviceNow = 999L, elapsedNow = 10L, bootNow = 7))
        assertNotNull(TrustedTime.nextAnchor(current, network = null, deviceNow = 999L, elapsedNow = 10L, bootNow = 8))
    }

    @Test
    fun freeUnlockDoesNotComeBackWhenClockGoesBackward() {
        assertFalse(UnlockRules.isFreeAvailable("2026-11-27", LocalDate.of(2026, 11, 26)))
        assertTrue(UnlockRules.isFreeAvailable("2026-11-27", LocalDate.of(2026, 11, 28)))
    }
}
