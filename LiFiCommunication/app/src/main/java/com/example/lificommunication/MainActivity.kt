package com.example.lificommunication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
    //открыть экран для режима отправки
    fun sendActivity (view: View) {
        val sendIntent = Intent(this, MainActivitySend::class.java)
        startActivity(sendIntent)
    }
    //открыть экран для режима приема
    fun receiveActivity (view: View) {
        val receiveIntent = Intent(this, MainActivityReceive::class.java)
        startActivity(receiveIntent)
    }
    fun settingActivity (view: View) {
        val settingIntent = Intent(this, MainActivitySetting::class.java)
        startActivity(settingIntent)
    }
    //открыть экран для спраки
    fun spravkaActivity (view: View) {
        val spravkaIntent = Intent(this, MainActivitySpravka::class.java)
        startActivity(spravkaIntent)
    }

}
