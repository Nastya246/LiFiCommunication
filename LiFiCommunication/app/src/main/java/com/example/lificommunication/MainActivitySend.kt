package com.example.lificommunication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main_send.*
import java.io.File
import java.lang.Exception
import java.util.jar.Manifest
import android.widget.TextView
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class MainActivitySend : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_send)
    }

    val arrayFiles = arrayOfNulls<String>(3)
    var count: Int = 0

    fun AddFile(view: View) {
        val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)//Выбор любого типа файла

        try {
            startActivityForResult(Intent.createChooser(intent, "Выбирите файл..."), 111)
        } catch (e: Exception) {
            Toast.makeText(this, "Пожалуйста установите файловый менеджер.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            var selectedFile = data?.data //The uri with the location of the file

            var matchResult = Regex("""([^%2F]*)$""").find(selectedFile.toString())

            if (matchResult === null) return

            if (count >= 3) {
                Toast.makeText(this, "Вы не можете выбрать больше трех файлов.", Toast.LENGTH_SHORT).show()
                return
            }

            arrayFiles.set(count, selectedFile.toString())
            count++

            if(count === 1) InfoAddFiles.text = matchResult.value
            else {
                InfoAddFiles.append(System.getProperty("line.separator"))
                InfoAddFiles.append(matchResult.value)
            }
        }
    }
}
