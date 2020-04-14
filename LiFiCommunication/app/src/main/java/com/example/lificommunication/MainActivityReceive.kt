package com.example.lificommunication

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process.setThreadPriority
import android.view.Gravity
import android.view.View
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_recieve.*
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

    private var userLogin = false //для проверки логина с передающего устройства
    private var userPassword = false //для проверки пароля с передающего устройства
    private var countUserInfo = 0 //для подсчета количества посылок авторизации
    private var resultUnitNameDecoder: String = "" //для хранения имени устройства
    private var resultUnitPasswordDecoder: String = "" //для хранения пароля устройства

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_recieve)
    }

    fun mainActivity (view: View) {
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
    }

    private fun onSwitchClicked(view: View){
        val file = File("dataUser.txt")

        if(file.exists()) { //проверка существует ли файл с логином и паролем
            if(file.length().toInt() != 0) { //проверка, что файл может быть пустым
                findViewById<Switch>(R.id.switchReceive)?.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        flagReceive = true
                        recieveData()
                    } else {
                        flagReceive = false
                        val showWarning = Toast.makeText(
                            this,
                            "Вы отключили режим приема.",
                            Toast.LENGTH_SHORT
                        )
                        showWarning.setGravity(Gravity.CENTER, 0, 0)
                        showWarning.show()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Что-то пошло не так, укажите логин и пароль для устройства.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        } else {
            Toast.makeText(
                this,
                "Необходимо указать логин и пароль для устройства.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
    }

    private fun stopAudio(audio: AudioRecord){ //функция для остановки записи
        try {
            try {
                audio.stop() //останавливаем запись
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                return
            }
        }

        finally { //освобождаем ресурсы
            audio.release()
        }
    }

    private fun userConfirm(dataUsers: ByteArray, audioData: AudioRecord) {
        val dataBitsUsers: BitSet = BitSet.valueOf(dataUsers).get(0, dataUsers.size) //получаем данные в битах

        if(dataBitsUsers[0] == dataBitsUsers[1] == dataBitsUsers[2] == dataBitsUsers[4]){//проверка на окончание данных
            countUserInfo++
        }

        var dataBitsWithoutCrc: BitSet = BitSet(256) //для посылки без crc чтобы сравнить с ранее полученной
        var dataBitsCrc: BitSet = BitSet(16) //crc
        var count = 0 //для подсчета всех битов
        var countCrc = 0 //для подсчета битов у crc
        var countBits = 0 //для подсчета битов данных

        while(count < 256) {
            if((count < 222) || (count in 254..255))  dataBitsWithoutCrc[countBits++] = dataBitsUsers[count++] //получаем самму посылку без crc
            else if ((count in 222..253) && (count % 2 == 0)) dataBitsCrc[countCrc++] = dataBitsUsers[count++] //получаем crc бещ доп битов
        }

        val crcForPack = crcPack(dataBitsWithoutCrc) //получили crc
        var fullPackage: BitSet = BitSet(108) //для хранения только данных из файла
        count = 2
        countBits = 0

        if(dataBitsCrc === crcForPack) { //проверяем совпадают ли суммы
            while (count < 222) { //получаем посылку без доп бит
                if ((count % 2 == 0)) fullPackage[countBits++] =
                    dataBitsWithoutCrc[count++] //получаем самму посылку без доп битов, а также без старт и стоп битов
            }
            //схема дешифрования
            if (countUserInfo == 0) { //получение имени файла
                val keyForUnitPassword = "LightNamePassw".toByteArray() //это ключ для шифрования пароля устройства
                val decoder: RC4 = RC4(keyForUnitPassword)
                val decoderResult = decoder.decode(fullPackage.toByteArray(), fullPackage.toByteArray().size)
                resultUnitPasswordDecoder = decoderResult.toString(Charsets.UTF_8) + "."//смотрим что получили после дешифрования
            } else if (countUserInfo == 1) { //получение формата файла
                val keyForUnitsFormat = "LightNameDevice".toByteArray() //это ключ для шифрования имени устройства
                val decoderF: RC4 = RC4(keyForUnitsFormat)
                val decoderResult = decoderF.decode(fullPackage.toByteArray(), fullPackage.toByteArray().size)
                resultUnitNameDecoder += decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
            }

            if(countUserInfo >= 2) {//когда прием данных авторизации закончен, осуществляется проверка их на соответствие
                val fileInputDataUser: FileInputStream = openFileInput("dataUser.txt") //файл с именем уст-ва и паролем
                val inputStreamFileUser = InputStreamReader(fileInputDataUser) //поток для чтения
                val inputBuffer = CharArray(22) //максимальный размер буфера
                inputStreamFileUser.read(inputBuffer) //запись из файла в буфер
                val dataNamePassword = String(inputBuffer) //получили строку с данныими
                var arrayDataNamePassword= dataNamePassword.split(',')
                val password= arrayDataNamePassword[0] //ключ безопасности
                val nameDevice= arrayDataNamePassword[1] //имя устройства

                if(resultUnitPasswordDecoder === password) {
                    userLogin = true
                    userPassword = true

                    nameDeviceConnectReceive.text = resultUnitNameDecoder //отображение имени передающего устройства
                } else { //если данные не совпали
                    audioRunning = false
                    Toast.makeText(
                        this,
                        "Данные не совпали, запросите их заново.",
                        Toast.LENGTH_SHORT
                    ).show()
                    stopAudio(audioData) //принудительная остановка приема, если данные не сопадают
                    return
                }
            }
        } else { //если сумма не сошлась
            audioRunning = false
            Toast.makeText(
                this,
                "Данные не совпали, запросите их заново.",
                Toast.LENGTH_SHORT
            ).show()
            stopAudio(audioData) //принудительная остановка приема, если сумма не сошлась
            return
        }
    }

    private fun fileCreate(dataUsers: ByteArray, audioData: AudioRecord) {
        if(countFiles >= 1 && dataUsers.isEmpty()) {//проверка что приняты все файлы
            audioRunning = false
            Toast.makeText(
                this,
                "Прием данных окончен.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dataBitsUsers: BitSet = (BitSet.valueOf(dataUsers)).get(0, dataUsers.size) //получаем данные в битах

        if(dataBitsUsers[0] == dataBitsUsers[1] == dataBitsUsers[2] == dataBitsUsers[4]){//проверка на окончание данных
            countPartFile++
        }

        var dataBitsWithoutCrc: BitSet = BitSet(256) //для посылки без crc чтобы сравнить с ранее полученной
        var dataBitsCrc: BitSet = BitSet(16) //crc
        var count = 0 //для подсчета всех битов
        var countCrc = 0 //для подсчета битов у crc
        var countBits = 0 //для подсчета битов данных

        while(count < 256) {
            if((count < 222) || (count in 254..255))  dataBitsWithoutCrc[countBits++] = dataBitsUsers[count++] //получаем самму посылку без crc
            else if ((count in 222..253) && (count % 2 == 0)) dataBitsCrc[countCrc++] = dataBitsUsers[count++] //получаем crc бещ доп битов
        }

        val crcForPack = crcPack(dataBitsWithoutCrc) //получили crc
        var fullPackage: BitSet = BitSet(108) //для хранения только данных из файла
        count = 2
        countBits = 0

        if(dataBitsCrc === crcForPack) { //проверяем совпадают ли суммы
            while(count < 222) { //получаем посылку без доп бит
                if((count % 2 == 0)) fullPackage[countBits++] = dataBitsWithoutCrc[count++] //получаем самму посылку без доп битов, а также без старт и стоп битов
            }
            //схема дешифрования
            if (countPartFile == 0) { //получение имени файла
                val keyForUnitsName = "LightName".toByteArray() //это ключ для шифрования имени файла
                val decoder: RC4 = RC4(keyForUnitsName)
                val decoderResult = decoder.decode(fullPackage.toByteArray(), fullPackage.toByteArray().size)
                resultFileNameDecoder = decoderResult.toString(Charsets.UTF_8) + "."//смотрим что получили после дешифрования
            } else if (countPartFile == 1) { //получение формата файла
                val keyForUnitsFormat = "LightFormat".toByteArray() //это ключ для шифрования формата файла
                val decoderF: RC4 = RC4(keyForUnitsFormat)
                val decoderResult = decoderF.decode(fullPackage.toByteArray(), fullPackage.toByteArray().size)
                resultFileNameDecoder += decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                InfoReceiveFiles.append(resultFileNameDecoder) //отображение имени файла на экране
                InfoReceiveFiles.append(System.getProperty("line.separator")) //перенос строки для следующего файла
            } else { //получение содержимого файла
                val keyForUnitsFile = "LightFile".toByteArray() //это ключ для шифрования данных файла
                val decoderFile: RC4 = RC4(keyForUnitsFile)
                val decoderResult = decoderFile.decode(fullPackage.toByteArray(), fullPackage.toByteArray().size)
                resultStrDecoder += decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
            }

            if(countPartFile >= 3) {//когда закончен прием всех данных создается файл со всем содержимым
                applicationContext.openFileOutput(resultFileNameDecoder, Context.MODE_PRIVATE).use {
                    it.write(resultStrDecoder.toByteArray())
                }
                countPartFile = 0
                countFiles++
            }
            if(countFiles == 3) {//проверка что приняты все файлы
                audioRunning = false
                Toast.makeText(
                    this,
                    "Прием данных окончен.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else { //если сумма не сошлась
            audioRunning = false
            Toast.makeText(
                this,
                "Файл поврежден, повторите попытку передачи.",
                Toast.LENGTH_SHORT
            ).show()
            stopAudio(audioData) //принудительная остановка приема, если сумма не сошлась
            return
        }
    }

    private fun recieveData () {
        if(flagReceive) {
            setThreadPriority(-19) //приоритет для потока обработки аудио
            audioRunning = true
            var dataRecord: ByteArray = byteArrayOf() //здесь буду храниться считанные байты с приемопередатчика для дальнейшей обработки

            val minBufferSize = AudioRecord.getMinBufferSize(
                44100,  //устанавливаем частоту, частота 44100Гц для всех устройств, которая поддерживается, где-то может быть больше
                AudioFormat.CHANNEL_OUT_FRONT_RIGHT, //принимаем через правый канал
                AudioFormat.ENCODING_PCM_16BIT //формат входных данных, более известный как кодек
            )

            if (minBufferSize == AudioRecord.ERROR) {
                Toast.makeText(
                    this,
                    "Что-то пошло не так, повторите попытку передачи.",
                    Toast.LENGTH_SHORT
                ).show()
                System.err.println("getMinBufferSize returned ERROR")
                return
            }
            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Toast.makeText(
                    this,
                    "Что-то пошло не так, повторите попытку передачи.",
                    Toast.LENGTH_SHORT
                ).show()
                System.err.println("getMinBufferSize returned ERROR_BAD_VALUE")
                return
            }

            val audioData: AudioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,  //устанавливаем частоту, частота 44100Гц для всех устройств, которая поддерживается, где-то может быть больше
                AudioFormat.CHANNEL_OUT_FRONT_RIGHT, //принимаем через правый канал
                AudioFormat.ENCODING_PCM_16BIT, //формат входных данных, более известный как кодек
                minBufferSize * 10 //размер самого внутреннего буфера
            )

            if (audioData.state != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(
                    this,
                    "Что-то пошло не так, повторите попытку передачи.",
                    Toast.LENGTH_SHORT
                ).show()
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
                    val samplesRead: Int = audioData.read(dataRecord, 0, 32) //считывание данных

                    if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Toast.makeText(
                            this,
                            "Что-то пошло не так, повторите попытку передачи.",
                            Toast.LENGTH_SHORT
                        ).show()
                        System.err.println("read() returned ERROR_INVALID_OPERATION")
                        return
                    }
                    if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Toast.makeText(
                            this,
                            "Файл поврежден, повторите попытку передачи.",
                            Toast.LENGTH_SHORT
                        ).show()
                        System.err.println("read() returned ERROR_BAD_VALUE")
                        return
                    }

                    if (userLogin && userPassword) fileCreate(
                        dataRecord,
                        audioData
                    ) //посылаем данные на обработку
                    else userConfirm(dataRecord, audioData)
                } else {
                    Toast.makeText(
                        this,
                        "Прием остановлен пользователем, данные утеряны.",
                        Toast.LENGTH_SHORT
                    ).show()
                    stopAudio(audioData)
                    return
                }
            }

            stopAudio(audioData)
        } else {
            Toast.makeText(
                this,
                "Прием данных отключен.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
    }
}
