package com.example.lificommunication

import android.content.Context
import android.content.Intent
import android.media.*
import android.media.AudioManager.*
import android.os.Build
import android.os.Bundle
import android.os.Process.setThreadPriority
import android.view.Gravity
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_recieve.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*

class MainActivityReceive: AppCompatActivity() {

    private var countPartFile: Int = 0 //для отсечения имени файла и формата от данных
    private var countFiles: Int = 0 //для подсчета количества файлов
    private var resultFileNameDecoder: String = "" //для хранения данных и записи в файл
    private var resultStrDecoder: String = "" //для хранения данных и записи в файл

    private var audioRunning = false //ключ для отслеживания окончания приема
    private var flagReceive = false //флаг для отслеживания готово ли устройство к приему
    private lateinit  var switch: Switch //для отслеживания нажатий и автоматического отключения свича

    private var userLogin = false //для проверки логина с передающего устройства
    private var userPassword = false //для проверки пароля с передающего устройства
    private var countUserInfo = 0 //для подсчета количества посылок авторизации
    private var resultUnitNameDecoder: String = "" //для хранения имени устройства
    private var resultUnitPasswordDecoder: String = "" //для хранения пароля устройства

    private var dataNamePassword = "" //считываемые данные из файла
    private var password = "" //пароль заданный пользователем
    private var nameDevice = "" //имя заданное пользователем



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_recieve)

        switch = findViewById<Switch>(R.id.switchReceive)
        val textView = findViewById<TextView>(R.id.nameDeviceConnectReceive) //вывод данных по нажатию на утсройство

        try { //проверка существует ли файл с логином и паролем
            val fileInputDataUser: FileInputStream = openFileInput("dataUser.txt") //файл с именем уст-ва и паролем
            val inputStreamFileUser = InputStreamReader(fileInputDataUser) //поток для чтения
            val inputBuffer = CharArray(22) //максимальный размер буфера
            inputStreamFileUser.read(inputBuffer) //запись из файла в буфер
            dataNamePassword = String(inputBuffer) //получили строку с данныими
        } catch (e: IllegalStateException) {
            switch.setEnabled(false)
            flagReceive = false
            showWarning("Необходимо указать логин и пароль для устройства.")
            return
        }

        if (switch != null) {
            if (dataNamePassword != "") { //проверка, что файл может быть пустым
                val arrayDataNamePassword = dataNamePassword.split(',')
                password = arrayDataNamePassword[0] //ключ безопасности
                nameDevice = arrayDataNamePassword[1] //имя устройства
                nameDeviceConnectReceive.text = nameDevice //отображение имени устройства
                switch.setEnabled(true)
                flagReceive = true
                GlobalScope.launch {
                    receiveData()
                }
            } else {
                switch.setEnabled(false)
                flagReceive = false
                showWarning("Что-то пошло не так, укажите логин и пароль для устройства.")
                return
            }
        } else {
            switch.setEnabled(false)
            flagReceive = false
            showWarning("Вы отключили режим приема.")
            return
        }

             if (textView != null) {
            textView.setOnClickListener {
                showWarning("Ваш ключ - $password")
            }
        }
    }

    fun mainWindowOpen(view: View) {
        val mainIntent = Intent(
            this,
            MainActivity::class.java
        )
        startActivity(mainIntent)
    }

    fun showWarning(textWarning: String){
        val showWarning = Toast.makeText(
            this,
            textWarning,
            Toast.LENGTH_LONG
        )
        showWarning.setGravity(
            Gravity.CENTER,
            0,
            0
        )
        showWarning.show()
    }

    private suspend fun userConfirm(dataUsers: ByteArray) {
        if (flagReceive) {
            val dataBitsUsers: BitSet = BitSet.valueOf(dataUsers).get(
                0,
                dataUsers.size
            ) //получаем данные в битах

            if (dataBitsUsers[0] == dataBitsUsers[1] == dataBitsUsers[2] == dataBitsUsers[4]) {//проверка на окончание данных
                countUserInfo++

                if (countUserInfo >= 2) { //когда прием данных авторизации закончен, осуществляется проверка их на соответствие
                    countUserInfo=0

                    if (resultUnitPasswordDecoder === password) {
                        userLogin = true
                        userPassword = true
                        withContext(Dispatchers.Main) {
                            nameDeviceConnectReceive.append(System.getProperty("line.separator"))
                            nameDeviceConnectReceive.append(resultUnitNameDecoder) //отображение имени передающего устройства
                        }
                    } else { //если данные не совпали
                        audioRunning = false
                        withContext(Dispatchers.Main) {
                            switch.setEnabled(false)
                            showWarning("Данные не совпали, запросите их заново.")
                        }
                    }
                }

                return
            }

            var dataBitsWithoutCrc: BitSet = BitSet(256) //для посылки без crc чтобы сравнить с ранее полученной
            var dataBitsCrc: BitSet = BitSet(16) //crc
            var count = 0 //для подсчета всех битов
            var countCrc = 0 //для подсчета битов у crc
            var countBits = 0 //для подсчета битов данных

            while (count < 256) {
                if ((count < 222) || (count in 254..255)) dataBitsWithoutCrc[countBits++] = dataBitsUsers[count++] //получаем самму посылку без crc
                else if ((count in 222..253) && (count % 2 == 0)) dataBitsCrc[countCrc++] = dataBitsUsers[count++] //получаем crc бещ доп битов
            }

            val crcForPack = crcPack(dataBitsWithoutCrc) //получили crc
            var fullPackage: BitSet = BitSet(108) //для хранения только данных из файла
            count = 2
            countBits = 0

            if (dataBitsCrc === crcForPack) { //проверяем совпадают ли суммы
                while (count < 222) { //получаем посылку без доп бит
                    if ((count % 2 == 0)) fullPackage[countBits++] = dataBitsWithoutCrc[count++] //получаем самму посылку без доп битов, а также без старт и стоп битов
                }
                //схема дешифрования
                if (countUserInfo == 0) { //получение имени файла
                    val keyForUnitPassword = "LightNamePassw".toByteArray() //это ключ для шифрования пароля устройства
                    val decoder: RC4 = RC4(keyForUnitPassword)
                    val decoderResult = decoder.decode(
                        fullPackage.toByteArray(),
                        fullPackage.toByteArray().size
                    )
                    resultUnitPasswordDecoder = decoderResult.toString(Charsets.UTF_8) + "."//смотрим что получили после дешифрования
                } else if (countUserInfo == 1) { //получение формата файла
                    val keyForUnitsFormat = "LightNameDevice".toByteArray() //это ключ для шифрования имени устройства
                    val decoderF: RC4 = RC4(keyForUnitsFormat)
                    val decoderResult = decoderF.decode(
                        fullPackage.toByteArray(),
                        fullPackage.toByteArray().size
                    )
                    resultUnitNameDecoder += decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                }
            } else { //если сумма не сошлась
                audioRunning = false
                withContext(Dispatchers.Main) {
                    switch.setEnabled(false)
                    showWarning("Данные не совпали, запросите их заново.")
                }
                return
            }
        } else {
            audioRunning = false
            withContext(Dispatchers.Main) {
                switch.setEnabled(false)
                showWarning("Вы отключили режим приема.")
            }
            return
        }
    }

    private suspend fun fileCreate(dataUsers: ByteArray) {
        if (flagReceive) {
            if (countFiles >= 1 && dataUsers.isEmpty()) {//проверка что приняты все файлы
                audioRunning = false
                withContext(Dispatchers.Main) {
                    switch.setEnabled(false)
                    showWarning("Прием данных окончен.")
                }
                return
            }

            val dataBitsUsers: BitSet = BitSet.valueOf(dataUsers).get(
                0,
                dataUsers.size
            ) //получаем данные в битах

            if (dataBitsUsers[0] == dataBitsUsers[1] == dataBitsUsers[2] == dataBitsUsers[4]) {//проверка на окончание данных
                countPartFile++

                if (countPartFile >= 3) {//когда закончен прием всех данных создается файл со всем содержимым
                    applicationContext.openFileOutput(
                        resultFileNameDecoder,
                        Context.MODE_PRIVATE
                    ).use {
                        it.write(resultStrDecoder.toByteArray())
                    }
                    countPartFile = 0
                    countFiles++
                    withContext(Dispatchers.Main) {
                        showWarning("Файл $resultFileNameDecoder загружен.")
                    }
                }

                if (countFiles == 3) {//проверка что приняты все файлы
                    audioRunning = false
                    withContext(Dispatchers.Main) {
                        switch.setEnabled(false)
                        showWarning("Прием данных окончен.")
                    }
                }

                return
            }

            var dataBitsWithoutCrc: BitSet = BitSet(256) //для посылки без crc чтобы сравнить с ранее полученной
            var dataBitsCrc: BitSet = BitSet(16) //crc
            var count = 0 //для подсчета всех битов
            var countCrc = 0 //для подсчета битов у crc
            var countBits = 0 //для подсчета битов данных

            while (count < 256) {
                if ((count < 222) || (count in 254..255)) dataBitsWithoutCrc[countBits++] = dataBitsUsers[count++] //получаем самму посылку без crc
                else if ((count in 222..253) && (count % 2 == 0)) dataBitsCrc[countCrc++] = dataBitsUsers[count++] //получаем crc бещ доп битов
            }

            val crcForPack = crcPack(dataBitsWithoutCrc) //получили crc
            var fullPackage: BitSet = BitSet(108) //для хранения только данных из файла
            count = 2
            countBits = 0

            if (dataBitsCrc === crcForPack) { //проверяем совпадают ли суммы
                while (count < 222) { //получаем посылку без доп бит
                    if ((count % 2 == 0)) fullPackage[countBits++] = dataBitsWithoutCrc[count++] //получаем самму посылку без доп битов, а также без старт и стоп битов
                }
                //схема дешифрования
                if (countPartFile == 0) { //получение имени файла
                    val keyForUnitsName = "LightName".toByteArray() //это ключ для шифрования имени файла
                    val decoder: RC4 = RC4(keyForUnitsName)
                    val decoderResult = decoder.decode(
                        fullPackage.toByteArray(),
                        fullPackage.toByteArray().size
                    )
                    resultFileNameDecoder = decoderResult.toString(Charsets.UTF_8) + "."//смотрим что получили после дешифрования
                } else if (countPartFile == 1) { //получение формата файла
                    val keyForUnitsFormat = "LightFormat".toByteArray() //это ключ для шифрования формата файла
                    val decoderF: RC4 = RC4(keyForUnitsFormat)
                    val decoderResult = decoderF.decode(
                        fullPackage.toByteArray(),
                        fullPackage.toByteArray().size
                    )
                    resultFileNameDecoder += decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                    InfoReceiveFiles.append(resultFileNameDecoder) //отображение имени файла на экране
                    InfoReceiveFiles.append(System.getProperty("line.separator")) //перенос строки для следующего файла
                } else { //получение содержимого файла
                    val keyForUnitsFile = "LightFile".toByteArray() //это ключ для шифрования данных файла
                    val decoderFile: RC4 = RC4(keyForUnitsFile)
                    val decoderResult = decoderFile.decode(
                        fullPackage.toByteArray(),
                        fullPackage.toByteArray().size
                    )
                    resultStrDecoder += decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                }
            } else { //если сумма не сошлась
                audioRunning = false
                withContext(Dispatchers.Main) {
                    switch.setEnabled(false)
                    showWarning("Файл поврежден, повторите попытку передачи.")
                }
                return
            }
        } else {
            audioRunning = false
            withContext(Dispatchers.Main) {
                switch.setEnabled(false)
                showWarning("Вы отключили режим приема.")
            }
            return
        }
    }

    private suspend fun receiveData () {
        if(flagReceive) {
            setThreadPriority(-19) //приоритет для потока обработки аудио
            audioRunning = true
            var result = 0
            var dataRecord: ByteArray = byteArrayOf() //здесь буду храниться считанные байты с приемопередатчика для дальнейшей обработки
            val minBufferSize = AudioRecord.getMinBufferSize(
                8000,  //устанавливаем частоту, частота 44100Гц для всех устройств, которая поддерживается, где-то может быть больше
                AudioFormat.CHANNEL_OUT_FRONT_RIGHT, //принимаем через правый канал
                AudioFormat.ENCODING_PCM_8BIT //формат входных данных, более известный как кодек
            )
             val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val changeListener = AudioManager.OnAudioFocusChangeListener { focusChange -> //слушаетль смены аудиофокуса
                if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    audioManager.setMicrophoneMute(true)
                    switch.setEnabled(false)
                    flagReceive = false
                    showWarning("Прием прерван, повторите попытку.")
                }
                else if (focusChange == AUDIOFOCUS_GAIN) {
                    audioManager.setMicrophoneMute(false)
                    switch.setEnabled(false)
                    flagReceive = false
                    showWarning("Прием прерван, повторите попытку.")
                }
                else if (focusChange == AUDIOFOCUS_GAIN_TRANSIENT) {
                    audioManager.setMicrophoneMute(true)
                    switch.setEnabled(false)
                    flagReceive = false
                    showWarning("Прием прерван, повторите попытку.")
                }
            }

            if (minBufferSize == AudioRecord.ERROR) {
                audioRunning = false
                withContext(Dispatchers.Main) {
                    switch.setEnabled(false)
                    audioManager.setMicrophoneMute(true)
                    showWarning("Что-то пошло не так, повторите попытку передачи.")
                }
                System.err.println("getMinBufferSize returned ERROR")
                return
            }

            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                audioRunning = false
                withContext(Dispatchers.Main) {
                    switch.setEnabled(false)
                    audioManager.setMicrophoneMute(true)
                    showWarning("Что-то пошло не так, повторите попытку передачи.")
                }
                System.err.println("getMinBufferSize returned ERROR_BAD_VALUE")
                return
            }

            val audioData: AudioRecord = AudioRecord(
                AudioFormat.CHANNEL_OUT_FRONT_RIGHT,
                8000,  //устанавливаем частоту, частота 44100Гц для всех устройств, которая поддерживается, где-то может быть больше
                AudioFormat.CHANNEL_OUT_FRONT_RIGHT, //принимаем через правый канал
                AudioFormat.ENCODING_PCM_16BIT, //формат входных данных, более известный как кодек
                minBufferSize * 10 //размер самого внутреннего буфера
            )

            if (audioData.state != AudioRecord.STATE_INITIALIZED) {
                audioRunning = false
                withContext(Dispatchers.Main) {
                    switch.setEnabled(false)
                    audioManager.setMicrophoneMute(true)
                    showWarning("Что-то пошло не так, повторите попытку передачи.")
                }
                System.err.println("getState() != STATE_INITIALIZED")
                return
            }

            try {
                audioData.startRecording() //начинаем запись
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                return
            }

            while (audioRunning) {
                if (flagReceive) {
                    if (Build.VERSION.SDK_INT < 26 )  { // для разных версии андрод разные подходы с смене аудиофокуса
                        result = audioManager.requestAudioFocus(
                            changeListener,
                            AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN)
                    } else {
                        var focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                            setAudioAttributes(AudioAttributes.Builder().run {
                                setUsage(AudioAttributes.USAGE_GAME)
                                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                build()})
                            setAcceptsDelayedFocusGain(true) //асинхронная обработка запроса фокуса
                            setOnAudioFocusChangeListener(changeListener)
                            build()}
                    }

                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        val samplesRead: Int = audioData.read( //считывание данных
                            dataRecord,
                            0,
                            32
                        )

                        if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                            audioRunning = false
                            withContext(Dispatchers.Main) {
                                switch.setEnabled(false)
                                audioManager.setMicrophoneMute(true)
                                showWarning("Что-то пошло не так, повторите попытку передачи.")
                            }
                            System.err.println("read() returned ERROR_INVALID_OPERATION")
                            return
                        }

                        if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                            audioRunning = false
                            withContext(Dispatchers.Main) {
                                switch.setEnabled(false)
                                audioManager.setMicrophoneMute(true)
                                showWarning("Файл поврежден, повторите попытку передачи.")
                            }
                            System.err.println("read() returned ERROR_BAD_VALUE")
                            return
                        }

                        if (userLogin && userPassword) {
                            fileCreate(dataRecord)
                        } else if (samplesRead == 32) {//посылаем данные на обработку
                            userConfirm(dataRecord)
                        }
                    } else {
                        audioRunning = false
                        withContext(Dispatchers.Main) {
                            switch.setEnabled(false)
                            audioManager.setMicrophoneMute(true)
                            showWarning("Прием остановлен, данные утеряны.")
                        }
                        return
                    }
                } else {
                    audioRunning = false
                    withContext(Dispatchers.Main) {
                        switch.setEnabled(false)
                        audioManager.setMicrophoneMute(true)
                        showWarning("Прием остановлен пользователем, данные утеряны.")
                    }
                    return
                }
            }

            try {
                try {
                    audioData.stop() //останавливаем запись
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                    return
                }
            }

            finally { //освобождаем ресурсы
                audioData.release()
            }
        } else {
             val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            withContext(Dispatchers.Main) {
                switch.setEnabled(false)
                audioManager.setMicrophoneMute(true)
                showWarning("Прием данных отключен.")
            }
            return
        }
    }
}
