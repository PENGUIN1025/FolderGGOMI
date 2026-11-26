package com.aggro1025.jeopda.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.core.content.ContextCompat

/** 기기 연락처 읽기. 기기 안에서만 쓰고 저장·전송하지 않는다 */
object ContactsRepo {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** 전화번호가 있는 연락처를 이름순으로 (같은 이름·번호는 하나로). 백그라운드 스레드에서 호출 */
    fun load(context: Context): List<Contact> {
        if (!hasPermission(context)) return emptyList()
        val result = LinkedHashMap<String, Contact>()
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER),
            null,
            null,
            "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { c ->
            val nameIdx = c.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY)
            val numIdx = c.getColumnIndex(Phone.NUMBER)
            while (c.moveToNext()) {
                val number = c.getString(numIdx)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val name = c.getString(nameIdx)?.trim().takeUnless { it.isNullOrEmpty() } ?: number
                val key = name + "|" + ContactSearch.digits(number)
                if (key !in result) result[key] = Contact(name, number)
            }
        }
        return result.values.toList()
    }
}
