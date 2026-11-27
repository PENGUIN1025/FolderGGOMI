package com.aggro1025.jeopda.session

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.aggro1025.jeopda.R
import com.aggro1025.jeopda.SettingsActivity
import com.aggro1025.jeopda.apps.AppPolicy
import com.aggro1025.jeopda.apps.Apps
import com.aggro1025.jeopda.data.AppPrefs

/**
 * 폴더폰 모드에서 허용하지 않은 앱의 알림을 바로 지운다 (요구사항 5장 "알림 처리 방식").
 * 알림 내용은 읽거나 저장하지 않고, 어느 앱의 알림인지(패키지)와 종류만 본다.
 * 진행 중 알림(음악 재생, 길안내 등)은 안드로이드가 지우지 못하게 해서 그대로 둔다.
 */
class NotificationBlocker : NotificationListenerService() {

    private var base: Set<String> = emptySet()

    override fun onListenerConnected() {
        instance = this
        base = AppPolicy.baseAllowedPackages(this)
        sweep()
        checkHomeRole()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (shouldBlock(sbn)) cancelNotification(sbn.key)
        checkHomeRole()
    }

    /** 지금 떠 있는 알림 중 허용하지 않은 것을 정리 (폴더폰 모드로 돌아올 때) */
    fun sweep() {
        base = AppPolicy.baseAllowedPackages(this)
        val active = try {
            activeNotifications
        } catch (e: SecurityException) {
            null
        } ?: return
        active.filter { shouldBlock(it) }.forEach { cancelNotification(it.key) }
    }

    private fun shouldBlock(sbn: StatusBarNotification): Boolean {
        if (!isFolderMode(this)) return false
        if (!sbn.isClearable) return false // 진행 중 알림은 지울 수 없음
        if (sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false
        val prefs = AppPrefs(this)
        return !AppPolicy.isNotificationAllowed(sbn.packageName, sbn.notification.category, base, prefs.allowedNotifApps)
    }

    /** 홈 앱 해제 감지 (B2): 폴더폰 모드인데 접다가 기본 홈 앱이 아니면 한 시간에 한 번 알린다 */
    private fun checkHomeRole() {
        val prefs = AppPrefs(this)
        if (!prefs.setupDone || Apps.isDefaultHome(this)) return
        val now = System.currentTimeMillis()
        if (now - prefs.lastHomeWarnAt < HOME_WARN_INTERVAL) return
        prefs.lastHomeWarnAt = now
        notifyHomeLost(this)
    }

    companion object {
        @Volatile
        private var instance: NotificationBlocker? = null
        private const val HOME_WARN_INTERVAL = 60 * 60 * 1000L
        private const val NOTIF_HOME_LOST = 3

        fun sweepNow() = instance?.sweep()

        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        fun component(context: Context) = ComponentName(context, NotificationBlocker::class.java)

        fun isFolderMode(context: Context): Boolean =
            AppPrefs(context).setupDone && !SmartSession.isSmart(context)

        fun notifyHomeLost(context: Context) {
            val pi = PendingIntent.getActivity(
                context, 4,
                Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val n = Notification.Builder(context, SmartSession.CHANNEL_ALERT)
                .setSmallIcon(R.drawable.ic_stat_jeopda)
                .setContentTitle("접다가 기본 홈 앱이 아니에요")
                .setContentText("폴더폰 화면을 쓰려면 다시 홈 앱으로 지정해 주세요")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            context.getSystemService(android.app.NotificationManager::class.java).notify(NOTIF_HOME_LOST, n)
        }
    }
}
