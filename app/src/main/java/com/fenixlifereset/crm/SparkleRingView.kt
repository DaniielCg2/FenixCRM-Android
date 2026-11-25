package com.fenixlifereset.crm

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SparkleRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class DustParticle(
        val angleDeg: Float,
        val radiusOffset: Float,
        val sizePx: Float,
        val phase: Float,
        val speedFactor: Float,
        val color: Int
    )

    private data class StarSparkle(
        val angleDeg: Float,
        val radiusFactor: Float,
        val maxSize: Float,
        val phase: Float
    )

    private var rotationAngle = 0f
    private var twinklePhase = 0f
    private var animator: ValueAnimator? = null

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringOuterGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val ringMainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dustPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFF4D2")
    }

    private val sweepMatrix = Matrix()
    private var ringSweepShader: SweepGradient? = null
    private var ambientGlowShader: RadialGradient? = null
    private val starPath = Path()

    private val dustParticles: List<DustParticle>
    private val stars: List<StarSparkle>

    init {
        val rng = Random(42)
        val palette = intArrayOf(
            Color.parseColor("#FFF2C6"),
            Color.parseColor("#FFD27D"),
            Color.parseColor("#FF9E3D"),
            Color.parseColor("#F5D6A9")
        )
        dustParticles = List(88) {
            DustParticle(
                angleDeg = rng.nextFloat() * 360f,
                radiusOffset = (rng.nextFloat() - 0.5f) * 34f,
                sizePx = 1.4f + rng.nextFloat() * 3.0f,
                phase = rng.nextFloat() * 6.28f,
                speedFactor = 0.65f + rng.nextFloat() * 0.75f,
                color = palette[rng.nextInt(palette.size)]
            )
        }
        stars = listOf(
            StarSparkle(32f, 1.15f, 13f, 0.2f),
            StarSparkle(85f, 1.12f, 9f, 1.5f),
            StarSparkle(138f, 1.18f, 11f, 2.7f),
            StarSparkle(195f, 1.14f, 10f, 3.9f),
            StarSparkle(242f, 1.17f, 14f, 4.8f),
            StarSparkle(312f, 1.20f, 15f, 5.4f)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val cy = h / 2f
        val baseRadius = minOf(w, h) * 0.38f

        ambientGlowShader = RadialGradient(
            cx,
            cy,
            baseRadius * 1.45f,
            intArrayOf(
                Color.parseColor("#44D94E1F"),
                Color.parseColor("#22F28B30"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.45f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = ambientGlowShader

        ringSweepShader = SweepGradient(
            cx,
            cy,
            intArrayOf(
                Color.parseColor("#C94214"),
                Color.parseColor("#FF9E3D"),
                Color.parseColor("#FFF4D2"),
                Color.parseColor("#FF8C2E"),
                Color.parseColor("#9C2F0E"),
                Color.parseColor("#C94214")
            ),
            floatArrayOf(0f, 0.18f, 0.28f, 0.42f, 0.75f, 1f)
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimationLoop()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private fun startAnimationLoop() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 9000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                rotationAngle = anim.animatedValue as Float
                twinklePhase = (rotationAngle / 360f) * (Math.PI.toFloat() * 6f)
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.37f
        val density = resources.displayMetrics.density

        // 1. Resplandor cálido de fondo
        canvas.drawCircle(cx, cy, radius * 1.45f, glowPaint)

        // 2. Anillo luminoso principal giratorio
        ringSweepShader?.let { shader ->
            sweepMatrix.reset()
            sweepMatrix.setRotate(rotationAngle, cx, cy)
            shader.setLocalMatrix(sweepMatrix)

            ringOuterGlowPaint.shader = shader
            ringOuterGlowPaint.strokeWidth = 9f * density
            ringOuterGlowPaint.alpha = 95
            canvas.drawCircle(cx, cy, radius, ringOuterGlowPaint)

            ringMainPaint.shader = shader
            ringMainPaint.strokeWidth = 3.2f * density
            ringMainPaint.alpha = 255
            canvas.drawCircle(cx, cy, radius, ringMainPaint)
        }

        // 3. Partículas de escarcha dorada girando alrededor
        for (p in dustParticles) {
            val currentDeg = p.angleDeg + rotationAngle * p.speedFactor
            val rad = Math.toRadians(currentDeg.toDouble())
            // Ligera órbita elíptica para efecto de estela envolvente
            val orbitRx = radius + p.radiusOffset * density * 0.65f + (sin(rad * 2.0).toFloat() * 8f * density)
            val orbitRy = radius + p.radiusOffset * density * 0.5f
            val px = cx + (cos(rad) * orbitRx).toFloat()
            val py = cy + (sin(rad) * orbitRy).toFloat()

            val pulse = (0.35f + 0.65f * ((sin((twinklePhase + p.phase).toDouble()).toFloat() + 1f) * 0.5f))
            dustPaint.color = p.color
            dustPaint.alpha = (pulse * 235).toInt().coerceIn(40, 255)
            canvas.drawCircle(px, py, p.sizePx * density * 0.55f, dustPaint)
        }

        // 4. Estrellitas de 4 puntas destellando alrededor del círculo
        for (s in stars) {
            val deg = s.angleDeg + rotationAngle * 0.35f
            val rad = Math.toRadians(deg.toDouble())
            val sx = cx + (cos(rad) * radius * s.radiusFactor).toFloat()
            val sy = cy + (sin(rad) * radius * s.radiusFactor).toFloat()

            val twinkle = 0.45f + 0.55f * ((sin((twinklePhase * 1.2f + s.phase).toDouble()).toFloat() + 1f) * 0.5f)
            val size = s.maxSize * density * 0.65f * twinkle
            starPaint.alpha = (twinkle * 255).toInt().coerceIn(70, 255)
            drawFourPointStar(canvas, sx, sy, size)
        }
    }

    private fun drawFourPointStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val inner = r * 0.22f
        starPath.reset()
        starPath.moveTo(cx, cy - r)
        starPath.lineTo(cx + inner, cy - inner)
        starPath.lineTo(cx + r, cy)
        starPath.lineTo(cx + inner, cy + inner)
        starPath.lineTo(cx, cy + r)
        starPath.lineTo(cx - inner, cy + inner)
        starPath.lineTo(cx - r, cy)
        starPath.lineTo(cx - inner, cy - inner)
        starPath.close()
        canvas.drawPath(starPath, starPaint)
    }
}
