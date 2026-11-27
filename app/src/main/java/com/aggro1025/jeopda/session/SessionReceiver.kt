package com.aggro1025.jeopda.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 종료 30분 전·10분 전 알림과 만료 처리 */
class SessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (SmartSession.isWarnAction(intent.action)) {
            SmartSession.showWarning(context)
            return
        }
        when (intent.action) {
            SmartSession.ACTION_EXPIRE -> {
                val wasSmart = com.aggro1025.jeopda.data.AppPrefs(context).smartEndAt != 0L
                if (SmartSession.isSmart(context)) {
                    // 부정확한 알람이 일찍 온 경우: 다시 맞춘다
                    SmartSession.sync(context)
                } else if (wasSmart) {
                    SmartSession.end(context, early = false)
                    SmartSession.showExpired(context)
                }
            }
        }
    }
}

/** 재부팅, 앱 업데이트, 시계 변경 후 저장된 모드와 남은 시간을 복구한다 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in HANDLED) SmartSession.sync(context)
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.TIME_SET",
        )
    }
}
