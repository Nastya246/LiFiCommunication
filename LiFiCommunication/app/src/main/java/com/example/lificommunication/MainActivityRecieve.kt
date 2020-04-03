package com.example.lificommunication

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivityRecieve: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_recieve)
    }

    private var countPartFile: Int = 0 //для отсечения имени файла и формата от данных
    private var resultFileNameDecoder: String = "" //для хранения данных и записи в файл
    private var resultStrDecoder: String = "" //для хранения данных и записи в файл

    private fun fileCreate(dataUsers: ByteArray) {
        val dataBitsUsers: BitSet = (BitSet.valueOf(dataUsers)).get(0, dataUsers.size) //получаем данные в битах

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
                countPartFile++
            } else if (countPartFile == 1) { //получение формата файла
                val keyForUnitsFormat = "LightFormat".toByteArray() //это ключ для шифрования формата файла
                val decoderF: RC4 = RC4(keyForUnitsFormat)
                val decoderResult = decoderF.decode(fullPackage.toByteArray(), fullPackage.toByteArray().size)
                resultFileNameDecoder += decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                countPartFile++
            } else { //получение содержимого файла
                val keyForUnitsFile = "LightFile".toByteArray() //это ключ для шифрования данных файла
                val decoderFile: RC4 = RC4(keyForUnitsFile)
                val decoderResult = decoderFile.decode(fullPackage.toByteArray(), fullPackage.toByteArray().size)
                resultStrDecoder = decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
            }

            applicationContext.openFileOutput(resultFileNameDecoder, Context.MODE_PRIVATE).use {
                it.write(resultStrDecoder.toByteArray())
            }
        } else { //если сумма не сошлась
            Toast.makeText(this, "Файл поврежден, повторите попытку передачи.", Toast.LENGTH_SHORT).show()
            return
        }
    }
}
