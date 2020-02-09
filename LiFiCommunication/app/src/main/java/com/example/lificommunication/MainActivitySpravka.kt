package com.example.lificommunication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivitySpravka : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_spravka)
    }
    //открыть экран для режима отправки
    fun SendMain (view: View) {
        val sendMain = Intent(this, MainActivity::class.java)
        startActivity(sendMain)
    }
}
