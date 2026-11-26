package com.aggro1025.jeopda.data

data class Contact(val name: String, val number: String)

/**
 * 숫자키로 연락처 찾기. 숫자키의 천지인 자음(4 → ㄱㅋㄲ)과 영문(4 → GHI)으로 이름 첫 글자들을 맞춘다.
 * 예: "47" → 첫 글자 초성이 ㄱ/ㅋ/ㄲ, 둘째 글자 초성이 ㅂ/ㅍ/ㅃ인 이름 (김보람, 강필수 …)
 * 숫자를 3개 이상 누르면 전화번호에 그 숫자가 들어 있는 연락처도 찾는다 (1~2개는 너무 많이 걸려서 제외).
 */
object ContactSearch {
    private val KEY_CHARS: Map<Char, String> = mapOf(
        '1' to "",
        '2' to "ABC",
        '3' to "DEF",
        '4' to "ㄱㅋㄲGHI",
        '5' to "ㄴㄹJKL",
        '6' to "ㄷㅌㄸMNO",
        '7' to "ㅂㅍㅃPQRS",
        '8' to "ㅅㅎㅆTUV",
        '9' to "ㅈㅊㅉWXYZ",
        '0' to "ㅇㅁ",
    )

    /** 화면 표시용: 누른 키의 자음 묶음 (예: "47" → "ㄱㅋ ㅂㅍ") */
    fun describe(keys: String): String = keys.map { k ->
        KEY_CHARS[k].orEmpty().filter { it in 'ㄱ'..'ㅎ' && it !in "ㄲㄸㅃㅆㅉ" }.ifEmpty { k.toString() }
    }.joinToString(" ")

    private const val CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

    /** 이름의 글자별 첫소리 (한글은 초성, 영문은 대문자). 공백·기호는 건너뛴다 */
    fun initials(name: String): String = buildString {
        for (c in name) {
            when {
                c in '가'..'힣' -> append(CHOSEONG[(c - '가') / 588])
                c in 'ㄱ'..'ㅎ' -> append(c)
                c.isLetter() -> append(c.uppercaseChar())
                c.isDigit() -> append(c)
            }
        }
    }

    fun matchesName(name: String, keys: String): Boolean {
        val ini = initials(name)
        if (keys.length > ini.length) return false
        return keys.withIndex().all { (i, k) ->
            val ch = ini[i]
            ch == k || KEY_CHARS[k].orEmpty().contains(ch)
        }
    }

    fun digits(number: String): String = number.filter { it.isDigit() }

    private const val MIN_NUMBER_KEYS = 3

    /** 이름이 맞는 연락처를 먼저, 그다음 번호에 숫자가 들어 있는 연락처 */
    fun filter(contacts: List<Contact>, keys: String): List<Contact> {
        if (keys.isEmpty()) return contacts
        val byName = contacts.filter { matchesName(it.name, keys) }
        if (keys.length < MIN_NUMBER_KEYS) return byName
        val byNumber = contacts.filter { it !in byName && digits(it.number).contains(keys) }
        return byName + byNumber
    }

    /** 다이얼러에서 누른 번호와 같은 연락처 (국가번호·하이픈 차이는 뒤 8자리로 비교) */
    fun findByNumber(contacts: List<Contact>, number: String): Contact? {
        val d = digits(number)
        if (d.length < 7) return null
        return contacts.firstOrNull { digits(it.number) == d }
            ?: contacts.firstOrNull { digits(it.number).takeLast(8) == d.takeLast(8) }
    }
}
