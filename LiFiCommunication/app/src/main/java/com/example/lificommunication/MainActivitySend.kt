package com.example.lificommunication

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_send.*
import java.util.*
import kotlin.experimental.xor


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
        val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)//Выбор любого типа файла

        try {
            startActivityForResult(Intent.createChooser(intent, "Выберите файл..."), 111)
        } catch (e: Exception) {
            Toast.makeText(this, "Пожалуйста установите файловый менеджер.", Toast.LENGTH_SHORT).show()
        }
    }

   fun packageCreate(dataUsers: ByteArray): BitSet  {

// byteArraySend:Array<Array <Byte>>
        var t: Int = 0
        var predel: Int = 0
        var count: Int = 0
        var size=(dataUsers.size*8)/110 //количество битовых посылок
        var fromIndex: Int = 0 * 8 + 0
        val sendPack: BitSet= (BitSet.valueOf(dataUsers)).get(fromIndex, fromIndex+110) //получаем набор битов
        var sendPackTemp:BitSet= BitSet() //для хранения посылки с доп. битами, crc и старт, стоп битами
        var crcArray= ByteArray (2, {0})
        if (size >1) {predel=110} //значит больше 1 посылки
        else {predel=dataUsers.size*8} //если меньше 110 бит

        for (n in 0..109) {
            if (n == 0) {
                sendPackTemp[count] = true //старт бит
                count++
                sendPackTemp[count] = false //доп.бит
                count++
            }
        if (n>predel) {
            sendPackTemp[count] = false //бит данных
            count++
            sendPackTemp[count] = true
            count++
        }

            else {
            sendPackTemp[count] = sendPack[n] //бит данных
            count++
            if (sendPack[n] == true) { //доп. бит
                sendPackTemp[count] = false
            } else {
                sendPackTemp[count] = true
            }
            count++
        }
            if (n == 109) //стоп-бит
            {
                sendPackTemp[count] = false
                count++
                sendPackTemp[count] = true
            }

        }

        return sendPackTemp
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

            if (selectedFile!=null) //если выбраны файлы
            { //получаем поток байт из  файла
                var fileUser=contentResolver.openInputStream(selectedFile)?.readBytes() //получаем поток байт из  файла

                if (fileUser!=null) //если файл не пустой, то:
                { //получаем имя и расширение
                    var returnCursor=contentResolver.query(selectedFile, null, null, null, null);
                    var nameIndex=returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    returnCursor.moveToFirst()
                    var nameFile= returnCursor.getString(nameIndex) //имя и расширение вместе
                    var arrayNameExection=nameFile.split('.') //разбиваем на массив
                    var nameFileStr=arrayNameExection[0].toByteArray() //имя
                    var exectionFileStr=arrayNameExection[1] //расширение без точки
                    var byteArraySend = ByteArray (256, {0}) //этот массив хранит посылку
                    var keyForUnits = "Light".toByteArray(); //это ключ для шифрования
                    var encoder: RC4=RC4(keyForUnits)
                    var encoderResult=encoder.Encode(nameFileStr, nameFileStr.size) //шифрование имени файла
                    var sendPack=packageCreate(encoderResult)

                        /*  var resultstrEncoder=encoderResult.toString(Charsets.UTF_8) //смотрим что получили после шифрования


                    var decoder: RC4=RC4(keyForUnits)
                    var decoderResult=decoder.Decode(encoderResult, encoderResult.size)

                    var resultstrDecoder=decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                    var temp1=resultstrDecoder */
                }

            }

        }
    }
}
