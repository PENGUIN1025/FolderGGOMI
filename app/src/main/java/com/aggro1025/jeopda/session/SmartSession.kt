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
import com.aggro1025.jeopda.data.UnlockRules
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

enum class UnlockMethod(val label: String) {
    FREE("무료"),
    AD("광고"),
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

    const val ACTION_WARN = "com.aggro1025.jeopda.WARN"
    const val ACTION_EXPIRE = "com.aggro1025.jeopda.EXPIRE"

    fun isSmart(context: Context, now: Long = System.currentTimeMillis()): Boolean =
        AppPrefs(context).smartEndAt > now

    fun endAt(context: Context): Long = AppPrefs(context).smartEndAt

    fun start(context: Context, minutes: Int, reason: String, method: UnlockMethod) {
        require(UnlockRules.isValidDuration(minutes))
        val prefs = AppPrefs(context)
        val now = System.currentTimeMillis()
        prefs.smartEndAt = now + minutes * 60_000L
        // 무료 1회는 해제가 완료되는 이 시점에만 소모한다 (4장 공통 규칙 6)
        if (method == UnlockMethod.FREE) prefs.lastFreeDate = LocalDate.now().toString()
        prefs.addReason(AppPrefs.ReasonEntry(now, minutes, method.name, reason.trim()))
        nm(context).cancel(NOTIF_ALERT)
        scheduleAlarms(context)
        showOngoing(context)
    }

    /** 조기 복귀 또는 만료. 남은 시간은 소멸한다 */
    fun end(context: Context) {
        AppPrefs(context).smartEndAt = 0L
        cancelAlarms(context)
        nm(context).cancel(NOTIF_ONGOING)
    }

    /** 앱 시작·재부팅·홈 진입 때 저장된 상태와 알람/알림을 맞춘다 */
    fun sync(context: Context) {
        val endAt = endAt(context)
        when {
            endAt == 0L -> Unit
            endAt <= System.currentTimeMillis() -> end(context)
            else -> {
                scheduleAlarms(context)
                showOngoing(context)
            }
        }
    }

    fun isFreeAvailableToday(context: Context): Boolean =
        UnlockRules.isFreeAvailable(AppPrefs(context).lastFreeDate, LocalDate.now())

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am(context).canScheduleExactAlarms()

    // ---- 알람 ----

    private fun scheduleAlarms(context: Context) {
        val endAt = endAt(context)
        val warnAt = endAt - UnlockRules.WARNING_BEFORE_MILLIS
        if (warnAt > System.currentTimeMillis()) setAlarm(context, warnAt, alarmIntent(context, ACTION_WARN))
        else am(context).cancel(alarmIntent(context, ACTION_WARN))
        setAlarm(context, endAt, alarmIntent(context, ACTION_EXPIRE))
    }

    private fun setAlarm(context: Context, at: Long, pi: PendingIntent) {
        val am = am(context)
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            // "알람 및 리마인더" 권한이 꺼져 있으면 몇 분 늦을 수 있는 알람으로 대체
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarms(context: Context) {
        am(context).cancel(alarmIntent(context, ACTION_WARN))
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
        val n = Notification.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_jeopda)
            .setContentTitle("5분 후 폴더폰으로 돌아가요")
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
