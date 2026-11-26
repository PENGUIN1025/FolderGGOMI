package com.aggro1025.jeopda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchTest {
    private val contacts = listOf(
        Contact("강민수", "010-2481-5530"),
        Contact("김보람", "010-7719-0042"),
        Contact("박서준", "010-9902-1187"),
        Contact("엄마", "010-1234-5678"),
        Contact("Tom Kim", "+82 10-3356-8120"),
    )

    @Test
    fun initials() {
        assertEquals("ㄱㅁㅅ", ContactSearch.initials("강민수"))
        assertEquals("ㅇㅁ", ContactSearch.initials("엄 마"))
        assertEquals("TOMKIM", ContactSearch.initials("Tom Kim"))
    }

    @Test
    fun keyMatchesChoseong() {
        assertTrue(ContactSearch.matchesName("강민수", "4"))
        assertTrue(ContactSearch.matchesName("강민수", "40")) // ㄱ, ㅁ
        assertTrue(ContactSearch.matchesName("김보람", "47")) // ㄱ, ㅂ
        assertFalse(ContactSearch.matchesName("박서준", "4"))
        assertTrue(ContactSearch.matchesName("박서준", "78")) // ㅂ, ㅅ
        assertTrue(ContactSearch.matchesName("엄마", "00")) // ㅇ, ㅁ
    }

    @Test
    fun keyMatchesLatin() {
        assertTrue(ContactSearch.matchesName("Tom Kim", "8"))
        assertTrue(ContactSearch.matchesName("Tom Kim", "866"))
    }

    @Test
    fun oneOrTwoKeysMatchNamesOnly() {
        assertEquals(listOf("강민수", "김보람"), ContactSearch.filter(contacts, "4").map { it.name })
    }

    @Test
    fun threeOrMoreKeysAlsoMatchNumbers() {
        // "567"은 이름으로는 아무도 안 맞고, 엄마 번호(1234-5678)에 들어 있음
        assertEquals(listOf("엄마"), ContactSearch.filter(contacts, "567").map { it.name })
    }

    @Test
    fun findByNumberIgnoresFormatting() {
        assertEquals("엄마", ContactSearch.findByNumber(contacts, "01012345678")?.name)
        assertEquals("Tom Kim", ContactSearch.findByNumber(contacts, "01033568120")?.name)
        assertNull(ContactSearch.findByNumber(contacts, "0101"))
    }

    @Test
    fun describeKeys() {
        assertEquals("ㄱㅋ ㅂㅍ", ContactSearch.describe("47"))
    }
}
