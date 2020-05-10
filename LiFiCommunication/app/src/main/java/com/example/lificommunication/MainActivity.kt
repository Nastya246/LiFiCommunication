package com.example.lificommunication

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setMicrophoneMute(true)
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
