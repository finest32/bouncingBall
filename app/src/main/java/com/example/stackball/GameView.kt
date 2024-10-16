package com.example.bouncingball

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    private var running = false

    // Параметры для шарика
    private var ballY = 0f
    private var ballSpeed = 0f
    private val ballRadius = 50f
    private val ballPaint = Paint()
    private val ballShadowPaint = Paint()

    // Параметры для платформ
    private val platforms = mutableListOf<Platform>()
    private var level = 1
    private val maxLevel = 10

    // Параметры для очков
    private var score = 0
    private var bestScore = 0 // Добавление переменной для хранения лучшего результата
    private val scorePaint = Paint().apply {
        color = Color.BLACK
        textSize = 70f
        textAlign = Paint.Align.CENTER
    }

    private val scoreShadowPaint = Paint().apply {
        color = Color.GRAY
        textSize = 70f
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    private val bestScorePaint = Paint().apply {
        color = Color.BLACK
        textSize = 70f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true // Добавлено сглаживание текста
    }

    private val bestScoreShadowPaint = Paint().apply {
        color = Color.GRAY
        textSize = 70f
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
        isAntiAlias = true // Добавлено сглаживание текста
    }


    private val handler = Handler(Looper.getMainLooper())
    private var jumpEnabled = true

    // Прогресс уровня
    private var currentLevelProgress = 0
    private val maxLevelProgress = 100

    init {
        holder.addCallback(this)
        setupBallPaint()
        setupShadowPaint()
        loadBestScore() // Загрузка лучшего результата при инициализации
    }

    private fun setupBallPaint() {
        // Изменяем градиент для создания ярко-красного цвета
        val gradient = RadialGradient(
            ballRadius, ballRadius, ballRadius,
            Color.parseColor("#FF0000"), // Яркий красный
            Color.parseColor("#FF4040"), // Более светлый красный
            Shader.TileMode.CLAMP
        )
        ballPaint.shader = gradient
    }


    private fun setupShadowPaint() {
        ballShadowPaint.color = Color.parseColor("#80000000")
        ballShadowPaint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resetGame()
        running = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun run() {
        while (running) {
            if (holder.surface.isValid) {
                val canvas = holder.lockCanvas()
                canvas?.let {
                    updateGame()
                    drawGame(it)
                    holder.unlockCanvasAndPost(it)
                }
            }
        }
    }

    private fun updateGame() {
        ballY += ballSpeed
        ballSpeed += 1.5f

        if (ballY > 0) {
            currentLevelProgress = ((ballY / (height - ballRadius)) * maxLevelProgress).toInt()
        }

        val destroyedPlatforms = mutableListOf<Platform>()
        for (i in platforms.indices) {
            val platform = platforms[i]
            if (ballY + ballRadius > platform.y && ballY - ballRadius < platform.y + platform.height) {
                if (platform.isBlack) {
                    if (platform.isBreakable) {
                        destroyedPlatforms.add(platform)
                    } else {
                        handleGameOver() // Обработка проигрыша
                        return
                    }
                } else if (!jumpEnabled) {
                    destroyedPlatforms.add(platform)
                } else {
                    ballSpeed = -15f
                    ballY = platform.y - ballRadius
                    break
                }
            }

            // Применяем изменение цвета только при приближении шарика к черной платформе
            if (platform.isBlack && !platform.isBreakable) {
                checkBallProximityAndTriggerChange(platform)
            }
        }

        for (platform in destroyedPlatforms) {
            platforms.remove(platform)
            score += 10
        }

        if (ballY - ballRadius > height) {
            nextLevel()
        }

        if (jumpEnabled && ballY + ballRadius < height && ballSpeed >= 0) {
            ballY -= 2f
        }
    }

    // Проверка близости шарика к платформе и смена цвета, если шарик близко
    private fun checkBallProximityAndTriggerChange(platform: Platform) {
        val proximityThreshold = 250f // расстояние, при котором шарик считается "близко" к платформе

        // Проверяем, находится ли шарик в пределах proximityThreshold от платформы
        if (Math.abs(ballY - platform.y) <= proximityThreshold) {
            triggerBlackPlatformChange(platform)
        }
    }

    // Функция для изменения цвета черной платформы на желтый через 1-4 секунды
    private fun triggerBlackPlatformChange(blackPlatform: Platform) {
        if (!blackPlatform.isBreakable) {
            val delay = Random.nextLong(1000, 4000) // Задержка от 1 до 4 секунд
            handler.postDelayed({
                blackPlatform.color = Color.YELLOW // Изменяем цвет на желтый
                blackPlatform.isBreakable = true // Делаем платформу разрушаемой
            }, delay)
        }
    }

    private fun saveLevel() {
        val sharedPref = context.getSharedPreferences("game_data", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("level", level)
            apply()
        }
    }

    private fun loadLevel() {
        val sharedPref = context.getSharedPreferences("game_data", Context.MODE_PRIVATE)
        level = sharedPref.getInt("level", 1) // Если данных нет, начинаем с 1 уровня
    }

    private fun loadBestScore() {
        val sharedPref = context.getSharedPreferences("game_data", Context.MODE_PRIVATE)
        bestScore = sharedPref.getInt("best_score", 0) // Загрузка лучшего результата
    }

    private fun saveBestScore() {
        val sharedPref = context.getSharedPreferences("game_data", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("best_score", bestScore) // Сохранение лучшего результата
            apply()
        }
    }

    private fun handleGameOver() {
        // Сохраняем лучший результат, если текущий больше
        if (score > bestScore) {
            bestScore = score
            saveBestScore()
        }
        // Вместо сброса уровня на 1, оставляем уровень, на котором проиграли
        ballY = 0f
        ballSpeed = 0f
        score = 0
        currentLevelProgress = 0
        setupPlatforms()
    }


    // Метод для сброса игры
    private fun resetGame() {
        loadLevel()
        // Устанавливаем текущее значение уровня на последний проигранный
        ballY = 0f
        ballSpeed = 0f
        score = 0
        currentLevelProgress = 0
        setupPlatforms()
    }


    private fun nextLevel() {
        saveLevel()
        level++
        ballY = 0f
        ballSpeed = 0f
        currentLevelProgress = 0
        setupPlatforms()
    }

    private fun setupPlatforms() {
        platforms.clear()
        val platformHeight = 50f
        val spacing = 30f
        val availableHeight = height - 400f
        val numberOfPlatforms = (availableHeight / (platformHeight + spacing)).toInt()

        val startY = 400f
        var yellowCount = 0

        for (i in 0 until numberOfPlatforms) {
            val isBlack = if (yellowCount >= 3 && Random.nextInt(0, 4) == 0) {
                yellowCount = 0
                true
            } else {
                false
            }

            val color = if (isBlack) Color.BLACK else Color.parseColor("#FFC107")
            platforms.add(Platform(0f, startY + i * (platformHeight + spacing), width.toFloat(), platformHeight, color, isBlack, isBreakable = false))

            if (!isBlack) yellowCount++
        }
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        canvas.drawCircle(width / 2f, ballY + 10, ballRadius, ballShadowPaint)
        canvas.drawCircle(width / 2f, ballY, ballRadius, ballPaint)

        for (platform in platforms) {
            drawPlatform(canvas, platform)
        }

        drawLevelProgressBar(canvas)
        drawScore(canvas)
        drawBestScore(canvas) // Вызов функции для отрисовки лучшего результата
    }

    private fun drawPlatform(canvas: Canvas, platform: Platform) {
        val paint = Paint().apply {
            color = platform.color
        }
        canvas.drawRect(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height, paint)
    }

    private fun drawScore(canvas: Canvas) {
        canvas.drawText("Score: $score", width / 2f, 200f, scoreShadowPaint)
        canvas.drawText("Score: $score", width / 2f, 200f, scorePaint)
    }

    private fun drawBestScore(canvas: Canvas) {
        // Отрисовка текста для лучшего результата
        canvas.drawText("Best score: $bestScore", width / 2f, 300f, bestScoreShadowPaint)
        canvas.drawText("Best score: $bestScore", width / 2f, 300f, bestScorePaint)
    }

    private fun drawLevelProgressBar(canvas: Canvas) {
        val progressBarHeight = 50f
        val margin = 50f

        val progressBarRect = RectF(margin, margin, width - margin, margin + progressBarHeight)
        val progressPaint = Paint().apply {
            color = Color.LTGRAY
        }
        canvas.drawRect(progressBarRect, progressPaint)

        val progressWidth = (width - margin * 2) * currentLevelProgress / maxLevelProgress
        val progressRect = RectF(margin, margin, margin + progressWidth, margin + progressBarHeight)
        progressPaint.color = Color.CYAN
        canvas.drawRect(progressRect, progressPaint)

        drawLevelCircles(canvas)
    }

    private fun drawLevelCircles(canvas: Canvas) {
        val circleRadius = 40f
        val circleX1 = 150f
        val circleX2 = width - 150f
        val circleY = 75f

        // Изменяем цвет на темно-серый, ближе к черному
        val circlePaint = Paint().apply {
            color = Color.parseColor("#333333") // Темно-серый, ближе к черному
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f // Размер шрифта
            textAlign = Paint.Align.CENTER
            setShadowLayer(5f, 0f, 0f, Color.BLACK) // Добавляем тень
            isAntiAlias = true
        }

        // Рисуем круги для уровней
        canvas.drawCircle(circleX1, circleY, circleRadius, circlePaint)
        canvas.drawCircle(circleX2, circleY, circleRadius, circlePaint)

        // Вычисляем Y-координату для выравнивания текста по центру
        val textY1 = circleY - (textPaint.descent() + textPaint.ascent()) / 2
        val textY2 = circleY - (textPaint.descent() + textPaint.ascent()) / 2

        // Отображаем текущий уровень слева и следующий уровень справа
        canvas.drawText("$level", circleX1, textY1, textPaint) // Текущий уровень
        canvas.drawText("${level + 1}", circleX2, textY2, textPaint) // Следующий уровень
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                jumpEnabled = false
                ballSpeed = 30f
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                jumpEnabled = true
                ballSpeed = 15f
            }
        }
        return super.onTouchEvent(event)
    }

    data class Platform(val x: Float,
                        val y: Float,
                        val width: Float,
                        val height: Float,
                        var color: Int,
                        val isBlack: Boolean,
                        var isBreakable: Boolean)
}
