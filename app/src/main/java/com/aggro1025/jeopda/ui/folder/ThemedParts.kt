package com.aggro1025.jeopda.ui.folder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import com.aggro1025.jeopda.ui.theme.CC
import com.aggro1025.jeopda.ui.theme.FolderTheme
import com.aggro1025.jeopda.ui.theme.InkStroke
import com.aggro1025.jeopda.ui.theme.LocalFolderTheme
import com.aggro1025.jeopda.ui.theme.leaf
import com.aggro1025.jeopda.ui.theme.pathOf
import com.aggro1025.jeopda.ui.theme.sample
import kotlin.random.Random

/** 지금 구름 고양이 테마인지 */
@Composable
internal fun isCatTheme(): Boolean = LocalFolderTheme.current == FolderTheme.CLOUD_CAT

private fun roundRectPath(w: Float, h: Float, r: Float, inset: Float = 1f) =
    Path().apply { addRoundRect(RoundRect(inset, inset, w - inset, h - inset, CornerRadius(r))) }

/**
 * 구름 고양이 테마에서 칸·버튼 둘레에 손으로 그린 흰 펜 선을 두른다 (밤하늘 위라 흰 선).
 * on이 false면 아무것도 하지 않는다.
 */
internal fun Modifier.inkFrame(on: Boolean, radius: Dp, strong: Boolean, seed: Int = 0): Modifier =
    if (!on) this else drawWithCache {
        val d = density
        val w = size.width / d; val h = size.height / d
        val ink = InkStroke(sample(roundRectPath(w, h, radius.value), 3f), closed = true, w = if (strong) 1.3f else 0.9f, color = CC.paper, alpha = if (strong) 0.95f else 0.55f, rnd = Random(40 + seed))
        onDrawWithContent {
            drawContent()
            scale(d, d, Offset.Zero) { ink.draw(this, 0) }
        }
    }

/** 기본 앱 1~5번 손그림 아이콘 (48×48 좌표). 직접 그린 모양이라 다른 앱의 아이콘·상표를 본뜨지 않는다 */
private class CatIcon(val tint: Color, val fills: List<Pair<String, Color>>, val lines: List<Pair<String, Float>>)

private val CAT_ICONS = mapOf(
    1 to CatIcon(
        Color(0xFFDDF2E4),
        emptyList(),
        listOf("M16,13Q17,32 35,35" to 3.4f, "M12,16L19,9" to 2.8f, "M32,39L39,32" to 2.8f),
    ),
    2 to CatIcon(
        Color(0xFFFFF0C2),
        listOf("M11,14Q11,10 15,10L33,10Q37,10 37,14L37,27Q37,31 33,31L21,31L14,37L16,31L15,31Q11,31 11,27Z" to Color(0xFFFFD66B)),
        listOf("M11,14Q11,10 15,10L33,10Q37,10 37,14L37,27Q37,31 33,31L21,31L14,37L16,31L15,31Q11,31 11,27Z" to 1.2f, "M17,18L31,18" to 1f, "M17,24L27,24" to 1f),
    ),
    3 to CatIcon(
        Color(0xFFDDEBFB),
        listOf("M18,18A6,6 0 1 1 30,18A6,6 0 1 1 18,18Z" to Color(0xFF9CC4F2), "M12,38Q12,27 24,27Q36,27 36,38Z" to Color(0xFF9CC4F2)),
        listOf("M18,18A6,6 0 1 1 30,18A6,6 0 1 1 18,18Z" to 1.2f, "M12,38Q12,27 24,27Q36,27 36,38Z" to 1.2f),
    ),
    4 to CatIcon(
        Color(0xFFFFE1D8),
        listOf("M12,26A12,12 0 1 1 36,26A12,12 0 1 1 12,26Z" to Color(0xFFFFB3A0)),
        listOf(
            "M12,26A12,12 0 1 1 36,26A12,12 0 1 1 12,26Z" to 1.2f, "M24,26L24,19" to 1.2f, "M24,26L29,29" to 1.2f,
            "M11,15Q13,10 18,9" to 1.4f, "M37,15Q35,10 30,9" to 1.4f, "M16,36L13,40" to 1.2f, "M32,36L35,40" to 1.2f,
        ),
    ),
    5 to CatIcon(
        Color(0xFFE8E4F4),
        listOf("M9,18Q9,15 12,15L18,15L21,11L27,11L30,15L36,15Q39,15 39,18L39,34Q39,37 36,37L12,37Q9,37 9,34Z" to Color(0xFFC9C1E6), "M18,26A6,6 0 1 1 30,26A6,6 0 1 1 18,26Z" to Color(0xFFFFFDF8)),
        listOf(
            "M9,18Q9,15 12,15L18,15L21,11L27,11L30,15L36,15Q39,15 39,18L39,34Q39,37 36,37L12,37Q9,37 9,34Z" to 1.2f,
            "M18,26A6,6 0 1 1 30,26A6,6 0 1 1 18,26Z" to 1.2f, "M21.5,26A2.5,2.5 0 1 1 26.5,26A2.5,2.5 0 1 1 21.5,26Z" to 0.9f,
        ),
    ),
)

/**
 * 구름 고양이 테마의 메뉴 아이콘: 종이 타일 + 펜 선.
 * 1~5번(기본 앱)은 직접 그린 그림, 6~9번(사용자가 고른 앱)은 그 앱의 진짜 아이콘을 종이 타일 안에 넣는다
 * (다른 회사 아이콘을 손그림으로 바꾸면 상표를 본뜨는 것이 되므로).
 */
@Composable
internal fun CatMenuIcon(slot: Int, icon: ImageBitmap?, empty: Boolean, size: Dp, label: String = "") {
    val art = CAT_ICONS[slot]
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        val parts = remember(slot, empty) { CatTileParts(slot, empty, art) }
        Canvas(Modifier.size(size)) {
            val k = this.size.width / 48f
            scale(k, k, Offset.Zero) { drawTile(parts, empty) }
        }
        if (art == null && !empty) {
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(size * 0.66f))
            } else {
                // 아이콘을 못 읽으면 앱 이름 첫 글자
                androidx.compose.material3.Text(
                    label.take(1),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = androidx.compose.ui.unit.TextUnit(size.value * 0.42f, androidx.compose.ui.unit.TextUnitType.Sp),
                        color = CC.ink,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private class CatTileParts(slot: Int, empty: Boolean, art: CatIcon?) {
    val rnd = Random(70 + slot)
    val tile = roundRectPath(48f, 48f, 12f, 2f)
    val tileInk = InkStroke(sample(tile, 2.5f), closed = true, w = 1.1f, color = if (empty) CC.paper else CC.ink, alpha = if (empty) 0.6f else 1f, rnd = rnd)
    val shade = Path().apply { repeat(8) { leaf(6f + rnd.nextFloat() * 34f, 38f + rnd.nextFloat() * 7f, 5f + rnd.nextFloat() * 3f, -0.45f, 0.5f) } }
    val tint = art?.tint ?: CC.paper
    val fills = art?.fills?.map { pathOf(it.first) to it.second }.orEmpty()
    val lines = art?.lines?.map { (d, w) ->
        val call = slot == 1
        InkStroke(sample(d, 1.5f), closed = d.endsWith("Z"), w = w, color = if (call) CC.call else CC.ink, taper = if (call) 0.6f else 1f, rnd = rnd)
    }.orEmpty()
    val plus = listOf(
        InkStroke(sample("M24,15L24,33", 1.5f), w = 1.4f, color = CC.paper, alpha = 0.6f, rnd = rnd),
        InkStroke(sample("M15,24L33,24", 1.5f), w = 1.4f, color = CC.paper, alpha = 0.6f, rnd = rnd),
    )
}

private fun DrawScope.drawTile(p: CatTileParts, empty: Boolean) {
    if (empty) {
        p.tileInk.draw(this, 0)
        p.plus.forEach { it.draw(this, 0) }
        return
    }
    drawPath(p.tile, p.tint)
    clipPath(p.tile) { drawPath(p.shade, CC.keyShade, 0.45f) }
    p.fills.forEach { (path, c) -> drawPath(path, c) }
    p.lines.forEach { it.draw(this, 0) }
    p.tileInk.draw(this, 0)
}
