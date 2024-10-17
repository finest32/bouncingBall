package com.example.bouncingball

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import com.example.bouncingball.R

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Инициализация ImageView
        val imageView: ImageView = findViewById(R.id.splashImage)
        imageView.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo32))

        // Скрываем логотип при запуске
        imageView.visibility = ImageView.INVISIBLE

        // Показываем логотип через 2 секунды
        Handler(Looper.getMainLooper()).postDelayed({
            imageView.visibility = ImageView.VISIBLE
        }, 1000) // 1000 миллисекунд

        // Задержка перед переходом на другой экран
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, com.example.bouncingball.GameActivity::class.java)
            startActivity(intent)
            finish() // Закрыть экран загрузки
        }, 5000) // 5000 миллисекунд
    }
}
