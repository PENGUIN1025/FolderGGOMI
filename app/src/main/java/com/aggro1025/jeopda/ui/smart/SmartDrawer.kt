package com.aggro1025.jeopda.ui.smart

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.aggro1025.jeopda.apps.AppEntry
import com.aggro1025.jeopda.apps.Apps
import com.aggro1025.jeopda.session.SmartSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 원래 홈 앱으로 넘길 수 없을 때만 쓰는 대체 화면 (단순 앱 목록).
 * 보통은 스마트폰 모드에서 원래 홈 앱(One UI 홈 등)이 바로 뜬다.
 */
@Composable
fun SmartDrawer(onReturn: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { Apps.launchableApps(context) } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("스마트폰 모드", fontSize = 18.sp)
                    Text(
                        "${SmartSession.formatTime(SmartSession.endAt(context))}에 끝나요",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Button(onClick = onReturn) { Text("폴더폰으로 돌아가기") }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Column(
                        Modifier.clickable { Apps.launchPackage(context, app.packageName) }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        app.icon?.let {
                            val bmp = remember(app.packageName) { it.toBitmap(144, 144).asImageBitmap() }
                            Image(bmp, contentDescription = null, modifier = Modifier.size(48.dp))
                        }
                        Text(
                            app.label,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** 최초 설정을 마치기 전 홈 화면 */
@Composable
fun SetupPending(onOpen: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("접다", fontSize = 28.sp)
            Text(
                "설정을 마치면 폴더폰 모드가 시작돼요",
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpen) { Text("설정 열기") }
        }
    }
}
