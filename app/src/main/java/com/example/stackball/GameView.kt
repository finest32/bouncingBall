package com.example.stackball

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
    private var ballSpeed = 0f // Скорость шарика
    private val ballRadius = 50f
    private val ballPaint = Paint()
    private val ballShadowPaint = Paint()

    // Параметры для платформ
    private val platforms = mutableListOf<Platform>()
    private var level = 1
    private val maxLevel = 10 // Максимальный уровень

    // Параметры для очков
    private var score = 0
    private val scorePaint = Paint().apply {
        color = Color.BLACK
        textSize = 70f
        textAlign = Paint.Align.CENTER
    }

    // Paint для тени текста
    private val scoreShadowPaint = Paint().apply {
        color = Color.GRAY
        textSize = 70f
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    // Handler для запуска таймеров
    private val handler = Handler(Looper.getMainLooper())

    // Флаг для отслеживания состояния прыжков
    private var jumpEnabled = true

    // Прогресс уровня
    private var currentLevelProgress = 0 // Прогресс в текущем уровне (в процентах)
    private val maxLevelProgress = 100 // Максимальный прогресс для уровня (100%)

    // Инициализация
    init {
        holder.addCallback(this)
        setupBallPaint()
        setupShadowPaint()
    }

    private fun setupBallPaint() {
        // Устанавливаем градиент для красного шарика
        val gradient = RadialGradient(
            ballRadius, ballRadius, ballRadius,
            Color.parseColor("#FF0000"), // Красный цвет
            Color.parseColor("#FF7F7F"), // Светло-красный для градиента
            Shader.TileMode.CLAMP
        )
        ballPaint.shader = gradient
    }

    private fun setupShadowPaint() {
        ballShadowPaint.color = Color.parseColor("#80000000") // Полупрозрачный черный для тени
        ballShadowPaint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL) // Размытие для тени
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resetGame() // Сброс игры при создании поверхности
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

    // Основной игровой цикл
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
        // Двигаем шарик вниз с заданной скоростью
        ballY += ballSpeed
        ballSpeed += 1.5f // Увеличиваем скорость падения

        // Обновляем текущий прогресс уровня
        if (ballY > 0) {
            currentLevelProgress = ((ballY / (height - ballRadius)) * maxLevelProgress).toInt()
        }

        // Проверка на столкновение с платформами
        val destroyedPlatforms = mutableListOf<Platform>() // Список разрушенных платформ
        for (platform in platforms) {
            if (ballY + ballRadius > platform.y && ballY - ballRadius < platform.y + platform.height) {
                if (!jumpEnabled) { // Если прыжки отключены
                    destroyedPlatforms.add(platform) // Добавляем платформу в список разрушенных
                } else {
                    // Изменяем направление скорости шарика при столкновении
                    ballSpeed = -15f // Устанавливаем скорость вверх при столкновении
                    // Корректируем положение шарика, чтобы он не проваливался сквозь платформу
                    ballY = platform.y - ballRadius
                    break // Выходим из цикла, чтобы не обрабатывать дальше
                }
            }
        }

        // Удаляем разрушенные платформы и увеличиваем счёт
        for (platform in destroyedPlatforms) {
            platforms.remove(platform)
            score += 10 // Увеличиваем счёт на 10 за каждую разбитую платформу
        }

        // Если шарик достиг нижней границы экрана, начинается новый уровень
        if (ballY - ballRadius > height) {
            nextLevel() // Переход на новый уровень
        }

        // Если прыжки включены, то поднимаем шарик на 2 см
        if (jumpEnabled && ballY + ballRadius < height && ballSpeed >= 0) {
            ballY -= 2f // Подлетаем на 2 см
        }
    }

    // Переход на новый уровень
    private fun nextLevel() {
        level++
        ballY = 0f
        ballSpeed = 0f // Сбрасываем скорость при переходе на новый уровень
        currentLevelProgress = 0 // Сбрасываем прогресс уровня
        setupPlatforms()
    }

    // Сброс игры
    private fun resetGame() {
        level = 1
        ballY = 0f
        ballSpeed = 0f
        score = 0
        currentLevelProgress = 0 // Сброс прогресса уровня
        setupPlatforms()
    }

    // Метод для установки платформ
    private fun setupPlatforms() {
        platforms.clear()
        val platformHeight = 50f
        val spacing = 30f
        val availableHeight = height - 400f
        val numberOfPlatforms = (availableHeight / (platformHeight + spacing)).toInt()

        val startY = 400f
        var yellowCount = 0 // Счетчик желтых платформ

        for (i in 0 until numberOfPlatforms) {
            val isBlack = if (yellowCount >= 3 && Random.nextInt(0, 4) == 0) {
                yellowCount = 0 // Сбрасываем счетчик после добавления черной платформы
                true
            } else {
                false
            }

            // Изменено с Color.YELLOW на Color.parseColor("#800080") для фиолетового
            val color = if (isBlack) Color.BLACK else Color.parseColor("#800080") // Фиолетовый цвет
            platforms.add(Platform(0f, startY + i * (platformHeight + spacing), width.toFloat(), platformHeight, color, isBlack))

            if (!isBlack) yellowCount++ // Увеличиваем счетчик, если платформа фиолетовая
        }
    }

    // Рисование игры
    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)

        // Рисуем тень шарика
        canvas.drawCircle(width / 2f, ballY + 10, ballRadius, ballShadowPaint) // Тень чуть ниже шарика
        // Рисуем шарик
        canvas.drawCircle(width / 2f, ballY, ballRadius, ballPaint)

        for (platform in platforms) {
            drawPlatform(canvas, platform)
        }

        drawLevelProgressBar(canvas)
        drawScore(canvas)
    }

    // Рисуем платформу
    private fun drawPlatform(canvas: Canvas, platform: Platform) {
        val paint = Paint().apply {
            color = platform.color
        }
        canvas.drawRect(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height, paint)
    }

    // Рисуем очки
    private fun drawScore(canvas: Canvas) {
        // Отображаем текст "Score"
        canvas.drawText("Score: $score", width / 2f, 250f, scoreShadowPaint) // Добавили тень к тексту
        canvas.drawText("Score: $score", width / 2f, 250f, scorePaint) // Основной текст
    }

    // Рисуем полоску прогресса
    private fun drawLevelProgressBar(canvas: Canvas) {
        val progressBarHeight = 50f
        val margin = 50f

        val progressBarRect = RectF(margin, margin, width - margin, margin + progressBarHeight)
        val progressPaint = Paint().apply {
            color = Color.LTGRAY
        }
        canvas.drawRect(progressBarRect, progressPaint)

        // Вычисляем ширину прогресса на основе текущего уровня и прогресса уровня
        val progressWidth = (width - margin * 2) * currentLevelProgress / maxLevelProgress
        val progressRect = RectF(margin, margin, margin + progressWidth, margin + progressBarHeight)
        progressPaint.color = Color.CYAN
        canvas.drawRect(progressRect, progressPaint)

        drawLevelCircles(canvas)
    }

    // Рисуем круги уровней
    private fun drawLevelCircles(canvas: Canvas) {
        val circleRadius = 40f
        val circleX1 = 150f
        val circleX2 = width - 150f
        val circleY = 75f

        // Изменяем цвет на почти черный, но слегка сероватый
        val circlePaint = Paint().apply {
            color = Color.parseColor("#3A3A3A") // Цвет: почти черный (серый)
        }
        canvas.drawCircle(circleX1, circleY, circleRadius, circlePaint)
        canvas.drawCircle(circleX2, circleY, circleRadius, circlePaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(level.toString(), circleX1, circleY + 20, textPaint)
        canvas.drawText((level + 1).toString(), circleX2, circleY + 20, textPaint)
    }

    // Обрабатываем касания
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Отключаем прыжки и устанавливаем скорость вниз
                jumpEnabled = false
                ballSpeed = 30f // Устанавливаем скорость падения
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Включаем прыжки
                jumpEnabled = true
                ballSpeed = 15f // Устанавливаем скорость для прыжка
            }
        }
        return super.onTouchEvent(event)
    }

    // Класс для платформ
    data class Platform(val x: Float, val y: Float, val width: Float, val height: Float, val color: Int, val isBlack: Boolean)
}