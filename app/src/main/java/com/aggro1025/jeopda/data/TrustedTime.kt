package com.aggro1025.jeopda.data

/**
 * 믿을 수 있는 현재 시각 (요구사항 7장 "시간 변경" 대응).
 * 1) 폰이 통신사·인터넷에서 받은 네트워크 시간
 * 2) 못 받으면: 같은 부팅 안에서는 기준점 + 부팅 후 경과 시간 (폰 시계를 돌려도 영향 없음)
 * 3) 재부팅 후 기준점이 없으면 폰 시계 (결정: 이때는 막지 않는다)
 * 안드로이드에 의존하지 않는 계산 부분이라 단위 테스트로 확인한다.
 */
object TrustedTime {
    data class Anchor(val wall: Long, val elapsed: Long, val boot: Int, val fromNetwork: Boolean)

    /** 기준점이 같은 부팅에서 만들어졌으면 경과 시간으로 계산, 아니면 폰 시계 */
    fun estimate(anchor: Anchor?, elapsedNow: Long, bootNow: Int, deviceNow: Long): Long =
        if (anchor != null && anchor.boot == bootNow && elapsedNow >= anchor.elapsed) {
            anchor.wall + (elapsedNow - anchor.elapsed)
        } else {
            deviceNow
        }

    /**
     * 새 기준점을 저장할지: 네트워크 시간이면 항상, 아니면 이번 부팅의 기준점이 아직 없을 때만
     * (부팅 뒤 처음 본 폰 시각을 기준으로 삼아, 그 뒤에 시계를 돌리는 것을 무시)
     */
    fun nextAnchor(current: Anchor?, network: Long?, deviceNow: Long, elapsedNow: Long, bootNow: Int): Anchor? = when {
        network != null -> Anchor(network, elapsedNow, bootNow, fromNetwork = true)
        current == null || current.boot != bootNow -> Anchor(deviceNow, elapsedNow, bootNow, fromNetwork = false)
        else -> null
    }
}
