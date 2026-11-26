package com.aggro1025.jeopda.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        val intent = when (item) {
            BuiltIn.PHONE -> defaultPackageLaunch(context, defaultDialerPackage(context))
                ?: Intent(Intent.ACTION_DIAL)
            BuiltIn.MESSAGES -> defaultPackageLaunch(context, Telephony.Sms.getDefaultSmsPackage(context))
                ?: Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
            BuiltIn.CONTACTS -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS)
            BuiltIn.ALARM -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            BuiltIn.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        }
        start(context, intent)
    }

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
