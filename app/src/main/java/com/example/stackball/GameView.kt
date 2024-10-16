package com.example.stackball

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class пGameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    private var running = false

    // Параметры для шарика
    private var ballY = 0f
    private var ballSpeed = 0f // Шарик не движется, пока не коснемся экрана
    private val ballRadius = 50f
    private val ballPaint = Paint().apply {
        color = Color.RED
    }

    // Параметры для платформ
    private val platforms = mutableListOf<Platform>()
    private var level = 1
    private var screenOffsetY = 0f // Смещение экрана по оси Y для прокрутки
    private var allowScreenScroll = true // Управляет прокруткой экрана

    // Параметры черного сегмента
    private var blackAreaPosition = 0f // Начальная позиция черного сегмента
    private var blackAreaDirection = 1 // Направление движения черного сегмента
    private val blackPaint = Paint().apply {
        color = Color.BLACK
    }

    // Инициализация
    init {
        holder.addCallback(this)
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

    // Обновление логики игры
    private fun updateGame() {
        // Двигаем шарик только если есть скорость (при касании)
        ballY += ballSpeed

        // Прокрутка экрана вниз вместе с шариком, пока он не на последней платформе
        if (allowScreenScroll && ballY > height / 2) {
            screenOffsetY = ballY - height / 2
        }

        // Если шарик достиг нижней границы экрана и последняя платформа пройдена, даём ему упасть до конца экрана
        val lastPlatform = platforms.lastOrNull()
        if (lastPlatform != null && ballY - ballRadius > lastPlatform.y + lastPlatform.height) {
            allowScreenScroll = false // Останавливаем прокрутку экрана
        }

        // Если шарик достиг нижней границы экрана, начинается новый уровень
        if (ballY - ballRadius > height) {
            nextLevel() // Переход на новый уровень
        }

        // Проверка на столкновение с платформами
        for (platform in platforms) {
            if (ballY + ballRadius > platform.y && ballY - ballRadius < platform.y + platform.height) {
                if (ballSpeed > 0) {
                    // Удаляем платформу при столкновении
                    platforms.remove(platform)
                    break
                }
            }
        }
    }

    // Переход на новый уровень
    private fun nextLevel() {
        level++ // Увеличиваем уровень
        ballY = 160f // Возвращаем шарик в начальную позицию ниже полоски с прогрессом
        screenOffsetY = 0f // Сбрасываем смещение экрана
        allowScreenScroll = true // Разрешаем прокрутку экрана снова
        setupPlatforms() // Генерируем новые платформы для следующего уровня
    }

    // Сброс игры
    private fun resetGame() {
        level = 1 // Сбрасываем уровень
        ballY = 160f // Устанавливаем начальную позицию шарика ниже полоски с прогрессом
        ballSpeed = 0f // Обнуляем скорость
        screenOffsetY = 0f // Сбрасываем смещение экрана
        allowScreenScroll = true // Разрешаем прокрутку экрана
        setupPlatforms() // Генерируем платформы для первого уровня
    }

    // Рисование игры
    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)

        // Применяем смещение экрана по Y, если разрешена прокрутка
        if (allowScreenScroll) {
            canvas.translate(0f, -screenOffsetY)
        }

        // Рисуем шарик
        canvas.drawCircle(width / 2f, ballY, ballRadius, ballPaint)

        // Рисуем платформы
        for (i in platforms.indices) {
            draw3DPlatform(canvas, platforms[i], i)
        }

        // Возвращаем экран в исходное положение после рисования
        if (allowScreenScroll) {
            canvas.translate(0f, screenOffsetY)
        }

        // Рисуем полоску прогресса уровня — всегда наверху экрана
        drawLevelProgressBar(canvas)
    }

    // Обрабатываем касания для движения шарика
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                ballSpeed = 30f // Устанавливаем скорость при касании экрана
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                ballSpeed = 0f // Останавливаем шарик при отпускании экрана
            }
        }
        return super.onTouchEvent(event)
    }

    // Метод для создания платформ
    private fun setupPlatforms() {
        platforms.clear()
        val platformHeight = 100f // Высота платформы
        val platformSpacing = 50f // Расстояние между платформами
        val numberOfPlatforms = 20 // Количество платформ на уровне

        // Рассчитываем ширину платформы
        val fullWidth = width.toFloat()
        val platformWidth = fullWidth * 0.8f // Платформы занимают 80% ширины экрана

        // Задаем фиксированную Y-координату для появления платформ
        val platformStartY = 300f // Начальная Y-координата для самой верхней платформ

        for (i in 0 until numberOfPlatforms) {
            val color = Color.YELLOW // Все платформы желтые
            val platformY = platformStartY + i * (platformHeight + platformSpacing) // Позиция по оси Y

            // Создаем платформу и добавляем в список
            platforms.add(Platform(fullWidth / 2f, platformY, platformWidth, platformHeight, color))
        }
    }

    // Рисуем 3D платформу
    private fun draw3DPlatform(canvas: Canvas, platform: Platform, index: Int) {
        val platformDepth = 30f // Глубина для эффекта 3D

        // Координаты верхнего прямоугольника (верхняя плоскость)
        val topLeftX = platform.x - platform.width / 2
        val topRightX = platform.x + platform.width / 2
        val topY = platform.y

        // Координаты нижнего прямоугольника (нижняя плоскость с отступом для глубины)
        val bottomLeftX = topLeftX + platformDepth
        val bottomRightX = topRightX + platformDepth
        val bottomY = platform.y + platform.height + platformDepth

        // Создаем верхний прямоугольник
        val topRect = Path().apply {
            moveTo(topLeftX, topY)
            lineTo(topRightX, topY)
            lineTo(topRightX, platform.y + platform.height)
            lineTo(topLeftX, platform.y + platform.height)
            close()
        }

        // Создаем нижний прямоугольник
        val bottomRect = Path().apply {
            moveTo(bottomLeftX, bottomY)
            lineTo(bottomRightX, bottomY)
            lineTo(bottomRightX, platform.y + platform.height)
            lineTo(bottomLeftX, platform.y + platform.height)
            close()
        }

        // Боковые грани
        val sideRect = Path().apply {
            moveTo(topLeftX, topY)
            lineTo(topLeftX, platform.y + platform.height)
            lineTo(bottomLeftX, platform.y + platform.height + platformDepth)
            lineTo(bottomLeftX, bottomY)
            close()
        }

        // Задняя грань
        val backRect = Path().apply {
            moveTo(topRightX, topY)
            lineTo(topRightX, platform.y + platform.height)
            lineTo(bottomRightX, platform.y + platform.height + platformDepth)
            lineTo(bottomRightX, bottomY)
            close()
        }

        // Задаем цвета для верхней и боковой частей платформы
        val topPaint = Paint().apply { color = Color.rgb(150, 150, 255) }
        val sidePaint = Paint().apply { color = Color.rgb(100, 100, 200) }

        // Рисуем верхнюю часть
        canvas.drawPath(topRect, topPaint)

        // Рисуем боковые части
        canvas.drawPath(sideRect, sidePaint)
        canvas.drawPath(backRect, sidePaint)

        // === ЧЕРНЫЙ СЕГМЕНТ ЗМЕЙКОЙ ===
        val blackSegmentWidth = platform.width * 0.25f // Ширина черного сегмента

        // Расчет позиции черного сегмента с учетом "змейки"
        val direction = if (index % 2 == 0) 1 else -1 // Направление для "змейки"
        blackAreaPosition += blackAreaDirection * direction * 5 // Изменяем позицию черного сегмента

        // Ограничиваем движение черного сегмента внутри платформы
        val clampedPosition = blackAreaPosition.coerceIn(0f, platform.width - blackSegmentWidth)

        // Добавляем черную движущуюся область
        val blackAreaRect = RectF(
            platform.x - platform.width / 2 + clampedPosition,
            platform.y,
            platform.x - platform.width / 2 + clampedPosition + blackSegmentWidth,
            platform.y + platform.height
        )
        canvas.drawRect(blackAreaRect, blackPaint)
    }

    // Рисуем полоску прогресса уровня
    private fun drawLevelProgressBar(canvas: Canvas) {
        val progressBarHeight = 20f
        val progressBarY = 0f
        val progressBarWidth = width.toFloat() * 0.8f // 80% ширины экрана

        val progressBarRect = RectF(
            (width - progressBarWidth) / 2,
            progressBarY,
            (width + progressBarWidth) / 2,
            progressBarY + progressBarHeight
        )

        val progressPaint = Paint().apply {
            color = Color.GREEN
        }

        // Рисуем полоску уровня
        canvas.drawRect(progressBarRect, progressPaint)
    }
}

// Класс для платформы
data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Int
)

class GameView {

}
