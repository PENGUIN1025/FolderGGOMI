package com.aggro1025.jeopda.data

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings

/** TrustedTime의 안드로이드 쪽: 네트워크 시간 읽기, 부팅 번호, 기준점 저장 */
object Clock {
    fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

    fun elapsed(): Long = SystemClock.elapsedRealtime()

    /** 폰이 받아 둔 네트워크 시간 (Android 13+). 없으면 null */
    private fun networkTime(): Long? {
        if (Build.VERSION.SDK_INT < 33) return null
        return try {
            SystemClock.currentNetworkTimeClock().millis()
        } catch (e: Exception) {
            null // 아직 네트워크 시간을 못 받음 (비행기 모드, 재부팅 직후 등)
        }
    }

    /** 믿을 수 있는 현재 시각 (ms) */
    fun now(context: Context): Long {
        val prefs = AppPrefs(context)
        val boot = bootCount(context)
        val elapsedNow = elapsed()
        val deviceNow = System.currentTimeMillis()
        val current = prefs.clockAnchor
        val next = TrustedTime.nextAnchor(current, networkTime(), deviceNow, elapsedNow, boot)
        if (next != null) prefs.clockAnchor = next
        return TrustedTime.estimate(next ?: current, elapsedNow, boot, deviceNow)
    }
}
