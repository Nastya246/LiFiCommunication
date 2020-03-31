package com.example.lificommunication

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
import android.media.AudioTrack
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_send.*
import java.util.*
import kotlin.concurrent.thread
import kotlin.experimental.xor
import kotlinx.coroutines.*



var ListArraySend = arrayListOf<BitSet>()
    class RC4 ( key: ByteArray){
        var Perestanovki= IntArray (256, {0})
        var x: Int = 0
        var y: Int = 0
        //метод начальной инициализации ключа по алгоритму ключевого распсиания
        fun InitParam(key: ByteArray)  {
            val lenghtKey=key.size
            for (n in 0..255)
            {
                Perestanovki[n]=n
                if (Perestanovki[n]<0) Perestanovki[n]+=256
            }
            var j: Int
            j=0
            for (n in 0..255)
            {
                j=(j+Perestanovki[n]+key[n%lenghtKey])%256
                if (j<0) j+=256
                var temp=Perestanovki[n]
                Perestanovki[n]=Perestanovki[j]
                Perestanovki[j]=temp
            }
        }
        init
        {
        InitParam(key)
        }
        //генератор случайной последовательности
        fun keyRandom(): Int  {
            x=(x+1)%256
            y=(y+Perestanovki[x])%256
            var temp=Perestanovki[x]
            Perestanovki[x]=Perestanovki[y]
            Perestanovki[y]=temp
            return  Perestanovki[(Perestanovki[x]+Perestanovki[y])%256]
        }
        // кодируем
        fun Encode(dataBinaryUsers: ByteArray, size: Int): ByteArray  {
            var dataUsers = dataBinaryUsers
            var chiperData = ByteArray (dataUsers.size, {0})
        for (n in 0..(size-1))
        {
            chiperData[n]=(dataUsers[n] xor  keyRandom().toByte())
        }
            return chiperData
        }
        fun Decode(dataBinaryUsers: ByteArray, size: Int): ByteArray  {

            return Encode(dataBinaryUsers, size)
        }
    }

    class MainActivitySend : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main_send)
        }

        val arrayFiles = arrayOfNulls<String>(3)
        var count: Int = 0
        fun AddFile(view: View) {
            val intent = Intent().setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)//Выбор любого типа файла

            try {
                startActivityForResult(Intent.createChooser(intent, "Выберите файл..."), 111)
            } catch (e: Exception) {
                Toast.makeText(this, "Пожалуйста установите файловый менеджер.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        fun SendData(view: View) = runBlocking {
            val taskSend = GlobalScope.launch {
                for (n in ListArraySend) {
                    SendInformation(n.toByteArray())
                }
            }
            taskSend.join()
        }

        fun CrcPack (packData:BitSet): BitSet //подсчет crc и преобразование ее в биты
        {
            var DataIntFormat :Int=0
            var Polinom:Int=98309
            for (n in 0..223) //в десятичное число данные
            {
                var temp:Int=0
                if (packData[n]) temp=1
                else temp=0
                DataIntFormat+=temp.shl(n)
            }
            var crcInt:Int=DataIntFormat%Polinom //получаем crc
            var crcPackString=crcInt.toString()
            var crcIntArrayPack= Array<Byte>(crcPackString.length,{0})
            for (n in 0..(crcPackString.length-1))
            {
            crcIntArrayPack[n]=crcPackString[n].toInt().toByte()
            }
            var crcBit: BitSet= (BitSet.valueOf(crcIntArrayPack.toByteArray())).get(0, crcIntArrayPack.size) // crc в битах

            return crcBit
        }
        fun packageCreate(dataUsers: ByteArray) {

           var countIndex: Int = 0
           var predel: Int = 0
           var count: Int = 0
           var size = (dataUsers.size * 8) / 110 //количество битовых посылок
           countIndex = size //число посылок
           var outCircle: Int = 0 //переменная внешнего цикла для  подсчета посылок
           if (size == 0) countIndex = 0

           var UnitPackSend: BitSet // для записи частей посылки
           var fromIndex: Int = 0 * 8 + 0
           val sendPack: BitSet =
               (BitSet.valueOf(dataUsers)).get(0, dataUsers.size) //получаем набор битов
           var sendPackTemp: BitSet =
               BitSet(256) //для хранения посылки с доп. битами, crc и старт, стоп битами

           while (outCircle <= size) {
               if (size <= 1) //меньше одной посылки
               {
                   predel = dataUsers.size * 8
               } //если меньше 110 бит
               else {
                   predel = 110 + (countIndex * outCircle)
               }
               for (n in (0 + countIndex * outCircle)..(109 + (countIndex * outCircle))) { //для одной посылки
                   if (n == 0 + countIndex * outCircle) {
                       sendPackTemp[count] = true //старт бит
                       count++
                       sendPackTemp[count] = false //доп.бит
                       count++
                   }
                   if (n > predel) { // если число битов меньше 110, то дополняем до 110 бит 0 и соответственно доп.единицами

                           sendPackTemp[count] = false //биты данных для дополнения посылки
                           count++
                           sendPackTemp[count] = true
                           count++


                   } else {
                       sendPackTemp[count] = sendPack[n] //бит данных
                       count++
                       if (sendPack[n] == true) { //доп. бит
                           sendPackTemp[count] = false
                       } else {
                           sendPackTemp[count] = true
                       }
                       count++
                   }
                   if (n == 109 + countIndex * outCircle) //стоп-бит
                   {
                       sendPackTemp[count] = false
                       count++
                       sendPackTemp[count] = true
                   }
               }
               outCircle++  // получили посылку без crc
               var countThirdCircleForCrc: Int = outCircle * countIndex + 110
               var crcForPack = CrcPack(sendPackTemp) //получили crc
               var sizeCircle = crcForPack.length()
               if (crcForPack.length() < 16) { //если crc меньше 16 бит
                   for (n in 0..(16-crcForPack.length()-1)) //добавляем доп. нули чтобы дополнить ее до 16 бит
                   {
                       sendPackTemp[countThirdCircleForCrc] = false
                       countThirdCircleForCrc++
                       sendPackTemp[countThirdCircleForCrc] = true
                       countThirdCircleForCrc++

                   }
               }
               for (n in 0..(sizeCircle)) //добавляем в посылку crc
               {
                   sendPackTemp[countThirdCircleForCrc] = crcForPack[n]
                   countThirdCircleForCrc++
                   if (n == sizeCircle - 1) //стоп-бит
                   {
                       sendPackTemp[countThirdCircleForCrc++] = false
                       countThirdCircleForCrc++
                       sendPackTemp[countThirdCircleForCrc++] = true
                   }
               }

               UnitPackSend = sendPackTemp

               ListArraySend.add(UnitPackSend) //добавляем в глобальную переменную преобразованные данные для дальнейшей отправки

           }
        }

       suspend fun SendInformation(dataUsersFoJack: ByteArray) {
            var buffersize = AudioTrack.getMinBufferSize(8000,  //устанавливаем частоту с запасом
                AudioFormat.CHANNEL_OUT_MONO, // данные идут в левый канал, поэтому моно
                AudioFormat.ENCODING_PCM_16BIT)

            var trackplayer = AudioTrack( AudioAttributes.Builder().setUsage(USAGE_MEDIA) //устанавливаем соответствующие настройки для объекта api
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build(),
                AudioFormat.Builder().setEncoding(ENCODING_PCM_16BIT)
                    .setChannelMask(CHANNEL_OUT_MONO)
                    .setSampleRate(8000)
                    .build(),
                8000,
                AudioTrack.MODE_STREAM,
                AUDIO_SESSION_ID_GENERATE)
               trackplayer.play() // включаем проигрывание
               trackplayer.write(dataUsersFoJack, 0,dataUsersFoJack.size) // пишем данные в цап мобильного
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


                    if (selectedFile!=null) //если выбраны файлы
                    { //получаем поток байт из  файла
                        var fileUser=contentResolver.openInputStream(selectedFile)?.readBytes() //получаем поток байт из  файла

                        if (fileUser!=null) //если файл не пустой, то:
                        { //получаем имя и расширение
                            var returnCursor=contentResolver.query(selectedFile, null, null, null, null);
                            var nameIndex=returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            returnCursor.moveToFirst()
                            var nameFile= returnCursor.getString(nameIndex) //имя и расширение вместе


                            if(count === 1) InfoAddFiles.text = nameFile //вывод имени файла
                            else {
                                InfoAddFiles.append(System.getProperty("line.separator"))
                                InfoAddFiles.append(nameFile)
                            }

                            var arrayNameExection=nameFile.split('.') //разбиваем на массив
                            var nameFileStr=arrayNameExection[0].toByteArray() //имя
                            var exectionFileStr=arrayNameExection[1].toByteArray() //расширение без точки


                            var keyForUnitsName = "LightName".toByteArray(); //это ключ для шифрования имени файла
                            var encoderN: RC4=RC4(keyForUnitsName)
                            var encoderResultNameFile=encoderN.Encode(nameFileStr, nameFileStr.size) //шифрование имени файла

                            var keyForUnitsFormat = "LightFormat".toByteArray(); //это ключ для шифрования формата файла
                            var encoderF: RC4=RC4(keyForUnitsFormat)
                            var encoderResultFormatFile=encoderF.Encode(exectionFileStr, exectionFileStr.size) //шифрование формата файла

                            var keyForUnitsFile = "LightFile".toByteArray(); //это ключ для шифрования данных файла
                            var encoderFile: RC4=RC4(keyForUnitsFile)
                            var encoderResultFile=encoderFile.Encode(fileUser, fileUser.size) //шифрование файла

                            packageCreate(encoderResultNameFile) //формирование посылки для имени
                            packageCreate(encoderResultFormatFile) // для формата
                            packageCreate(encoderResultFile) // для файла

                                /*схема дешифрования:
                            var decoder: RC4=RC4(keyForUnitsName)
                            var decoderResult=decoder.Decode(encoderResultNameFile, encoderResultNameFile.size)
                            var resultstrDecoder=decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования


                            var decoderF: RC4=RC4(keyForUnitsFormat)
                            decoderResult=decoderF.Decode(encoderResultFormatFile, encoderResultFormatFile.size)
                            var resultstrDecoderF=decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования

                            var decoderFile: RC4=RC4(keyForUnitsFile)
                            decoderResult=decoderFile.Decode(encoderResultFile, encoderResultFile.size)
                            var resultstrDecoderFile=decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                            var t3=resultstrDecoderFile*/
                        }

                    }

                }
            }
        }
