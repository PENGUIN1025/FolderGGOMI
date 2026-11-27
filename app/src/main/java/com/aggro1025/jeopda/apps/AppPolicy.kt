package com.aggro1025.jeopda.apps

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri

/**
 * 폴더폰 모드 규칙 (요구사항 5장).
 * - 메뉴에 넣을 수 없는 앱: 소셜·영상·게임 카테고리(메신저 예외), 차단 목록, 웹 브라우저, 앱 스토어
 * - 알림 기본 허용: 전화, 문자, 알람, 재난·긴급 알림, 시스템 알림, 접다 + 사용자 허용 앱 3개
 */
object AppPolicy {
    /** 차단 목록 대표 앱 (앱 안 파일로 시작, 원격 갱신은 나중에) */
    val BLOCKLIST = setOf(
        "com.instagram.android", // 인스타그램
        "com.instagram.barcelona", // 스레드
        "com.google.android.youtube", // 유튜브
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go", // 틱톡
        "com.facebook.katana", "com.facebook.lite", // 페이스북
        "com.twitter.android", // X
        "com.netflix.mediaclient", // 넷플릭스
        "com.snapchat.android", // 스냅챗
        "tv.twitch.android.app", // 트위치
    )

    /** 소셜 카테고리여도 메뉴에 넣을 수 있는 메신저 */
    val MESSENGERS = setOf(
        "com.kakao.talk", "jp.naver.line.android", "org.telegram.messenger",
        "com.whatsapp", "com.facebook.orca", "com.google.android.apps.messaging", "com.samsung.android.messaging",
    )

    private val APP_STORES = setOf("com.android.vending", "com.sec.android.app.samsungapps", "com.skt.skaf.A000Z00040", "com.onestore.android")

    const val BLOCKED_MESSAGE = "이 앱은 폴더폰 모드에서 사용할 수 없어요. 스마트폰 모드에서 쓰세요"

    /** 메뉴에 넣을 수 없으면 이유, 넣을 수 있으면 null */
    fun menuBlockReason(context: Context, pkg: String, browsers: Set<String> = browserPackages(context)): String? {
        if (pkg in MESSENGERS) return null
        if (pkg in BLOCKLIST) return "차단 목록에 있는 앱"
        if (pkg in APP_STORES) return "앱 스토어"
        if (pkg in browsers) return "웹 브라우저"
        val info = try {
            context.packageManager.getApplicationInfo(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        @Suppress("DEPRECATION")
        val isGameFlag = info.flags and ApplicationInfo.FLAG_IS_GAME != 0
        return when {
            info.category == ApplicationInfo.CATEGORY_GAME || isGameFlag -> "게임"
            info.category == ApplicationInfo.CATEGORY_SOCIAL -> "소셜 앱"
            info.category == ApplicationInfo.CATEGORY_VIDEO -> "영상 앱"
            else -> null
        }
    }

    /** 아무 웹 주소나 열 수 있는 앱 = 브라우저 */
    fun browserPackages(context: Context): Set<String> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.toSet()
    }

    // ---------------- 알림 ----------------

    private val SYSTEM_NOTIF_PACKAGES = setOf(
        "android", "com.android.systemui", "com.android.phone", "com.android.server.telecom",
        "com.samsung.android.incallui", "com.android.providers.telephony", "com.android.settings",
    )

    private val ALWAYS_ALLOWED_CATEGORIES = setOf(
        Notification.CATEGORY_CALL,
        "missed_call", // Notification.CATEGORY_MISSED_CALL (API 31 상수, 값만 사용)
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_SYSTEM,
        Notification.CATEGORY_ERROR,
    )

    /** 기본 허용 패키지: 기기의 전화·문자·시계 앱 + 시스템 */
    fun baseAllowedPackages(context: Context): Set<String> = buildSet {
        add(context.packageName)
        addAll(SYSTEM_NOTIF_PACKAGES)
        listOf(BuiltIn.PHONE, BuiltIn.MESSAGES, BuiltIn.ALARM).forEach { item ->
            Apps.builtInPackage(context, item)?.let { add(it) }
        }
    }

    fun isNotificationAllowed(
        pkg: String,
        category: String?,
        base: Set<String>,
        userAllowed: Set<String>,
    ): Boolean =
        pkg in base ||
            pkg in userAllowed ||
            category in ALWAYS_ALLOWED_CATEGORIES ||
            pkg.contains("cellbroadcast", ignoreCase = true) || // 재난문자
            pkg.contains("emergency", ignoreCase = true)
}
