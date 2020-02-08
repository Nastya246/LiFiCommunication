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
    fun SendActivity (view: View) {
        val sendIntent = Intent(this, MainActivitySend::class.java)
        startActivity(sendIntent)

    }
    //открыть экран для режима приема
    fun RecieveActivity (view: View) {
        val recieveIntent = Intent(this, MainActivityRecieve::class.java)
        startActivity(recieveIntent)

    }
    fun SettingActivity (view: View) {
        val SettingIntent = Intent(this, MainActivitySetting::class.java)
        startActivity(SettingIntent)

    }
    //открыть экран для спраки
    fun SpravkaActivity (view: View) {
        val SpravkaIntent = Intent(this, MainActivitySpravka::class.java)
        startActivity(SpravkaIntent)

    }

}
