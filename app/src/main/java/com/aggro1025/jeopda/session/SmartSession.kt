package com.aggro1025.jeopda.session

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aggro1025.jeopda.MainActivity
import com.aggro1025.jeopda.R
import com.aggro1025.jeopda.SettingsActivity
import com.aggro1025.jeopda.data.AppPrefs
import com.aggro1025.jeopda.data.Clock
import com.aggro1025.jeopda.data.UnlockRules
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

enum class UnlockMethod(val label: String) {
    FREE("무료"),
    WAIT("1분 대기"),
    AD_WAIT("광고 실패·1분 대기"),
    AD("광고 보며 멈추기"),
    PAY("결제"),
    DEV("개발용"),
}

/**
 * 스마트폰 모드 세션 관리 (요구사항 2장, 4장).
 * 모드 판단은 항상 저장된 종료 시각과 현재 시각을 비교해서 한다.
 * 알람은 알림용일 뿐이라, 알람이 늦거나 사라져도 홈 버튼을 누르는 순간 정확히 복귀한다.
 */
object SmartSession {
    const val CHANNEL_ONGOING = "session"
    const val CHANNEL_ALERT = "alert"
    private const val NOTIF_ONGOING = 1
    private const val NOTIF_ALERT = 2

    /** 종료 N분 전 알림. 실제 액션 이름은 "...WARN_30", "...WARN_10" */
    private const val ACTION_WARN_PREFIX = "com.aggro1025.jeopda.WARN_"

    fun isWarnAction(action: String?) = action?.startsWith(ACTION_WARN_PREFIX) == true
    const val ACTION_EXPIRE = "com.aggro1025.jeopda.EXPIRE"

    /**
     * 남은 시간(ms). 같은 부팅 안에서는 부팅 후 경과 시간으로 계산해서 폰 시계를 돌려도 바뀌지 않는다.
     * 재부팅 뒤에는 믿을 수 있는 시각(Clock.now) 기준으로 다시 계산한다.
     */
    fun remainingMs(context: Context): Long {
        val prefs = AppPrefs(context)
        if (prefs.smartEndAt == 0L) return 0L
        return if (prefs.smartBoot == Clock.bootCount(context)) {
            prefs.smartEndElapsed - Clock.elapsed()
        } else {
            prefs.smartEndAt - Clock.now(context)
        }.coerceAtLeast(0L)
    }

    fun isSmart(context: Context): Boolean = remainingMs(context) > 0

    /** 화면 표시용 종료 시각 (지금 폰 시계 + 남은 시간) */
    fun endAt(context: Context): Long = System.currentTimeMillis() + remainingMs(context)

    fun start(context: Context, minutes: Int, reason: String, method: UnlockMethod) {
        require(UnlockRules.isValidDuration(minutes))
        val prefs = AppPrefs(context)
        val now = Clock.now(context)
        val duration = minutes * 60_000L
        prefs.smartEndAt = now + duration
        prefs.smartEndElapsed = Clock.elapsed() + duration
        prefs.smartBoot = Clock.bootCount(context)
        // 무료 1회는 해제가 완료되는 이 시점에만 소모한다 (4장 공통 규칙 6). 날짜는 믿을 수 있는 시각 기준
        if (method == UnlockMethod.FREE) {
            val day = today(context).toString()
            prefs.freeUsedCount = if (prefs.lastFreeDate == day) prefs.freeUsedCount + 1 else 1
            prefs.lastFreeDate = day
        }
        prefs.addReason(AppPrefs.ReasonEntry(now, minutes, method.name, reason.trim()))
        nm(context).cancel(NOTIF_ALERT)
        scheduleAlarms(context)
        showOngoing(context)
    }

    /** 조기 복귀(early=true) 또는 만료. 남은 시간은 소멸한다 */
    fun end(context: Context, early: Boolean = true) {
        val prefs = AppPrefs(context)
        if (prefs.smartEndAt != 0L) {
            val now = Clock.now(context)
            prefs.closeLastReason { e -> if (early) now else e.plannedEnd }
        }
        prefs.smartEndAt = 0L
        prefs.smartEndElapsed = 0L
        cancelAlarms(context)
        nm(context).cancel(NOTIF_ONGOING)
        // 폴더폰 모드로 돌아왔으니 허용하지 않은 알림을 정리한다
        NotificationBlocker.sweepNow()
    }

    /** 앱 시작·재부팅·홈 진입 때 저장된 상태와 알람/알림을 맞춘다 */
    fun sync(context: Context) {
        val prefs = AppPrefs(context)
        if (prefs.smartEndAt == 0L) return
        val remaining = remainingMs(context)
        if (remaining <= 0L) {
            end(context, early = false)
            return
        }
        // 재부팅 뒤에는 경과 시간 기준을 새로 잡는다
        val boot = Clock.bootCount(context)
        if (prefs.smartBoot != boot) {
            prefs.smartEndElapsed = Clock.elapsed() + remaining
            prefs.smartBoot = boot
        }
        scheduleAlarms(context)
        showOngoing(context)
    }

    fun today(context: Context): LocalDate =
        Instant.ofEpochMilli(Clock.now(context)).atZone(ZoneId.systemDefault()).toLocalDate()

    fun freeRemainingToday(context: Context): Int {
        val prefs = AppPrefs(context)
        return UnlockRules.freeRemaining(prefs.lastFreeDate, prefs.freeUsedCount, today(context))
    }

    fun isFreeAvailableToday(context: Context): Boolean = freeRemainingToday(context) > 0

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am(context).canScheduleExactAlarms()

    // ---- 알람 (부팅 후 경과 시간 기준이라 시계를 바꿔도 제때 울린다) ----

    private fun scheduleAlarms(context: Context) {
        val remaining = remainingMs(context)
        val nowElapsed = Clock.elapsed()
        // 이미 지난 시점의 알림은 건너뛴다 (예: 15분만 쓰면 10분 전 알림만)
        UnlockRules.WARNING_MINUTES.forEach { m ->
            val warnIn = remaining - m * 60_000L
            val pi = alarmIntent(context, ACTION_WARN_PREFIX + m)
            if (warnIn > 0) setAlarm(context, nowElapsed + warnIn, pi) else am(context).cancel(pi)
        }
        setAlarm(context, nowElapsed + remaining, alarmIntent(context, ACTION_EXPIRE))
    }

    private fun setAlarm(context: Context, atElapsed: Long, pi: PendingIntent) {
        val am = am(context)
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi)
        } else {
            // "알람 및 리마인더" 권한이 꺼져 있으면 몇 분 늦을 수 있는 알람으로 대체
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi)
        }
    }

    private fun cancelAlarms(context: Context) {
        UnlockRules.WARNING_MINUTES.forEach { am(context).cancel(alarmIntent(context, ACTION_WARN_PREFIX + it)) }
        am(context).cancel(alarmIntent(context, ACTION_EXPIRE))
    }

    private fun alarmIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, SessionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    // ---- 알림 ----

    fun createChannels(context: Context) {
        val nm = nm(context)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "스마트폰 모드 남은 시간", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "스마트폰 모드 종료 알림", NotificationManager.IMPORTANCE_HIGH),
        )
    }

    private fun showOngoing(context: Context) {
        val endAt = endAt(context)
        val n = Notification.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_jeopda)
            .setContentTitle("스마트폰 모드")
            .setContentText("${formatTime(endAt)}에 폴더폰으로 돌아가요")
            .setWhen(endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(settingsIntent(context))
            .addAction(returnAction(context))
            .build()
        nm(context).notify(NOTIF_ONGOING, n)
    }

    fun showWarning(context: Context) {
        if (!isSmart(context)) return
        // 남은 시간 알림을 밀어서 지웠더라도 이때 다시 띄운다
        showOngoing(context)
        val left = ((remainingMs(context) + 30_000L) / 60_000L).coerceAtLeast(1)
        val n = Notification.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_jeopda)
            .setContentTitle("${left}분 후 폴더폰으로 돌아가요")
            .setContentText("${formatTime(endAt(context))}에 스마트폰 모드가 끝나요")
            .setContentIntent(settingsIntent(context))
            .addAction(returnAction(context))
            .setAutoCancel(true)
            .build()
        nm(context).notify(NOTIF_ALERT, n)
    }

    fun showExpired(context: Context) {
        val n = Notification.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_jeopda)
            .setContentTitle("스마트폰 모드가 끝났어요")
            .setContentText("홈 버튼을 누르면 폴더폰 화면으로 돌아가요")
            .setContentIntent(homeIntent(context, returnToFolder = false))
            .setAutoCancel(true)
            .build()
        nm(context).notify(NOTIF_ALERT, n)
    }

    private fun returnAction(context: Context): Notification.Action =
        Notification.Action.Builder(null, "폴더폰으로 돌아가기", homeIntent(context, returnToFolder = true)).build()

    private fun homeIntent(context: Context, returnToFolder: Boolean): PendingIntent =
        PendingIntent.getActivity(
            context,
            if (returnToFolder) 1 else 2,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_RETURN_TO_FOLDER, returnToFolder),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun settingsIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            3,
            Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun formatTime(millis: Long): String = SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(millis))

    private fun nm(context: Context) = context.getSystemService(NotificationManager::class.java)
    private fun am(context: Context) = context.getSystemService(AlarmManager::class.java)
}
