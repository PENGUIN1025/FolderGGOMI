package com.aggro1025.jeopda.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 기기 내 저장소 (요구사항 7장 "상태 저장").
 * 홈 버튼을 누를 때마다 즉시 읽어야 해서 동기식 SharedPreferences를 쓴다.
 */
class AppPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("jeopda", Context.MODE_PRIVATE)

    /** 최초 설정 완료 여부. 완료 전에는 폴더폰 모드가 아니어서 자유롭게 편집 가능 */
    var setupDone: Boolean
        get() = sp.getBoolean(KEY_SETUP_DONE, false)
        set(v) = sp.edit().putBoolean(KEY_SETUP_DONE, v).apply()

    /** 스마트폰 모드 종료 시각(벽시계 ms). 0이면 폴더폰 모드 */
    var smartEndAt: Long
        get() = sp.getLong(KEY_SMART_END_AT, 0L)
        set(v) { sp.edit().putLong(KEY_SMART_END_AT, v).commit() }

    /** 마지막으로 무료 해제를 쓴 날짜 (yyyy-MM-dd, 기기 현지 날짜) */
    var lastFreeDate: String?
        get() = sp.getString(KEY_LAST_FREE_DATE, null)
        set(v) { sp.edit().putString(KEY_LAST_FREE_DATE, v).commit() }

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
        })
        sp.edit().putString(KEY_REASONS, arr.toString()).apply()
    }

    fun reasons(): List<ReasonEntry> {
        val arr = JSONArray(sp.getString(KEY_REASONS, "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ReasonEntry(o.getLong("at"), o.getInt("minutes"), o.getString("method"), o.getString("reason"))
        }
    }

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.unregisterOnSharedPreferenceChangeListener(l)

    data class ReasonEntry(val at: Long, val minutes: Int, val method: String, val reason: String)

    companion object {
        val USER_SLOTS = 6..9
        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_SMART_END_AT = "smart_end_at"
        private const val KEY_LAST_FREE_DATE = "last_free_date"
        private const val KEY_SMART_HOME = "smart_home_component"
        private const val KEY_SLOT_PREFIX = "slot_"
        private const val KEY_REASONS = "reasons"
    }
}
