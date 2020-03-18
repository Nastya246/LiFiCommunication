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

var ListArraySend = arrayListOf<Array<BitSet>>()
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
fun CrcPack (packData:BitSet): BitSet //подсчет crc и преобразование ее в биты
{
    //11000000000000101
   /* var Polinom:BitSet= BitSet(17)
    Polinom[0]=true
    Polinom[1]=true
    Polinom[2]=false
    Polinom[3]=false
    Polinom[4]=false
    Polinom[5]=false
    Polinom[6]=false
    Polinom[7]=false
    Polinom[8]=false
    Polinom[9]=false
    Polinom[10]=false
    Polinom[11]=false
    Polinom[12]=false
    Polinom[13]=false
    Polinom[14]=true
    Polinom[15]=false
    Polinom[16]=true*/
    var Polinom:Int=98309


    //int n, sum=0; // n - количество бит, sum это наше десятичное число которое получится из двоичного
   // char ch[n];//  это наше двоичное число. число символов рано n из предыдущей строки
//for (n in packData.size()-1..-1)
  //  for (i=n-1;i>-1;i--)//    пошли по строке бит
   // if (ch[i]=='1')   // если i-й бит в строке = 1 ,
      //  sum+=pow(2,i) ;// то прибавить к общей сумме 2 в степени i
   // var crcInt=packData.toLongArray() % Polinom
  //  var crcByteArrayPack=crcInt.toString().toByteArray()
  //  var crcBit: BitSet= (BitSet.valueOf(crcByteArrayPack)).get(0, crcByteArrayPack.size)
  //  return crcBit
}
   fun packageCreate(dataUsers: ByteArray)  {

        var countIndex :Int=0

        var predel: Int = 0
        var count: Int = 0
        var size=(dataUsers.size*8)/110 //количество битовых посылок
        countIndex=size //число посылок
        var outCircle:Int=0 //переменная внешнего цикла
        if (size==0) countIndex=0

        var UnitPackSend: Array<BitSet> // для записи частей посылки
        var fromIndex: Int = 0 * 8 + 0
        val sendPack: BitSet= (BitSet.valueOf(dataUsers)).get(fromIndex, fromIndex+110) //получаем набор битов
        var sendPackTemp:BitSet= BitSet() //для хранения посылки с доп. битами, crc и старт, стоп битами

       while (outCircle<=size) {
           if (size <=1) //меньше одной посылки
           {predel=dataUsers.size*8} //если меньше 110 бит
           else
           {
               predel=110+(countIndex*outCircle)
           }
            for (n in (0 + countIndex*outCircle)..(109 + (countIndex*outCircle))) { //для одной посылки
                if (n == 0 + countIndex*outCircle) {
                    sendPackTemp[count] = true //старт бит
                    count++
                    sendPackTemp[count] = false //доп.бит
                    count++
                }
                if (n > predel) {
                    sendPackTemp[count] = false //бит данных
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
                if (n == 109 + countIndex*outCircle) //стоп-бит
                {
                    sendPackTemp[count] = false
                    count++
                    sendPackTemp[count] = true
                }
            }
            outCircle++
           var countThirdCircleForCrc:Int= outCircle*countIndex+109
           var crcForPack=CrcPack(sendPackTemp) //получили crc
           var sizeCircle=crcForPack.size()*8
           for (n in 0..(sizeCircle)) //добавляем в посылку crc
           {
               sendPackTemp[countThirdCircleForCrc]=crcForPack[n]
               countThirdCircleForCrc++
               if (n==sizeCircle-1) //стоп-бит
               {
                   sendPackTemp[countThirdCircleForCrc++] = false
                   countThirdCircleForCrc++
                   sendPackTemp[ countThirdCircleForCrc++] = true
               }
           }
       }
       UnitPackSend = arrayOf(sendPackTemp)
       ListArraySend.add(UnitPackSend) //добавляем в глобальную переменную преобразованные данные

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
                    packageCreate(encoderResult) //формирование посылки

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
