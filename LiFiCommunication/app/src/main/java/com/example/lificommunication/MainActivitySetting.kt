package com.example.lificommunication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main_setting.*

class MainActivitySetting : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_setting)
    }

    fun SaveDeviceSettings (view: View) {
        //TODO::Добавить преобразование и шифрование пароля
        //получение имени устройства
        val deviceName = editTextNameDevice.text.toString()

        if (deviceName.length > 16) {
            val showWarning = Toast.makeText(this, "Имя устройства должно быть короче 17 символов.", Toast.LENGTH_SHORT)
            showWarning.show()
            return
        }

        //получение пароля для передачи
        val devicePassword = editTextPassword.text.toString()

        if (devicePassword.length < 8) {
            val showWarning = Toast.makeText(this, "Пароль устройства должен быть длинее 8 символов.", Toast.LENGTH_SHORT)
            showWarning.show()
            return
        }

        //Проверка корректности полученных данных для нас
        /*val showName = Toast.makeText(this, deviceName, Toast.LENGTH_SHORT)
        showName.show()
        val showPass = Toast.makeText(this, devicePassword, Toast.LENGTH_SHORT)
        showPass.show()*/
    }
}
