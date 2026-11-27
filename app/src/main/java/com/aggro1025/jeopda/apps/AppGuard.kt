package com.aggro1025.jeopda.apps

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.aggro1025.jeopda.data.AppPrefs

/**
 * 폴더폰 모드에서 허용하지 않은 앱이 화면에 뜨는지 확인한다 (최근 앱 화면 등 홈이 아닌 경로 대응).
 * "사용 기록 접근" 권한으로 지금 화면 맨 앞 앱(패키지 이름)만 확인하고, 기록은 저장하거나 보내지 않는다.
 */
object AppGuard {

    /** 폴더폰 모드에서도 항상 열 수 있는 시스템 화면 */
    private val SYSTEM_ALLOWED = setOf(
        "android", "com.android.systemui", "com.android.settings",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.phone", "com.android.server.telecom", "com.samsung.android.incallui",
        "com.android.emergency", "com.samsung.android.emergency", "com.google.android.gms",
    )

    /** 허용 목록: 접다, 메뉴 1~9번, 알림 허용 앱, 키보드, 시스템 화면 */
    fun allowedPackages(context: Context): Set<String> {
        val prefs = AppPrefs(context)
        return buildSet {
            add(context.packageName)
            addAll(SYSTEM_ALLOWED)
            BuiltIn.entries.forEach { Apps.builtInPackage(context, it)?.let(::add) }
            AppPrefs.USER_SLOTS.forEach { slot ->
                prefs.slotPackage(slot)?.takeIf { AppPolicy.menuBlockReason(context, it) == null }?.let(::add)
            }
            addAll(prefs.allowedNotifApps)
            context.getSystemService(InputMethodManager::class.java)?.enabledInputMethodList
                ?.forEach { add(it.packageName) }
        }
    }

    fun isAllowed(pkg: String, allowed: Set<String>): Boolean =
        pkg in allowed ||
            pkg.contains("cellbroadcast", ignoreCase = true) ||
            // 500원 결제 창은 플레이스토어 화면으로 뜬다
            (pkg == "com.android.vending" && com.aggro1025.jeopda.billing.RefillBilling.isPurchasing())

    /** 지금 화면 맨 앞 앱. 최근 [lookbackMs] 동안 바뀐 게 없으면 null */
    fun foregroundPackage(context: Context, lookbackMs: Long = 10_000L): String? {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val end = System.currentTimeMillis()
        val events = try {
            usm.queryEvents(end - lookbackMs, end)
        } catch (e: SecurityException) {
            return null
        } ?: return null
        val e = UsageEvents.Event()
        var pkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) pkg = e.packageName
        }
        return pkg
    }

    // ---------------- 권한 ----------------

    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** 두 권한이 모두 있어야 다른 앱 막기가 동작한다 */
    fun isReady(context: Context): Boolean = hasUsageAccess(context) && canDrawOverlays(context)
}
