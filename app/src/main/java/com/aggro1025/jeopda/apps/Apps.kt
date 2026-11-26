package com.aggro1025.jeopda.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager
import android.widget.Toast

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

/** 메뉴 1~5번 고정 항목. 모두 기기의 기본 앱을 연다 (요구사항 질문 6번 b안) */
enum class BuiltIn(val slot: Int, val label: String) {
    PHONE(1, "전화"),
    MESSAGES(2, "문자"),
    CONTACTS(3, "연락처"),
    ALARM(4, "알람"),
    CAMERA(5, "카메라"),
}

object Apps {

    fun launchBuiltIn(context: Context, item: BuiltIn) {
        // 메뉴 아이콘과 같은 앱이 열리도록, 찾은 패키지로 연다
        val pkg = builtInPackage(context, item)
        val generic = genericIntent(item)
        // 같은 패키지에 전화·연락처가 함께 있는 기기도 있으므로, 그 기능을 하는 화면(액티비티)을 정확히 지정한다
        val target = pkg?.let { p ->
            context.packageManager.queryIntentActivities(generic, 0)
                .firstOrNull { it.activityInfo.packageName == p }
                ?.activityInfo
        }
        val intent = if (target != null) {
            // 앱 종류로 찾는 인텐트(문자·연락처)는 찾은 화면을 일반 실행으로 연다
            val base = if (generic.selector != null) Intent(Intent.ACTION_MAIN) else Intent(generic)
            base.setClassName(target.packageName, target.name)
        } else {
            pkg?.let { context.packageManager.getLaunchIntentForPackage(it) } ?: generic
        }
        start(context, intent)
    }

    private fun genericIntent(item: BuiltIn): Intent = when (item) {
        BuiltIn.PHONE -> Intent(Intent.ACTION_DIAL)
        BuiltIn.MESSAGES -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
        BuiltIn.CONTACTS -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS)
        BuiltIn.ALARM -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        BuiltIn.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    }

    /**
     * 메뉴 아이콘용: 각 고정 항목을 여는 실제 앱의 패키지.
     * 1) 기기 기본 앱 → 2) 그 기능을 하는 앱 중 시스템 앱(제조사 기본 앱) → 3) 알려진 기본 앱 이름 순으로 찾는다.
     */
    fun builtInPackage(context: Context, item: BuiltIn): String? {
        val pm = context.packageManager
        val intent = genericIntent(item)

        val default = when (item) {
            BuiltIn.PHONE -> defaultDialerPackage(context)
            BuiltIn.MESSAGES -> Telephony.Sms.getDefaultSmsPackage(context)
            else -> pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }
        // "android"는 앱 선택 창이라 아이콘을 정할 수 없음
        if (default != null && default != "android" && isInstalled(pm, default)) return default

        val candidates = pm.queryIntentActivities(intent, 0).map { it.activityInfo }
        candidates.firstOrNull { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            ?.let { return it.packageName }
        candidates.firstOrNull()?.let { return it.packageName }

        return KNOWN_PACKAGES[item]?.firstOrNull { isInstalled(pm, it) }
    }

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** 삼성·구글 기본 앱 이름 (마지막 대안) */
    private val KNOWN_PACKAGES = mapOf(
        BuiltIn.PHONE to listOf("com.samsung.android.dialer", "com.google.android.dialer", "com.android.dialer"),
        BuiltIn.MESSAGES to listOf("com.samsung.android.messaging", "com.google.android.apps.messaging"),
        BuiltIn.CONTACTS to listOf("com.samsung.android.app.contacts", "com.google.android.contacts", "com.android.contacts"),
        BuiltIn.ALARM to listOf("com.sec.android.app.clockpackage", "com.google.android.deskclock", "com.android.deskclock"),
        BuiltIn.CAMERA to listOf("com.sec.android.app.camera", "com.google.android.GoogleCamera", "com.android.camera2"),
    )

    /** 다이얼러에 번호를 채워서 연다. 통화 버튼은 사용자가 누른다 (긴급번호 포함 동일) */
    fun dial(context: Context, number: String) {
        val uri = if (number.isEmpty()) null else Uri.fromParts("tel", number, null)
        start(context, Intent(Intent.ACTION_DIAL, uri))
    }

    fun launchPackage(context: Context, pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(context, "앱을 찾을 수 없어요", Toast.LENGTH_SHORT).show()
            return
        }
        start(context, intent)
    }

    fun entry(context: Context, pkg: String): AppEntry? {
        val pm = context.packageManager
        return try {
            val info = pm.getApplicationInfo(pkg, 0)
            AppEntry(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info))
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** 앱 서랍에 보이는 앱 전체 (접다 자신 제외), 이름순 */
    fun launchableApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .mapNotNull { entry(context, it) }
            .sortedBy { it.label.lowercase() }
    }

    /** 스마트폰 모드에서 띄울 수 있는 다른 홈 앱 목록 (예: 삼성 One UI 홈) */
    fun otherHomeApps(context: Context): List<Pair<ComponentName, String>> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName && it.packageName !in IGNORED_HOME_PACKAGES }
            .map { ComponentName(it.packageName, it.name) to it.loadLabel(pm).toString() }
    }

    /** 접다가 지금 기본 홈 앱인지 */
    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == context.packageName
    }

    private fun defaultDialerPackage(context: Context): String? =
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage

    private fun defaultPackageLaunch(context: Context, pkg: String?): Intent? =
        pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "이 기능을 여는 앱이 없어요", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "앱을 열 수 없어요", Toast.LENGTH_SHORT).show()
        }
    }

    /** 홈 후보에서 뺄 시스템 패키지 (부팅 중 임시 홈 등) */
    private val IGNORED_HOME_PACKAGES = setOf("com.android.settings", "com.android.provision")
}
