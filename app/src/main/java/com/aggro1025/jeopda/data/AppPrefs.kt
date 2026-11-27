package com.aggro1025.jeopda.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 기기 내 저장소 (요구사항 7장 "상태 저장").
 * 홈 버튼을 누를 때마다 즉시 읽어야 해서 동기식 SharedPreferences를 쓴다.
 */
@android.annotation.SuppressLint("ApplySharedPref")
class AppPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("jeopda", Context.MODE_PRIVATE)

    /** 최초 설정 완료 여부. 완료 전에는 폴더폰 모드가 아니어서 자유롭게 편집 가능 */
    var setupDone: Boolean
        get() = sp.getBoolean(KEY_SETUP_DONE, false)
        set(v) = sp.edit().putBoolean(KEY_SETUP_DONE, v).apply()

    // 모드·무료 사용 기록은 앱이 곧바로 종료돼도 남도록 commit()으로 즉시 저장한다

    /** 스마트폰 모드 종료 시각(벽시계 ms). 0이면 폴더폰 모드 */
    var smartEndAt: Long
        get() = sp.getLong(KEY_SMART_END_AT, 0L)
        set(v) { sp.edit().putLong(KEY_SMART_END_AT, v).commit() }

    /** 마지막으로 무료 해제를 쓴 날짜 (yyyy-MM-dd, 기기 현지 날짜) */
    var lastFreeDate: String?
        get() = sp.getString(KEY_LAST_FREE_DATE, null)
        set(v) { sp.edit().putString(KEY_LAST_FREE_DATE, v).commit() }

    /** 스마트폰 모드 종료 시점(부팅 후 경과 시간 ms)과 그때의 부팅 번호. 시계를 돌려도 남은 시간이 늘지 않게 */
    var smartEndElapsed: Long
        get() = sp.getLong(KEY_SMART_END_ELAPSED, 0L)
        set(v) { sp.edit().putLong(KEY_SMART_END_ELAPSED, v).commit() }

    var smartBoot: Int
        get() = sp.getInt(KEY_SMART_BOOT, -1)
        set(v) { sp.edit().putInt(KEY_SMART_BOOT, v).commit() }

    /** 믿을 수 있는 시각의 기준점 (네트워크 시간 또는 이번 부팅에서 처음 본 시각) */
    var clockAnchor: TrustedTime.Anchor?
        get() {
            val boot = sp.getInt(KEY_ANCHOR_BOOT, Int.MIN_VALUE)
            if (boot == Int.MIN_VALUE) return null
            return TrustedTime.Anchor(
                sp.getLong(KEY_ANCHOR_WALL, 0L),
                sp.getLong(KEY_ANCHOR_ELAPSED, 0L),
                boot,
                sp.getBoolean(KEY_ANCHOR_NETWORK, false),
            )
        }
        set(v) {
            sp.edit().apply {
                if (v == null) {
                    remove(KEY_ANCHOR_BOOT)
                } else {
                    putLong(KEY_ANCHOR_WALL, v.wall)
                    putLong(KEY_ANCHOR_ELAPSED, v.elapsed)
                    putInt(KEY_ANCHOR_BOOT, v.boot)
                    putBoolean(KEY_ANCHOR_NETWORK, v.fromNetwork)
                }
            }.commit()
        }

    /** 폴더폰 모드에서도 알림을 받을 앱 (최대 3개) */
    var allowedNotifApps: Set<String>
        get() = sp.getStringSet(KEY_ALLOWED_NOTIF, emptySet()).orEmpty().toSet()
        set(v) {
            require(v.size <= MAX_ALLOWED_NOTIF_APPS)
            sp.edit().putStringSet(KEY_ALLOWED_NOTIF, v).apply()
        }

    /** 홈 앱 해제 알림을 마지막으로 보낸 시각 (너무 자주 보내지 않게) */
    var lastHomeWarnAt: Long
        get() = sp.getLong(KEY_HOME_WARN, 0L)
        set(v) = sp.edit().putLong(KEY_HOME_WARN, v).apply()

    /** 스마트폰 모드에서 띄울 원래 홈 앱 (패키지/액티비티). null이면 자동 선택 */
    var smartHomeComponent: String?
        get() = sp.getString(KEY_SMART_HOME, null)
        set(v) = sp.edit().putString(KEY_SMART_HOME, v).apply()

    /** 메뉴 6~9번 사용자 선택 앱 패키지명 */
    fun slotPackage(slot: Int): String? = sp.getString("$KEY_SLOT_PREFIX$slot", null)

    fun setSlotPackage(slot: Int, pkg: String?) {
        require(slot in USER_SLOTS)
        sp.edit().apply { if (pkg == null) remove("$KEY_SLOT_PREFIX$slot") else putString("$KEY_SLOT_PREFIX$slot", pkg) }.apply()
    }

    fun addReason(entry: ReasonEntry) {
        val arr = JSONArray(sp.getString(KEY_REASONS, "[]"))
        arr.put(JSONObject().apply {
            put("at", entry.at)
            put("minutes", entry.minutes)
            put("method", entry.method)
            put("reason", entry.reason)
            entry.endedAt?.let { put("endedAt", it) }
        })
        sp.edit().putString(KEY_REASONS, arr.toString()).apply()
    }

    fun reasons(): List<ReasonEntry> {
        val arr = JSONArray(sp.getString(KEY_REASONS, "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ReasonEntry(
                o.getLong("at"), o.getInt("minutes"), o.getString("method"), o.getString("reason"),
                if (o.has("endedAt")) o.getLong("endedAt") else null,
            )
        }
    }

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.unregisterOnSharedPreferenceChangeListener(l)

    /** 마지막 해제 기록에 실제로 끝난 시각을 적는다 (일찍 돌아오면 그 시각, 만료면 예정 시각) */
    fun closeLastReason(endedAt: (ReasonEntry) -> Long) {
        val arr = JSONArray(sp.getString(KEY_REASONS, "[]"))
        if (arr.length() == 0) return
        val o = arr.getJSONObject(arr.length() - 1)
        if (o.has("endedAt")) return
        val e = ReasonEntry(o.getLong("at"), o.getInt("minutes"), o.getString("method"), o.getString("reason"))
        o.put("endedAt", endedAt(e).coerceIn(e.at, e.plannedEnd))
        sp.edit().putString(KEY_REASONS, arr.toString()).apply()
    }

    /** endedAt: 실제로 폴더폰으로 돌아온 시각 (없으면 아직 사용 중) */
    data class ReasonEntry(val at: Long, val minutes: Int, val method: String, val reason: String, val endedAt: Long? = null) {
        val plannedEnd: Long get() = at + minutes * 60_000L
    }

    companion object {
        val USER_SLOTS = 6..9
        const val MAX_ALLOWED_NOTIF_APPS = 3
        private const val KEY_SMART_END_ELAPSED = "smart_end_elapsed"
        private const val KEY_SMART_BOOT = "smart_boot"
        private const val KEY_ANCHOR_WALL = "anchor_wall"
        private const val KEY_ANCHOR_ELAPSED = "anchor_elapsed"
        private const val KEY_ANCHOR_BOOT = "anchor_boot"
        private const val KEY_ANCHOR_NETWORK = "anchor_network"
        private const val KEY_ALLOWED_NOTIF = "allowed_notif_apps"
        private const val KEY_HOME_WARN = "last_home_warn"
        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_SMART_END_AT = "smart_end_at"
        private const val KEY_LAST_FREE_DATE = "last_free_date"
        private const val KEY_SMART_HOME = "smart_home_component"
        private const val KEY_SLOT_PREFIX = "slot_"
        private const val KEY_REASONS = "reasons"
    }
}
