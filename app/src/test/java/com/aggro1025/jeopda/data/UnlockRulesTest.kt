package com.aggro1025.jeopda.data

import com.aggro1025.jeopda.data.UnlockRules.ReasonCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UnlockRulesTest {
    @Test
    fun reasonUnderSixCharsIsRejected() {
        assertEquals(ReasonCheck.TooShort(5), UnlockRules.checkReason("은행 송금함"))
    }

    @Test
    fun spacesDoNotCount() {
        assertEquals(ReasonCheck.TooShort(5), UnlockRules.checkReason("가 나 다 라 마          "))
        assertEquals(ReasonCheck.Ok, UnlockRules.checkReason("가 나 다 라 마 바"))
    }

    @Test
    fun sixCharsIsAccepted() {
        assertEquals(ReasonCheck.Ok, UnlockRules.checkReason("은행 송금 해야함"))
    }

    @Test
    fun repeatedSingleCharIsRejected() {
        assertEquals(ReasonCheck.Repetitive, UnlockRules.checkReason("ㅋㅋㅋ ㅋㅋㅋㅋ"))
        assertEquals(ReasonCheck.Repetitive, UnlockRules.checkReason("aaaaaaa"))
    }

    @Test
    fun durationLimits() {
        assertFalse(UnlockRules.isValidDuration(0))
        assertFalse(UnlockRules.isValidDuration(4))
        assertTrue(UnlockRules.isValidDuration(5))
        assertTrue(UnlockRules.isValidDuration(12 * 60))
        assertFalse(UnlockRules.isValidDuration(12 * 60 + 1))
    }

    @Test
    fun freeUnlockResetsAtLocalMidnight() {
        val today = LocalDate.of(2026, 11, 26)
        assertEquals(3, UnlockRules.freeRemaining(null, 0, today))
        assertEquals(2, UnlockRules.freeRemaining("2026-11-26", 1, today))
        assertEquals(0, UnlockRules.freeRemaining("2026-11-26", 3, today))
        assertFalse(UnlockRules.isFreeAvailable("2026-11-26", 3, today))
        assertEquals(3, UnlockRules.freeRemaining("2026-11-25", 3, today))
    }
}
