package com.example.lificommunication
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_setting.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivitySetting : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileInputDataUser: FileInputStream = openFileInput("dataUser.txt") // файл с именем уст-ва и паролем
        val inputStreamFileUser = InputStreamReader(fileInputDataUser) //
        val inputBuffer = CharArray(22)
        inputStreamFileUser.read(inputBuffer)
        val dataNamePass = String(inputBuffer) // получили строку с данныими
        var arrayDataNamePass=dataNamePass.split(',')
        setContentView(R.layout.activity_main_setting)
        editTextNameDevice.text.append(arrayDataNamePass[1])
        editTextPassword.text.append(arrayDataNamePass[0])
    }
    //открыть экран для режима приема
    fun mainActivity (view: View) {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
    }
    fun saveDeviceSettings(view: View) {
        //получение имени устройства
        val deviceName = editTextNameDevice.text.toString()
        if (deviceName.length > 10) {
            val showWarning = Toast.makeText(
                this,
                "Имя устройства должно быть короче 11 символов.",
                Toast.LENGTH_SHORT
            )
            showWarning.setGravity(Gravity.CENTER, 0, 0)
            showWarning.show()
            return
        } else if (deviceName.length < 4) {
            val showWarning = Toast.makeText(
                this,
                "Имя устройства должно быть длиннее 3 символов.",
                Toast.LENGTH_SHORT
            )
            showWarning.setGravity(Gravity.CENTER, 0, 0)
            showWarning.show()
            return
        }
        //получение пароля для передачи
        val devicePassword = editTextPassword.text.toString()
        if (devicePassword.length < 5) {
            val showWarning = Toast.makeText(
                this,
                "Пароль устройства должен быть длинее 4 символов.",
                Toast.LENGTH_SHORT
            )
            showWarning.setGravity(Gravity.CENTER, 0, 0)
            showWarning.show()
            return
        } else if (devicePassword.length > 10) {
            val showWarning = Toast.makeText(
                this,
                "Пароль устройства должен быть менее 11 символов.",
                Toast.LENGTH_SHORT
            )
            showWarning.setGravity(Gravity.CENTER, 0, 0)
            showWarning.show()
            return
        }
            val fileOut: FileOutputStream = openFileOutput("dataUser.txt", Context.MODE_PRIVATE)
            val outputStreamFile = OutputStreamWriter(fileOut)
            outputStreamFile.write(devicePassword + "," + deviceName+",")  // записываем строку c именем устройства и паролем  в файл
            outputStreamFile.flush()// проверяем, что все записалось в файл
            outputStreamFile.close() //закрываем поток
        val showWarning = Toast.makeText(
            this,
            "Данные успешно сохранены!",
            Toast.LENGTH_SHORT
        )
        showWarning.setGravity(Gravity.CENTER, 0, 0)
        showWarning.show()
            //Проверка корректности полученных данных для нас
            /*val showName = Toast.makeText(this, deviceName, Toast.LENGTH_SHORT)
        showName.show()
        val showPass = Toast.makeText(this, devicePassword, Toast.LENGTH_SHORT)
        showPass.show()*/
    }
}
