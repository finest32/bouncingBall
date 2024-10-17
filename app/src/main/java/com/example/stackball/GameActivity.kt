package com.example.bouncingball

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем ваше SurfaceView (GameView) как контент для GameActivity
        val gameView = GameView(this)
        setContentView(gameView)
    }
}
