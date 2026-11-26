package com.aggro1025.jeopda.data

import com.aggro1025.jeopda.data.UnlockRules.ReasonCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UnlockRulesTest {
    @Test
    fun reasonUnderTenCharsIsRejected() {
        assertEquals(ReasonCheck.TooShort(7), UnlockRules.checkReason("은행 송금 해야함"))
    }

    @Test
    fun spacesDoNotCount() {
        assertEquals(ReasonCheck.TooShort(5), UnlockRules.checkReason("가 나 다 라 마          "))
    }

    @Test
    fun tenCharsIsAccepted() {
        assertEquals(ReasonCheck.Ok, UnlockRules.checkReason("회사 메일 확인해야 해요"))
    }

    @Test
    fun repeatedSingleCharIsRejected() {
        assertEquals(ReasonCheck.Repetitive, UnlockRules.checkReason("ㅋㅋㅋㅋㅋ ㅋㅋㅋㅋㅋㅋ"))
        assertEquals(ReasonCheck.Repetitive, UnlockRules.checkReason("aaaaaaaaaaaa"))
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
        assertTrue(UnlockRules.isFreeAvailable(null, today))
        assertFalse(UnlockRules.isFreeAvailable("2026-11-26", today))
        assertTrue(UnlockRules.isFreeAvailable("2026-11-25", today))
    }
}
