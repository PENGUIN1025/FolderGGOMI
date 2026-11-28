package com.aggro1025.jeopda.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 손으로 그린 펜 선.
 * 경로 위 점을 고르게 뽑은 뒤(PathMeasure), 굵기가 조금씩 변하고 양 끝이 가늘어지는 다각형으로 바꿔 채워 그린다.
 * 가끔 살짝 비켜서 한 번 더 그은 선(겹쳐 긋기)을 더하고, tick이 바뀔 때마다 점을 아주 조금 흔들어 떨리는 느낌을 낸다.
 */
object Ink {
    var lineWidth = 1.15f
    var taper = 0.32f
    var retrace = 0.35f
    var wobble = 1.3f
    val color = Color(0xFF3A2E4D)
}

class Pts(val x: FloatArray, val y: FloatArray) {
    val n: Int get() = x.size
}

fun pathOf(d: String): Path = PathParser().parsePathString(d).toPath()

fun sample(path: Path, step: Float = 2.5f): Pts {
    val pm = PathMeasure()
    pm.setPath(path, false)
    val len = pm.length
    val n = max(2, ceil(len / step).toInt())
    val xs = FloatArray(n + 1)
    val ys = FloatArray(n + 1)
    for (i in 0..n) {
        val p = pm.getPosition(len * i / n)
        xs[i] = p.x
        ys[i] = p.y
    }
    return Pts(xs, ys)
}

fun sample(d: String, step: Float = 2.5f): Pts = sample(pathOf(d), step)

/** 꺾은선 점들을 촘촘하게 */
fun densify(vararg xy: Float, step: Float = 2.5f): Pts {
    val xs = ArrayList<Float>()
    val ys = ArrayList<Float>()
    xs += xy[0]; ys += xy[1]
    var i = 2
    while (i + 1 < xy.size) {
        val ax = xy[i - 2]; val ay = xy[i - 1]; val bx = xy[i]; val by = xy[i + 1]
        val n = max(1, ceil(hypot(bx - ax, by - ay) / step).toInt())
        for (k in 1..n) { xs += ax + (bx - ax) * k / n; ys += ay + (by - ay) * k / n }
        i += 2
    }
    return Pts(xs.toFloatArray(), ys.toFloatArray())
}

fun ellipsePts(cx: Float, cy: Float, rx: Float, ry: Float, rot: Float, a0: Float, a1: Float, n: Int = 40): Pts {
    val c = cos(rot); val s = sin(rot)
    val xs = FloatArray(n + 1); val ys = FloatArray(n + 1)
    for (i in 0..n) {
        val a = a0 + (a1 - a0) * i / n
        val x = rx * cos(a); val y = ry * sin(a)
        xs[i] = cx + x * c - y * s
        ys[i] = cy + x * s + y * c
    }
    return Pts(xs, ys)
}

private fun hash(i: Double): Double {
    val s = sin(i * 127.1 + 311.7) * 43758.5453
    return s - floor(s)
}

fun noise1(x: Double): Double {
    val i = floor(x)
    val f = x - i
    val u = f * f * (3 - 2 * f)
    return hash(i) * (1 - u) + hash(i + 1) * u
}

/** 소수 첫째 자리까지만 (경로 문자열용) */
fun f1(v: Float): String = ((v * 10).roundToInt() / 10f).toString()

/** 나뭇잎 모양 색연필 빗금 한 획을 경로에 더한다 */
fun Path.leaf(x: Float, y: Float, len: Float, ang: Float, w: Float) {
    val x2 = x + cos(ang) * len
    val y2 = y + sin(ang) * len
    val mx = (x + x2) / 2; val my = (y + y2) / 2
    val nx = -sin(ang) * w; val ny = cos(ang) * w
    moveTo(x, y)
    quadraticTo(mx + nx, my + ny, x2, y2)
    quadraticTo(mx - nx, my - ny, x, y)
    close()
}

/** 펜 선 하나 */
class InkStroke(
    var pts: Pts,
    val closed: Boolean = false,
    val w: Float = 1f,
    val taper: Float = 1f,
    val color: Color = Ink.color,
    val alpha: Float = 1f,
    val persp: ((Float, Float) -> Float)? = null,
    rnd: Random,
    noRetrace: Boolean = false,
) {
    private val seed = rnd.nextFloat() * 500f
    private val retR = if (noRetrace) 2f else rnd.nextFloat()
    private val rt0 = 0.04f + rnd.nextFloat() * 0.3f
    private val rt1 = 0.55f + rnd.nextFloat() * 0.4f
    private val rside = if (rnd.nextFloat() < 0.5f) -1f else 1f
    private var cachedTick = Int.MIN_VALUE
    private var cachedUnit = 0f
    private val main = Path()
    private val ret = Path()
    private var hasRet = false

    /** tick이 바뀌었거나 dynamic이면 다시 계산. unit: 선 굵기 배율 (화면 px로 그릴 때 density) */
    fun draw(scope: DrawScope, tick: Int, unit: Float = 1f, dynamic: Boolean = false, alphaMul: Float = 1f) {
        if (dynamic || tick != cachedTick || unit != cachedUnit) {
            cachedTick = tick
            cachedUnit = unit
            build(main, tick.toDouble(), 0f, 1f, 0f, 1f, unit)
            hasRet = Ink.retrace > retR && pts.n > 6
            if (hasRet) build(ret, tick + 0.37, rt0, rt1, 1.25f * rside * unit, 0.55f, unit)
        }
        scope.drawPath(main, color, alpha * alphaMul)
        if (hasRet) scope.drawPath(ret, color, alpha * alphaMul * 0.45f)
    }

    private fun build(out: Path, tick: Double, t0: Float, t1: Float, off: Float, wScale: Float, unit: Float) {
        out.reset()
        val p = pts
        val n = p.n
        if (n < 2) return
        val isClosed = closed && t0 == 0f && t1 == 1f
        val cum = FloatArray(n)
        for (i in 1 until n) cum[i] = cum[i - 1] + hypot(p.x[i] - p.x[i - 1], p.y[i] - p.y[i - 1])
        val total = if (cum[n - 1] > 0f) cum[n - 1] else 1f
        val tap = if (isClosed) 0f else Ink.taper * taper
        val jit = Ink.wobble * 0.22f * unit
        val lx = FloatArray(n); val ly = FloatArray(n); val rx = FloatArray(n); val ry = FloatArray(n)
        var m = 0
        for (i in 0 until n) {
            val t = cum[i] / total
            if (t < t0 || t > t1) continue
            val tt = (t - t0) / (if (t1 - t0 > 0f) t1 - t0 else 1f)
            val a = if (i > 0) i - 1 else if (isClosed) n - 2 else i
            val b = if (i < n - 1) i + 1 else if (isClosed) 1 else i
            var tx = p.x[b] - p.x[a]; var ty = p.y[b] - p.y[a]
            val tl = hypot(tx, ty).let { if (it > 0f) it else 1f }
            tx /= tl; ty /= tl
            val nx = -ty; val ny = tx
            var w = Ink.lineWidth * this.w * wScale * unit * (0.72f + 0.56f * noise1(i * 0.09 + seed).toFloat())
            if (tap > 0f) w *= min(1f, tt / tap).pow(0.7f) * min(1f, (1f - tt) / tap).coerceAtLeast(0f).pow(0.7f)
            persp?.let { w *= it(p.x[i], p.y[i]) }
            w = max(w, 0.08f * unit)
            val j = (noise1(i * 0.35 + seed * 1.7 + tick * 13.1) - 0.5).toFloat() * jit + off
            val cx = p.x[i] + nx * j; val cy = p.y[i] + ny * j
            lx[m] = cx + nx * w / 2; ly[m] = cy + ny * w / 2
            rx[m] = cx - nx * w / 2; ry[m] = cy - ny * w / 2
            m++
        }
        if (m < 2) return
        out.moveTo(lx[0], ly[0])
        for (i in 1 until m) out.lineTo(lx[i], ly[i])
        if (isClosed) {
            out.close()
            out.moveTo(rx[m - 1], ry[m - 1])
            for (i in m - 2 downTo 0) out.lineTo(rx[i], ry[i])
            out.close()
            out.fillType = PathFillType.EvenOdd
        } else {
            for (i in m - 1 downTo 0) out.lineTo(rx[i], ry[i])
            out.close()
        }
    }
}
