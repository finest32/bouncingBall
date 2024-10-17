package com.example.bouncingball

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bouncingball.GameView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем кастомное представление игры
        setContentView(GameView(this))
    }

}