package com.aggro1025.jeopda.ui.folder

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.aggro1025.jeopda.apps.BuiltIn

/**
 * 실제 앱 아이콘을 읽지 못할 때(미리보기, 기본 앱을 못 찾은 경우) 쓰는 기호 아이콘.
 * 경로는 Material Icons(Apache 2.0)의 모양을 따른다.
 */
object Symbols {
    private fun vector(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { addPath(addPathNodes(it), fill = SolidColor(Color.White)) }
        }.build()

    val Chat = vector(
        "Chat",
        "M20,2H4c-1.1,0 -2,0.9 -2,2v18l4,-4h14c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2z",
    )

    val Alarm = vector(
        "Alarm",
        "M22,5.72l-4.6,-3.86 -1.29,1.53 4.6,3.86L22,5.72zM7.88,3.39L6.6,1.86 2,5.71l1.29,1.53 4.59,-3.85z" +
            "M12.5,8H11v6l4.75,2.85 0.75,-1.23 -4,-2.37V8zM12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9c4.97,0 9,-4.03 9,-9" +
            "s-4.03,-9 -9,-9zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
    )

    val Camera = vector(
        "Camera",
        "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0",
        "M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9z" +
            "M12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
    )

    val Apps = vector(
        "Apps",
        "M4,8h4V4H4v4zM10,20h4v-4h-4v4zM4,20h4v-4H4v4zM4,14h4v-4H4v4zM10,14h4v-4h-4v4zM16,4v4h4V4h-4z" +
            "M10,8h4V4h-4v4zM16,14h4v-4h-4v4zM16,20h4v-4h-4v4z",
    )

    fun forSlot(slot: Int): ImageVector = when (BuiltIn.entries.firstOrNull { it.slot == slot }) {
        BuiltIn.PHONE -> Icons.Filled.Call
        BuiltIn.MESSAGES -> Chat
        BuiltIn.CONTACTS -> Icons.Filled.Person
        BuiltIn.ALARM -> Alarm
        BuiltIn.CAMERA -> Camera
        null -> Apps
    }
}
