package com.example.bouncingball

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import com.example.bouncingball.R
import com.example.bouncingball.GameView
import com.google.androidgamesdk.GameActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Инициализация ImageView
        val imageView: ImageView = findViewById(R.id.splashImage)
        imageView.setImageBitmap(BitmapFactory.decodeResource(resources, R.drawable.logo32))

        // Задержка в 3 секунды
        Handler().postDelayed({
            val intent = Intent(this, GameView::class.java)
            startActivity(intent)
            finish() // Закрыть экран загрузки
        }, 3000) // 3000 миллисекунд = 3 секунды
    }
}
