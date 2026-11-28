package com.aggro1025.jeopda.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** 구름 고양이 테마 색 */
object CC {
    val ink = Color(0xFF3A2E4D)
    val paper = Color(0xFFFFFDF8)
    val skyTop = Color(0xFF5B4CB8)
    val skyMid = Color(0xFF9B8FE0)
    val skyLight = Color(0xFFC9C3F2)
    val skyLow = Color(0xFFA9D0F5)
    val moon = Color(0xFFF7E08A)
    val planet = Color(0xFFF29A98)
    val tram = Color(0xFFFFF0D6)
    val band = Color(0xFFD9503F)
    val window = Color(0xFFF4C85A)
    val cat = Color(0xFFFFF8EE)
    val ginger = Color(0xFFF0A050)
    val call = Color(0xFF3E9C66)
    val keyShade = Color(0xFFC3B8D6)
    val pressed = Color(0xFFEEE8F8)
}

/** 4각 반짝이 모양 (가운데가 원점) */
fun star4(s: Float): String {
    val a = 0.16f * s
    return "M0,${f1(-s)}Q${f1(a)},${f1(-a)} ${f1(s)},0Q${f1(a)},${f1(a)} 0,${f1(s)}Q${f1(-a)},${f1(a)} ${f1(-s)},0Q${f1(-a)},${f1(-a)} 0,${f1(-s)}Z"
}

/**
 * 구름 고양이 테마의 화면(디스플레이) 그림. 한 번만 만들어 두고 계속 쓴다.
 * 좌표는 시안(docs/design/moonlight-theme.html)과 같다: 화면 판은 x 12~372, y 10~452 (가로 360).
 * 사용자 참고 그림을 따라 그리지 않고 새로 그린 고양이다.
 */
class CloudCatArt {
    private val rnd = Random(7)
    private fun r(a: Float, b: Float) = a + (b - a) * rnd.nextFloat()
    private fun ink(d: String, w: Float = 1f, closed: Boolean = false, alpha: Float = 1f, color: Color = CC.ink, taper: Float = 1f, step: Float = 2.5f, persp: ((Float, Float) -> Float)? = null) =
        InkStroke(sample(d, step), closed, w, taper, color, alpha, persp, rnd)
    private fun ink(p: Pts, w: Float = 1f, closed: Boolean = false, alpha: Float = 1f, color: Color = CC.ink, taper: Float = 1f) =
        InkStroke(p, closed, w, taper, color, alpha, null, rnd)

    class Leaves(val path: Path, val color: Color, val alpha: Float)
    class Cloud(val fill: Path, val fillColor: Color, val fillAlpha: Float, val hatch: Leaves, val ink: InkStroke, val amp: Float, val per: Float, val ph: Float)
    class Dot(val x: Float, val y: Float, val r: Float, val ph: Float, val sp: Float)
    class Sparkle(val x: Float, val y: Float, val fill: Path, val ink: InkStroke, val ph: Float, val sp: Float)
    class Mist(val x0: Float, val v: Float, val cy: Float, val rx: Float, val ry: Float)
    class Window(val fill: Path, val ink: InkStroke, val cx: Float, val cy: Float, val ph: Float)

    // ---------- 하늘 ----------
    val skyLight = Path()
    val skyDark = Path()
    val dots = ArrayList<Dot>()
    val sparkles = ArrayList<Sparkle>()

    // ---------- 달과 행성 ----------
    val moonFill = pathOf("M50,112A30,30 0 1 1 110,112A30,30 0 1 1 50,112Z")
    val moonHatch = Path()
    val moonInk = ArrayList<InkStroke>()
    val ringBack: InkStroke
    val planetFill = pathOf("M95,138A17,17 0 1 1 129,138A17,17 0 1 1 95,138Z")
    val planetHatch = Path()
    val planetInk = ArrayList<InkStroke>()

    // ---------- 구름 ----------
    val far: Cloud
    val mists = ArrayList<Mist>()
    val mid = ArrayList<Cloud>()
    val sea = ArrayList<Cloud>()
    val near = ArrayList<Cloud>()

    // ---------- 전차 ----------
    val tramStatic = ArrayList<InkStroke>()
    val tramBody = pathOf("M156,304C156,296 170,289 190,288C210,289 224,296 224,304L225,348C225,352 222,354 218,354L162,354C158,354 155,352 155,348Z")
    val tramHatch = Path()
    val tramInkBack = ArrayList<InkStroke>()
    val tramInkFront = ArrayList<InkStroke>()
    val windows = ArrayList<Window>()
    val signFill = pathOf("M172,292L208,292L208,301L172,301Z")
    val bogie = pathOf("M166,354L166,358.5L214,358.5L214,354Z")

    // ---------- 고양이 (고양이 좌표) ----------
    val catBody = pathOf("M18,79C6,76 4,58 12,46C20,32 38,24 58,24C72,24 82,26 92,33L101,56C105,66 104,74 98,79Z")
    val catGinger = Path()
    val catShade = Path()
    val catBodyInk = ArrayList<InkStroke>()
    val earLFill = pathOf("M86,31L88,8L99,18Z")
    val earRFill = pathOf("M107,18L119,6L121,27Z")
    val earLeaves = Path()
    val earLInk: InkStroke
    val earRInk: InkStroke
    val headFill = pathOf("M84,37C82,25 90,16 102,16C114,16 122,24 121,36C120,47 111,53 101,53C91,53 85,46 84,37Z")
    val headGinger = Path()
    val headInk = ArrayList<InkStroke>()
    val nose = pathOf("M104.5,38.5L108.5,38.5L106.5,40.8Z")
    val bandFill = pathOf("M86,46C94,52 108,54 119,47L118,54C108,61 94,59 87,53Z")
    val bandInk = ArrayList<InkStroke>()
    val tailInk: InkStroke
    val scarfInk: InkStroke

    // ---------- 글자 화면용 작은 행성들 (가장자리에 걸치게) ----------
    class MiniPlanet(val fill: Path, val color: Color, val hatch: Path, val hatchColor: Color, val inks: List<InkStroke>)
    val miniPlanets = ArrayList<MiniPlanet>()

    // ---------- 효과용 ----------
    val starPath = pathOf(star4(1f))
    val heartPath = pathOf("M0,2.5C-7,-3 -4.5,-9 0,-5.5C4.5,-9 7,-3 0,2.5Z")

    private fun cloud(x: Float, y: Float, w: Float, h: Float, b: Int, fill: Color, fillAlpha: Float, inkW: Float, inkAlpha: Float, hatchN: Int, hatchColor: Color, hatchAlpha: Float, amp: Float, per: Float): Cloud {
        val yb = y + h * 0.45f
        val px = FloatArray(b + 1); val py = FloatArray(b + 1)
        for (i in 0..b) {
            px[i] = x + w * i / b + (if (i in 1 until b) (rnd.nextFloat() - 0.5f) * w / b * 0.3f else 0f)
            py[i] = yb + (rnd.nextFloat() - 0.5f) * h * 0.18f
        }
        val sb = StringBuilder()
        sb.append("M${f1(x)},${f1(y + h)}Q${f1(x - 4)},${f1((y + h + py[0]) / 2)} ${f1(px[0])},${f1(py[0])}")
        for (i in 0 until b) {
            val ch = hypot(px[i + 1] - px[i], py[i + 1] - py[i])
            val rr = f1(ch / 2 * (1 + rnd.nextFloat() * 0.25f))
            sb.append("A$rr,$rr 0 0 1 ${f1(px[i + 1])},${f1(py[i + 1])}")
        }
        sb.append("Q${f1(x + w + 4)},${f1((y + h + py[b]) / 2)} ${f1(x + w)},${f1(y + h)}Q${f1(x + w / 2)},${f1(y + h + 5)} ${f1(x)},${f1(y + h)}Z")
        val d = sb.toString()
        val hatch = Path()
        repeat(hatchN) { hatch.leaf(r(x, x + w), r(yb - 2, y + h), r(5f, 10f), -0.62f + r(-0.15f, 0.15f), r(0.5f, 0.95f)) }
        return Cloud(pathOf(d), fill, fillAlpha, Leaves(hatch, hatchColor, hatchAlpha * 0.8f), ink(d, inkW, closed = true, alpha = inkAlpha, step = 3f), amp * r(0.6f, 1.2f), per * r(0.8f, 1.2f), r(0f, 6.28f))
    }

    init {
        // 하늘 빗금 (화면이 세로로 길 때를 위해 위쪽까지)
        repeat(260) {
            val light = rnd.nextFloat() < 0.55f
            (if (light) skyLight else skyDark).leaf(r(-10f, 394f), r(-220f, 300f), r(6f, 13f), -0.5f + r(-0.15f, 0.15f), r(0.55f, 1.1f))
        }
        // 작은 별
        repeat(22) {
            var x: Float; var y: Float
            do { x = r(20f, 364f); y = r(20f, 270f) } while ((x > 200f && y < 165f) || hypot(x - 90f, y - 120f) < 50f)
            dots += Dot(x, y, r(0.7f, 1.4f), r(0f, 6.28f), r(1.5f, 3.5f))
        }
        // 반짝이
        listOf(
            Triple(40f, 52f, 6f), Triple(176f, 62f, 5f), Triple(150f, 190f, 7.5f), Triple(196f, 120f, 4.5f), Triple(40f, 214f, 5f),
            Triple(250f, 212f, 6.5f), Triple(336f, 196f, 5f), Triple(196f, 246f, 4f), Triple(118f, 254f, 3.5f), Triple(292f, 238f, 3.5f),
        ).forEach { (x, y, s) ->
            val d = star4(s)
            sparkles += Sparkle(x, y, pathOf(d), ink(d, 0.55f, closed = true, alpha = 0.7f, step = 1.5f), r(0f, 6.28f), r(1.4f, 2.6f))
        }
        // 달
        repeat(34) {
            val a = r(-0.6f, 2.2f); val rr = r(4f, 30f)
            moonHatch.leaf(80 + cos(a) * rr, 112 + sin(a) * rr, r(5f, 9f), -0.7f + r(-0.12f, 0.12f), r(0.5f, 0.9f))
        }
        moonInk += ink("M67,99q4,-3 8,0", 0.7f, alpha = 0.5f)
        moonInk += ink("M86,94q3,-2 6,0", 0.6f, alpha = 0.45f)
        moonInk += ink("M70,124q3,2 6,0", 0.6f, alpha = 0.4f)
        moonInk += ink("M50,112A30,30 0 1 1 110,112A30,30 0 1 1 50,112Z", 1.05f, closed = true)
        ringBack = ink(ellipsePts(112f, 138f, 28f, 6.5f, -0.36f, PI.toFloat(), (2 * PI).toFloat()), 0.8f, alpha = 0.55f)
        repeat(18) {
            val a = r(-0.4f, 2f); val rr = r(3f, 17f)
            planetHatch.leaf(112 + cos(a) * rr, 138 + sin(a) * rr, r(4f, 7f), -0.7f, r(0.45f, 0.8f))
        }
        planetInk += ink("M103,132q4,-3 9,-2", 0.6f, alpha = 0.5f, color = Color.White)
        planetInk += ink("M95,138A17,17 0 1 1 129,138A17,17 0 1 1 95,138Z", 1f, closed = true)
        planetInk += ink(ellipsePts(112f, 138f, 28f, 6.5f, -0.36f, 0f, PI.toFloat()), 0.95f)

        // 먼 구름, 안개
        far = cloud(-6f, 282f, 396f, 34f, 12, Color(0xFFE6E0FA), 0.92f, 0.6f, 0.35f, 30, Color(0xFFA79CDC), 0.3f, 3f, 46f)
        repeat(3) { i -> mists += Mist(r(0f, 520f), r(4f, 9f), 298f + i * 16, r(60f, 90f), r(6f, 9f)) }
        // 가운데 구름
        mid += cloud(4f, 304f, 150f, 50f, 5, Color(0xFFF1EEFD), 1f, 0.8f, 0.6f, 26, Color(0xFFACA2E0), 0.4f, 4f, 34f)
        mid += cloud(228f, 300f, 154f, 52f, 5, Color(0xFFF1EEFD), 1f, 0.8f, 0.6f, 26, Color(0xFFACA2E0), 0.4f, 4f, 38f)
        // 구름 바다 (멀수록 흐리게)
        listOf(
            Triple(318f, Color(0xFFE2DCF7), 0.4f), Triple(340f, Color(0xFFEAE6FA), 0.5f),
            Triple(364f, Color(0xFFF2EFFD), 0.6f), Triple(390f, Color(0xFFF9F7FF), 0.72f),
        ).forEachIndexed { i, (y, c, op) ->
            sea += cloud(-12f, y, 408f, 64f, 10 + i, c, 1f, 0.6f + i * 0.1f, op, 34, Color(0xFFADA3DF), 0.3f + i * 0.05f, 2f + i, 40f - i * 4)
        }
        // 전차 선과 레일
        tramStatic += ink("M-10,264Q190,281 394,260", 0.6f, alpha = 0.55f, taper = 0.2f)
        val depth = { y: Float -> ((y - 300f) / 155f).coerceIn(0f, 1f) }
        for (k in 1..16) {
            val u = k / 16f
            val y = 300f + 155f * u.pow(1.7f)
            if (y < 352f) continue
            val s = depth(y); val xl = 183 - 73 * s; val xr = 197 + 73 * s
            tramStatic += ink(densify(xl - 5 * s - 2, y, xr + 5 * s + 2, y + 0.3f), 0.5f + 1.7f * s, alpha = 0.8f, taper = 0.6f)
        }
        val rail = { _: Float, y: Float -> 0.35f + 1.7f * depth(y) }
        tramStatic += ink("M183,300L110,455", 1.1f, taper = 0.15f, persp = rail)
        tramStatic += ink("M197,300L270,455", 1.1f, taper = 0.15f, persp = rail)
        // 전차
        tramInkBack += ink("M184,289L190,272.5L196,289", 0.8f)
        tramInkBack += ink("M181,272.5L199,272.5", 1f)
        repeat(26) { tramHatch.leaf(r(204f, 226f), r(292f, 354f), r(5f, 9f), -1f, r(0.5f, 0.85f)) }
        tramInkFront += ink("M156,334L224,334", 0.7f, alpha = 0.85f)
        tramInkFront += ink("M156,342L224,342", 0.7f, alpha = 0.85f)
        listOf(162f to 310f, 194f to 310f).forEachIndexed { i, (x, y) ->
            val wd = "M${x + 3},${y}L${x + 21},${y}Q${x + 24},${y} ${x + 24},${y + 3}L${x + 24},${y + 16}Q${x + 24},${y + 19} ${x + 21},${y + 19}L${x + 3},${y + 19}Q${x},${y + 19} ${x},${y + 16}L${x},${y + 3}Q${x},${y} ${x + 3},${y}Z"
            windows += Window(pathOf(wd), ink(wd, 0.8f, closed = true), x + 12, y + 10, i * 9.3f)
        }
        tramInkFront += ink("M172,292L208,292L208,301L172,301Z", 0.7f, closed = true)
        tramInkFront += ink("M186.8,347A3.2,3.2 0 1 1 193.2,347A3.2,3.2 0 1 1 186.8,347Z", 0.6f, closed = true, step = 1f)
        tramInkFront += ink("M156,304C156,296 170,289 190,288C210,289 224,296 224,304L225,348C225,352 222,354 218,354L162,354C158,354 155,352 155,348Z", 1.1f, closed = true)
        tramInkFront += ink("M190,310L190,329", 0.6f, alpha = 0.6f)

        // 가까운 구름
        val nearArgs = listOf(floatArrayOf(118f, 426f, 150f, 42f, 5f), floatArrayOf(16f, 414f, 142f, 52f, 5f), floatArrayOf(238f, 418f, 130f, 48f, 4f))
        nearArgs.forEach { a -> near += cloud(a[0], a[1], a[2], a[3], a[4].toInt(), Color.White, 1f, 1f, 1f, 40, Color(0xFFB3A9E2), 0.5f, 2.5f, 28f) }

        // 고양이 몸
        repeat(34) { catGinger.leaf(r(16f, 86f), r(25f, 44f), r(5f, 10f), -0.25f + r(-0.2f, 0.2f), r(0.6f, 1.1f)) }
        repeat(14) { catShade.leaf(r(30f, 96f), r(64f, 79f), r(5f, 8f), -0.4f, 0.55f) }
        catBodyInk += ink("M14,71C6,59 10,44 22,36C34,28 50,24 66,25C76,26 85,29 92,33", 1.1f)
        catBodyInk += ink("M26,79.5C46,80.5 70,80.5 88,79.5", 0.85f, alpha = 0.9f)
        catBodyInk += ink("M40,79C35,67 43,56 57,57C64,59 67,67 63,77", 0.9f)
        catBodyInk += ink("M93,52C99,58 102,67 99,76", 0.85f)
        catBodyInk += ink("M74,80C75,73 88,71 96,74C101,77 99,81 92,81", 0.85f)
        catBodyInk += ink("M86,77.5L86,80.5", 0.6f, taper = 0.5f)
        // 귀
        earLeaves.leaf(89f, 25f, 8f, -1.45f, 1.2f)
        earLeaves.leaf(117f, 22f, 8f, -1.2f, 1.1f)
        earLInk = ink("M86,31C86,22 87,14 88,8C92,11 95,14 99,18", 0.95f)
        earRInk = ink("M107,18C111,13 116,9 119,6C121,12 121,19 121,27", 0.95f)
        // 머리
        repeat(10) { headGinger.leaf(r(90f, 112f), r(17f, 24f), r(4f, 7f), -0.2f, 0.7f) }
        headInk += ink("M86,29C82,39 87,51 100,53C112,54 121,46 121,35C121,31 120.5,28 120,26", 1.05f)
        headInk += ink("M96,18.5C100,16.5 105,16.3 109,17.3", 0.9f)
        headInk += ink("M102.5,42.5Q104.5,44.6 106.5,41.4Q108.5,44.6 110.5,42.5", 0.6f, taper = 0.4f)
        headInk += ink("M116,40L129,38", 0.45f, alpha = 0.8f)
        headInk += ink("M116,43L129,44.5", 0.45f, alpha = 0.8f)
        headInk += ink("M93,41L85,40", 0.45f, alpha = 0.7f)
        // 목도리
        bandInk += ink("M86,46C94,52 108,54 119,47", 0.8f)
        bandInk += ink("M87,53C94,59 108,61 118,54", 0.8f)
        // 꼬리·목도리 꼬리 (매 프레임 모양이 바뀜)
        tailInk = InkStroke(Pts(FloatArray(2), FloatArray(2)), w = 1f, rnd = rnd, noRetrace = true)
        scarfInk = InkStroke(Pts(FloatArray(2), FloatArray(2)), w = 0.85f, rnd = rnd, noRetrace = true)
    }

    init {
        fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},${cy}A$r,$r 0 1 1 ${cx + r},${cy}A$r,$r 0 1 1 ${cx - r},${cy}Z"
        fun planet(cx: Float, cy: Float, rr: Float, color: Color, hatchColor: Color, ring: Boolean) {
            val d = circle(cx, cy, rr)
            val h = Path()
            repeat((rr * 1.2f).toInt()) {
                val a = r(-0.4f, 2f); val q = r(rr * 0.2f, rr)
                h.leaf(cx + cos(a) * q, cy + sin(a) * q, r(4f, 7f), -0.7f, r(0.45f, 0.8f))
            }
            val inks = ArrayList<InkStroke>()
            if (ring) inks += ink(ellipsePts(cx, cy, rr * 1.65f, rr * 0.38f, -0.36f, PI.toFloat(), (2 * PI).toFloat()), 0.8f, alpha = 0.55f)
            inks += ink(d, 1f, closed = true)
            if (ring) inks += ink(ellipsePts(cx, cy, rr * 1.65f, rr * 0.38f, -0.36f, 0f, PI.toFloat()), 0.95f)
            miniPlanets += MiniPlanet(pathOf(d), color, h, hatchColor, inks)
        }
        planet(358f, 150f, 16f, CC.planet, Color(0xFFC25E63), ring = true)
        planet(26f, 300f, 12f, CC.moon, Color(0xFFD9A93A), ring = false)
        planet(350f, 418f, 10f, Color(0xFF9CC9F2), Color(0xFF5E93C8), ring = true)
    }

    /** 모든 펜 선 (떨림을 미리 계산할 때) */
    fun allStatic(): List<InkStroke> = buildList {
        sparkles.forEach { add(it.ink) }
        addAll(moonInk); add(ringBack); addAll(planetInk)
        (listOf(far) + mid + sea + near).forEach { add(it.ink) }
        addAll(tramStatic); addAll(tramInkBack); addAll(tramInkFront); windows.forEach { add(it.ink) }
        addAll(catBodyInk); add(earLInk); add(earRInk); addAll(headInk); addAll(bandInk)
    }

    companion object {
        /** 화면 판 크기 (시안 좌표) */
        const val LEFT = 12f
        const val TOP = 10f
        const val WIDTH = 360f
        const val HEIGHT = 442f
        const val CAT_X = 24f
        const val CAT_Y = 344f

        @Volatile
        private var instance: CloudCatArt? = null
        fun get(): CloudCatArt = instance ?: synchronized(this) { instance ?: CloudCatArt().also { instance = it } }
    }
}
