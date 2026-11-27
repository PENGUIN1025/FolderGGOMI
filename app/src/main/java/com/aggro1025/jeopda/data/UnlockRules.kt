package com.aggro1025.jeopda.data

import java.time.LocalDate

/**
 * 해제 규칙 (요구사항 4장). 안드로이드에 의존하지 않아 단위 테스트로 확인한다.
 */
object UnlockRules {
    /** 스마트폰 모드 최대 유지 시간(분). 출시 후 조정할 수 있게 한 곳에만 둔다. */
    const val MAX_MINUTES = 12 * 60

    /** 시간 입력 최소값과 분 단위 간격 */
    const val MIN_MINUTES = 5
    const val MINUTE_STEP = 5

    /** 사유 최소 글자 수 (공백 제외). 문서의 10글자에서 사용자 결정으로 6글자로 변경 */
    const val MIN_REASON_CHARS = 6

    /** 종료 전 알림 시점 (분). 문서의 "5분 전"에서 사용자 결정으로 30분 전·10분 전으로 변경 */
    val WARNING_MINUTES = listOf(30, 10)

    sealed interface ReasonCheck {
        data object Ok : ReasonCheck
        data class TooShort(val count: Int) : ReasonCheck
        data object Repetitive : ReasonCheck
    }

    /** 공백을 뺀 글자 수 (한글 조합 중인 자모도 한 글자로 센다) */
    fun reasonCharCount(reason: String): Int =
        reason.codePoints().filter { !Character.isWhitespace(it) }.count().toInt()

    fun checkReason(reason: String): ReasonCheck {
        val chars = reason.codePoints().filter { !Character.isWhitespace(it) }.toArray()
        if (chars.size < MIN_REASON_CHARS) return ReasonCheck.TooShort(chars.size)
        if (chars.distinct().size == 1) return ReasonCheck.Repetitive
        return ReasonCheck.Ok
    }

    fun isValidDuration(minutes: Int): Boolean = minutes in MIN_MINUTES..MAX_MINUTES

    /** v0.2a 임시 해제(광고 자리): 기다리는 시간. 광고를 불러오지 못했을 때도 같은 시간 */
    const val WAIT_SECONDS = 60

    /**
     * 하루 1회 무료: 마지막 사용일보다 오늘이 뒤일 때만 사용 가능.
     * (시계를 과거로 돌려도 다시 생기지 않게 "다른 날"이 아니라 "다음 날 이후"로 판단)
     */
    fun isFreeAvailable(lastFreeDate: String?, today: LocalDate): Boolean {
        val last = lastFreeDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return true
        return today.isAfter(last)
    }
}
