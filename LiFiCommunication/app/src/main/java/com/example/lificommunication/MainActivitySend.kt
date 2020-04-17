package com.example.lificommunication
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_recieve.*
import kotlinx.android.synthetic.main.activity_main_send.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.experimental.xor

class RC4 (key: ByteArray){
    var perestanovki = IntArray (256, {0})
    var x: Int = 0
    var y: Int = 0
    //метод начальной инициализации ключа по алгоритму ключевого распсиания
    private fun initParam(key: ByteArray) {
        val lengthKey= key.size
        for (n in 0..255) {
            perestanovki[n] = n
            if (perestanovki[n] < 0) perestanovki[n] += 256
        }
        var j: Int
        j = 0
        for (n in 0..255) {
            j = (j + perestanovki[n] + key[n % lengthKey]) % 256
            if (j < 0) j += 256
            var temp= perestanovki[n]
            perestanovki[n] = perestanovki[j]
            perestanovki[j] = temp
        }
    }

    init {initParam(key)} //генератор случайной последовательности

    private fun keyRandom(): Int {
        x = (x + 1) % 256
        y = (y + perestanovki[x]) % 256
        var temp= perestanovki[x]
        perestanovki[x] = perestanovki[y]
        perestanovki[y] = temp
        return perestanovki[(perestanovki[x] + perestanovki[y]) % 256]
    }
    // кодируем
    fun encode(dataBinaryUsers: ByteArray, size: Int): ByteArray {
        var dataUsers = dataBinaryUsers
        var chiperData = ByteArray (dataUsers.size, {0})
        for (n in 0 until size) {
            chiperData[n] = (dataUsers[n] xor keyRandom().toByte())
        }
        return chiperData
    }
    //декодирование
    fun decode(dataBinaryUsers: ByteArray, size: Int): ByteArray {
        return encode(dataBinaryUsers, size)
    }
}

fun crcPack (packData:BitSet): BitSet  {//подсчет crc и преобразование ее в биты
    var dataIntFormat = 0
    val polinom = 98309
    for (n in 0..223) {//в десятичное число данные
        var temp = 0
        if (packData[n]) temp = 1
        else temp = 0
        dataIntFormat += temp.shl(n)
    }
    var crcInt: Int = dataIntFormat % polinom //получаем crc
    var crcPackString= crcInt.toString()
    var crcIntArrayPack = Array<Byte>(crcPackString.length, {0})
    for (n in 0..(crcPackString.length-1)) {
        crcIntArrayPack[n]=crcPackString[n].toInt().toByte()
    }
    val crcBit: BitSet = (BitSet.valueOf(crcIntArrayPack.toByteArray())).get(0, crcIntArrayPack.size) //crc в битах

    return crcBit
}

class MainActivitySend : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_send)
        InfoAddFiles.setTextColor(Color.GRAY)
        InfoAddFiles.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f)
        val fileInputDataUser: FileInputStream = openFileInput("dataUser.txt") //файл с именем уст-ва и паролем
        val inputStreamFileUser = InputStreamReader(fileInputDataUser) //поток для чтения
        val inputBuffer = CharArray(22) //максимальный размер буфера
        inputStreamFileUser.read(inputBuffer) //запись из файла в буфер
        val dataNamePassw = String(inputBuffer) //получили строку с данныими
        var arrayDataNamePasw= dataNamePassw.split(',')
        val passw= arrayDataNamePasw[0].toByteArray() //ключ безопасности
        val nameDevice= arrayDataNamePasw[1].toByteArray() //имя устройства
        nameDeviceConnectSend.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0f)
        nameDeviceConnectSend.setTextColor(Color.BLUE)
        nameDeviceConnectSend.setText(arrayDataNamePasw[1])
        val keyForPassw = "LightNamePassw".toByteArray() //это ключ для шифрования ключа пароля безопасности
        val encoderKeyPassw: RC4 = RC4(keyForPassw)
        val encoderResultPassw= encoderKeyPassw.encode(passw, passw.size) //шифрование пароля безопасности

        val keyForNameDevice = "LightNameDevice".toByteArray() //это ключ для шифрования ключа имени устройства
        val encoderKeyDevice: RC4 = RC4(keyForNameDevice)
        val encoderResultNameDevice= encoderKeyDevice.encode(nameDevice, nameDevice.size) //шифрование имени устройства
        GlobalScope.launch {
            packageCreate(encoderResultPassw) //формирование посылки для пароля безопасности
            splitPackage() //разделяем данные
            packageCreate(encoderResultNameDevice) //для имени устройства
            splitPackage()
        }
        val nameDeviceText = findViewById<TextView>(R.id.nameDeviceConnectSend) //вывод данных по нажатию на утсройство
        if (nameDeviceText != null) {
           nameDeviceText.setOnClickListener {
                    val showWarning = Toast.makeText(
                        this,
                        "Ваш ключ - "+ arrayDataNamePasw[0],
                        Toast.LENGTH_LONG
                    )
                    showWarning.setGravity(Gravity.CENTER, 0, 0)
                    showWarning.show()
                 } }
        val switchSend = findViewById<Switch>(R.id.switchSend) //проверка переключателя switch
        if (switchSend != null) {
            switchSend.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) flagSend = true
                else {
                    flagSend = false
                    val showWarning = Toast.makeText(
                        this,
                        "Вы отключили режим передачи",
                        Toast.LENGTH_SHORT
                    )
                    showWarning.setGravity(Gravity.CENTER, 0, 0)
                    showWarning.show()
                } } } }
    var ListArraySend = arrayListOf<BitSet>() //здесь храним посылки
    private val arrayFiles = arrayOfNulls<Uri>(3)
    private var count: Int = 0
    private var flagSend: Boolean = true
    private var flagComplete: Boolean = false
    private suspend fun splitPackage()
    {
        var sendPackZero: BitSet = BitSet(256)
        ListArraySend.add(sendPackZero)
    }
    fun addFile(view: View) {
        val intent = Intent().setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT) //Выбор любого типа файла
        try {
            startActivityForResult(Intent.createChooser(intent, "Выберите файл..."), 111)
        } catch (e: Exception) {
            Toast.makeText(this, "Пожалуйста установите файловый менеджер.", Toast.LENGTH_SHORT)
                .show()
        } }

    fun sendData(view: View?) {
        if (flagComplete)
        { if (!flagSend) {
            val showWarning = Toast.makeText(
                this,
                "Вы отключили режим передачи",
                Toast.LENGTH_SHORT
            )
            showWarning.setGravity(Gravity.CENTER, 0, 0)
            showWarning.show()
        }
        else {
            val taskSend = GlobalScope.launch {
                if (flagSend) sendInformation()
            } }}
    else {
            val showWarning = Toast.makeText(
                this,
                "Дождитесь окончания операции",
                Toast.LENGTH_SHORT
            )
            showWarning.setGravity(Gravity.CENTER, 0, 0)
            showWarning.show()
        }}

    private suspend fun packageCreate(dataUsers: ByteArray) {
        var countIndex = 0
        var predel = 0
        var count = 0
        var size = (dataUsers.size * 8) / 110 //количество битовых посылок
        countIndex = 1 //переменная для цикла
        var outCircle = 0 //переменная внешнего цикла для  подсчета посылок
        var unitPackSend: BitSet // для записи частей посылки
        var fromIndex: Int = 0 * 8 + 0
        val sendPack: BitSet =
            (BitSet.valueOf(dataUsers)).get(0, dataUsers.size) //получаем набор битов
        var sendPackTemp: BitSet =
            BitSet(256) //для хранения посылки с доп. битами, crc и старт, стоп битами
        if (size <= 1) predel = dataUsers.size * 8 //меньше одной посылки
       do {
            if (size <= 1) predel = dataUsers.size * 8 //меньше одной посылки
            else predel = 109 *countIndex+outCircle // 110 бит
            for (n in (110 * outCircle)..(109 * countIndex+outCircle)) { //для одной посылки
                if (n == 110 * outCircle) {
                    sendPackTemp[count] = true //старт бит
                    count++
                    sendPackTemp[count] = false //доп.бит
                    count++
                }
                if (n > predel) { //если число битов меньше 110, то дополняем до 110 бит 0 и соответственно доп.единицами
                    sendPackTemp[count] = false //биты данных для дополнения посылки
                    count++
                    sendPackTemp[count] = true
                    count++
                } else {
                    sendPackTemp[count] = sendPack[n] //бит данных
                    count++
                    if (sendPack[n] == true) sendPackTemp[count] = false //доп. бит
                    else sendPackTemp[count] = true
                    count++
                }
                if (n == 109 * countIndex+outCircle) { //стоп-бит
                    sendPackTemp[count] = false
                    count++
                    sendPackTemp[count] = true
                }
            }
          //получили посылку без crc
            var countThirdCircleForCrc: Int = outCircle + countIndex * 109+3
            outCircle++
            var crcForPack = crcPack(sendPackTemp) //получили crc
            var sizeCircle = crcForPack.length()
            if (crcForPack.length() < 16) { //если crc меньше 16 бит
                for (n in 0..(16-crcForPack.length()-1)) { //добавляем доп. нули чтобы дополнить ее до 16 бит
                    sendPackTemp[countThirdCircleForCrc] = false
                    countThirdCircleForCrc++
                    sendPackTemp[countThirdCircleForCrc] = true
                    countThirdCircleForCrc++
                }
            }
            for (n in 0..(sizeCircle)) { //добавляем в посылку crc
                sendPackTemp[countThirdCircleForCrc] = crcForPack[n]
                countThirdCircleForCrc++
                if (n == sizeCircle - 1) { //стоп-бит
                    sendPackTemp[countThirdCircleForCrc++] = false
                    countThirdCircleForCrc++
                    sendPackTemp[countThirdCircleForCrc++] = true
                }
            }
            unitPackSend = sendPackTemp
            ListArraySend.add(unitPackSend) //добавляем в глобальную переменную преобразованные данные для дальнейшей отправки
            countIndex++
        } while (outCircle <= size-1)
    }

    private suspend fun sendInformation() {
        var buffersize = AudioTrack.getMinBufferSize(
            44100,  //устанавливаем частоту с запасом
            AudioFormat.CHANNEL_OUT_MONO, // данные идут в левый канал, поэтому моно
            AudioFormat.ENCODING_PCM_16BIT)
        var trackplayer = AudioTrack(
            AudioAttributes.Builder().setUsage(USAGE_MEDIA) //устанавливаем соответствующие настройки для объекта api
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setLegacyStreamType(STREAM_MUSIC)
                .build(),
            AudioFormat.Builder().setEncoding(ENCODING_PCM_16BIT)
                .setChannelMask(CHANNEL_OUT_MONO)
                .setSampleRate(44100)
                .build(),
            buffersize,
            AudioTrack.MODE_STREAM,
            AUDIO_SESSION_ID_GENERATE)
        trackplayer.play() // включаем проигрывание
        var arrayData: BitSet = BitSet(ListArraySend.count()*256)
        var tempcountArray=0
        withContext(Dispatchers.Main) {
            var arrayText = InfoAddFiles.text.split("...")
            InfoAddFiles.setTextColor(Color.BLUE)
            InfoAddFiles.setText(arrayText[0].trimEnd() + " ... Подготовка данных " )
        }
        for (n in ListArraySend)
        {
            if (flagSend) {
                for (t in 0..255) {
                    arrayData[tempcountArray] = n[t]
                    tempcountArray++
                } }
            else
            {   withContext(Dispatchers.Main)
                {
                    var arrayText = InfoAddFiles.text.split("...")
                    InfoAddFiles.setTextColor(Color.RED)
                    InfoAddFiles.setText(arrayText[0]+" ... Прервано")
                }
                return
            } }
        if (flagSend) {
            withContext(Dispatchers.Main) {
                var arrayText = InfoAddFiles.text.split("...")
                InfoAddFiles.setTextColor(Color.BLACK)
                InfoAddFiles.setText(arrayText[0].trimEnd() + " ... Идет передача" )
            }
            trackplayer.write(
                arrayData.toByteArray(),
                0,
                buffersize
            ) // пишем данные в цап мобильного
            withContext(Dispatchers.Main)
            {
                var arrayText = InfoAddFiles.text.split("...")
                InfoAddFiles.setTextColor(Color.GREEN)
                InfoAddFiles.setText(arrayText[0].trimEnd()+ " ... Отправлено!")
            }
        }
        else {
            withContext(Dispatchers.Main)
            {
                var arrayText = InfoAddFiles.text.split("...")
                InfoAddFiles.setTextColor(Color.RED)
                InfoAddFiles.setText(arrayText[0].trimEnd()+" ... Прервано")
            }
            return
        } }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111 && resultCode == RESULT_OK) {
            flagComplete=false
            if (InfoAddFiles.text.contains("Отправлено!")) {
                arrayFiles[0] = null
                arrayFiles[1] = null
                arrayFiles[2] = null
                count = 0
                val pasw = ListArraySend[0]
                val name = ListArraySend[2]
                ListArraySend.clear()
                GlobalScope.launch() {
                ListArraySend.add(pasw)
                splitPackage()
                ListArraySend.add(name)
                splitPackage() }
            }
            var selectedFile = data?.data //The uri with the location of the file
            var matchResult = Regex("""([^%2F]*)$""").find(selectedFile.toString())
            if (matchResult === null) {
                Toast.makeText(this, "Ничего не выбрано для отправки.", Toast.LENGTH_SHORT).show()
                return
            }
            if (count >= 3) {
                Toast.makeText(this, "Вы не можете выбрать больше трех файлов.", Toast.LENGTH_SHORT).show()
                return
            }
            arrayFiles[count]=selectedFile
            count++
            for (file in arrayFiles) {
            if (file != null) {//если выбраны файлы получаем поток байт из  файла
                if ((file==arrayFiles[0] && arrayFiles[1]==null && arrayFiles[2]==null) || (file==arrayFiles[1] && arrayFiles[0]!=null && arrayFiles[2]==null) || (file==arrayFiles[2] && arrayFiles[1]!=null && arrayFiles[0]!==null))
                { val fileUser= contentResolver.openInputStream(file)?.readBytes() //получаем поток байт из  файла
                if (fileUser != null) {//если файл не пустой, то: получаем имя и расширение
                    var returnCursor = contentResolver.query(file, null, null, null, null);
                    var nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    returnCursor.moveToFirst()
                    var nameFile= returnCursor.getString(nameIndex) //имя и расширение вместе

                    if(count == 1) InfoAddFiles.text = nameFile //вывод имени файла
                    else {
                        InfoAddFiles.append(System.getProperty("line.separator"))
                        InfoAddFiles.append(nameFile) }
                    var arrayNameExection = nameFile.split('.') //разбиваем на массив
                    val nameFileStr = arrayNameExection[0].toByteArray() //имя
                    val exectionFileStr = arrayNameExection[1].toByteArray() //расширение без точки

                    val keyForUnitsName = "LightName".toByteArray() //это ключ для шифрования имени файла
                    val encoderN: RC4 = RC4(keyForUnitsName)
                    val encoderResultNameFile= encoderN.encode(nameFileStr, nameFileStr.size) //шифрование имени файла

                    val keyForUnitsFormat = "LightFormat".toByteArray() //это ключ для шифрования формата файла
                    val encoderF: RC4 = RC4(keyForUnitsFormat)
                    val encoderResultFormatFile= encoderF.encode(exectionFileStr, exectionFileStr.size) //шифрование формата файла

                    val keyForUnitsFile = "LightFile".toByteArray() //это ключ для шифрования данных файла
                    val encoderFile: RC4 = RC4(keyForUnitsFile)
                    val encoderResultFile= encoderFile.encode(fileUser, fileUser.size) //шифрование файла
                    InfoAddFiles.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0f)
                    var arrayText = InfoAddFiles.text.split("...")
                    InfoAddFiles.setTextColor(Color.BLUE)
                    InfoAddFiles.setText(arrayText[0].trimEnd()+" ... Пожалуйста, подождите")
                    GlobalScope.launch() {
                        packageCreate(encoderResultNameFile) //формирование посылки для имени файла
                        splitPackage()
                        packageCreate(encoderResultFormatFile) //для формата
                        splitPackage()
                        packageCreate(encoderResultFile) //для файла
                        splitPackage()
                        flagComplete=true
                    }
                    GlobalScope.launch() {
                        while(!flagComplete){}
                        withContext(Dispatchers.Main) {
                            InfoAddFiles.setTextColor(Color.GRAY)
                            InfoAddFiles.setText(arrayText[0])
                        } }
                    /*схема дешифрования:
                    val decoder: RC4 = RC4(keyForUnitsName)
                    var decoderResult = decoder.Decode(encoderResultNameFile, encoderResultNameFile.size)
                    val resultStrDecoder = decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования


                    val decoderF: RC4 = RC4(keyForUnitsFormat)
                    decoderResult = decoderF.Decode(encoderResultFormatFile, encoderResultFormatFile.size)
                    val resultStrDecoderF = decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования

                    val decoderFile: RC4 = RC4(keyForUnitsFile)
                    decoderResult = decoderFile.Decode(encoderResultFile, encoderResultFile.size)
                    val resultStrDecoderFile = decoderResult.toString(Charsets.UTF_8) //смотрим что получили после дешифрования
                    var t3 = resultStrDecoderFile*/
                } } } } } }

    fun sendMain (view: View) {
        val sendMain = Intent(this, MainActivity::class.java)
        startActivity(sendMain)
    }
}