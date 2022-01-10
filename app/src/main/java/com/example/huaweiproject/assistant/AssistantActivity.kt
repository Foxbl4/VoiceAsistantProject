package com.example.huaweiproject.assistant

import android.animation.Animator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.hardware.camera2.CameraManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.huaweiproject.R
import com.example.huaweiproject.data.AssistantDB
import com.example.huaweiproject.databinding.ActivityAssistantBinding
import com.example.weathermap.constant.Units
import com.example.weathermap.implementation.OpenWeatherMapHelper
import com.example.weathermap.implementation.callback.CurrentWeatherCallback
import com.example.weathermap.model.currentweather.CurrentWeather
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ml.quaterion.text2summary.Text2Summary
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import android.speech.tts.Voice

class AssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssistantBinding
    private lateinit var assistantViewModel: AssistantViewModel

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var keeper: String

    private var REQUESTCALL = 1
    private var SENDSMS = 2
    private var READSMS = 3
    private var SHAREFILE = 4
    private var SHARETEXTFILE = 5
    private var READCONTACT = 6
    private var CAPTUREPHOTO = 7

    private var REQUEST_CODE_SELECT_DOC: Int = 100
    private var REQUEST_ENABLE_BT = 1000

    private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var cameraManager: CameraManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var cameraID: String
    private lateinit var ringtone: Ringtone

    private val logtts = "TTS"
    private val logsr = "SR"
    private val logkeeper = "keeper"

    private var imageIndex: Int = 0
    private lateinit var imgUri: Uri
    private lateinit var helper: OpenWeatherMapHelper
    private lateinit var englishRussianTranslator: com.google.mlkit.nl.translate.Translator


    @Suppress("DEPRECATION")
    private val imageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        .toString()+"/assistant/"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant)

        overridePendingTransition(R.anim.non_movable, R.anim.non_movable)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_assistant)

        val application = requireNotNull(this).application
        val dataSource = AssistantDB.getInstance(application).assistantDao
        val viewModelFactory = AssistantViewModelFactory(dataSource, application)

        assistantViewModel =
            ViewModelProvider(this, viewModelFactory)[AssistantViewModel::class.java]

        val adapter = AssistantAdapter()
        binding.recyclerview.adapter = adapter

        assistantViewModel.messages.observe(this, {
            it?.let {
                adapter.data = it
            }
        })
        binding.lifecycleOwner = this
        //animation
        if (savedInstanceState == null) {
            binding.assistantConstraintLayout.visibility = View.VISIBLE
            val viewTreeObserver: ViewTreeObserver =
                binding.assistantConstraintLayout.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver
                .OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        circularRevealActivity()
                        binding.assistantConstraintLayout.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraID = cameraManager.cameraIdList[0]
            //0 back camera
            //1 front camera
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        ringtone = RingtoneManager.getRingtone(
            applicationContext,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        helper = OpenWeatherMapHelper(getString(R.string.OPEN_WEATHER_API_KEY))

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result: Int = textToSpeech.setLanguage(Locale.ENGLISH)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e(logtts, "Language not supported")
                } else {
                    Log.e(logtts, "Language supported")
                }
            } else {
                Log.e(logtts, "Initialization failed")
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
            }

            override fun onBeginningOfSpeech() {
                Log.d("SR", "started")
            }

            override fun onRmsChanged(p0: Float) {
            }

            override fun onBufferReceived(p0: ByteArray?) {
                TODO("Not yet implemented")
            }

            override fun onEndOfSpeech() {
                Log.d("SR", "ended")
            }

            override fun onError(p0: Int) {
                TODO("Not yet implemented")
            }

            override fun onResults(bundle: Bundle?) {
                val data = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (data != null) {
                    keeper = data[0].lowercase(Locale.getDefault())
                    Log.d(logkeeper, keeper)
                    when {
                        /** Общение **/
                        keeper.contains("спасибо") -> speak("Это моя работа! Дай знать, если тебе понадобится что-то ещё")
                        keeper.contains("добро пожаловать") -> speak("Добро пожаловать для чего?")
                        keeper.contains("привет") -> speak("Привет, чем я могу тебе помочь?")
                        /** Маленькие задачки **/
                        keeper.contains("дата") -> getDate()
                        keeper.contains("время") -> getTime()
                        keeper.contains("расскажи шутку") -> joke()
                        keeper.contains("очистить") -> assistantViewModel.onClear()
                        keeper.contains("погода") -> weather()
                        /** Открытие приложений **/
                        keeper.contains("открой почту") -> openGmail()
                        keeper.contains("открой messenger") -> openWhatsApp()
                        keeper.contains("открой сообщения") -> openMessages()
                        keeper.contains("открой презентацию") -> openPowerPoint()
                        /** Звонки и СМС**/
                        keeper.contains("позвони контакту") -> callContact()
                        keeper.contains("позвони на номер") -> makePhoneCall()
                        keeper.contains("отправь сообщение") -> sendSMS()
                        keeper.contains("прочитай моё последнее сообщение") -> readSMS()
                        /** Весёлые плюшки **/
                        keeper.contains("включи bluetooth") -> turnOnBluetooth()
                        keeper.contains("выключи bluetooth") -> turnOffBluetooth()
                        keeper.contains("покажи устройство bluetooth") -> getAllPairedDevices()
                        keeper.contains("включи фонарь") -> turnOnFlash()
                        keeper.contains("выключи фонарь") -> turnOffFlash()
                        keeper.contains("включи музыку") -> playRingtone()
                        keeper.contains("выключи музыку") -> stopRingtone()
                        /** Арифметика **/
                        keeper.contains("+") -> sumNum(keeper)
                        keeper.contains("%") -> percentNum(keeper)
                        /** Не работающие **/
                        /**Не проверил*/keeper.contains("share a text message") -> shareTextMessage()
                        /**Не проверил*/keeper.contains("copy to clipboard") -> clipBoardCopy()
                        /**Не проверил*/keeper.contains("read last clipboard") -> clipBoardSpeak()
                        /**Не сохраняет*/keeper.contains("фото") -> capturePhoto()
                        /**Пуст*/keeper.contains("сигнал") -> setAlarm()
                        /**Пуст*/keeper.contains("question") -> question()
                        /**Не проверил*/keeper.contains("share a file") -> shareFile()
                        /**Пуст*/keeper.contains("medical") -> medicalApplication()

                        else -> speak("Invalid command, try again")
                    }
                }
            }

            override fun onPartialResults(p0: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                TODO("Not yet implemented")
            }

        })

        binding.assistantActionButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopListening()
                }

                MotionEvent.ACTION_DOWN -> {
                    textToSpeech.stop()
                    speechRecognizer.startListening(recognizerIntent)
                }
            }
            false
        }
        checkIfSpeechRecognizerAvailable()
    }


    private fun checkIfSpeechRecognizerAvailable() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.d(logsr, "yes")
        } else {
            Log.d(logsr, "false")
        }
    }

    fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        assistantViewModel.sendMessageToDataBase(keeper, text)
    }



    fun sumNum(keeper: String){
        val firstNum: Int = keeper.substringBefore(" +").toInt()
        val secondNum: Int = keeper.substringAfter("+ ").toInt()
        val sum = firstNum + secondNum
        speak("$firstNum + $secondNum = $sum")
    }

    fun percentNum(keeper: String){
        val firstNum: Int = keeper.substringBefore("%").toInt()
        val secondNum: Int = keeper.substringAfter("от ").toInt()
        val percent = ((firstNum * secondNum).toDouble() / 100).roundToInt()
        speak("$firstNum процентов от $secondNum это $percent")
    }

    fun getDate() {
        val calendar = Calendar.getInstance()
        val formattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
        val splitDate = formattedDate.split(",").toTypedArray()
        val date = splitDate[1].trim { it <= ' ' }
        speak("The date is $date")
    }

    fun getTime() {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("HH:mm:ss")
        val time: String = format.format(calendar.time)
        speak("The time is $time")
    }

    private fun makePhoneCall() {
        val keeperSplit = keeper.replace(" ".toRegex(), "").substringAfter("номер")
        //no space
        if (keeperSplit.trim { it <= ' ' }.isNotEmpty()) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.CALL_PHONE
                ) != PackageManager
                    .PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest
                            .permission.CALL_PHONE
                    ), REQUESTCALL
                )
            } else {
                val dial = "tel:$keeperSplit"
                speak("Calling $keeperSplit")
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
            }
        } else {
            Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSMS() {
        Log.d("keeper", "Done0")
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission
                    .SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission
                        .SEND_SMS
                ), SENDSMS
            )
            Log.d("keeper", "Done1")
        } else {
            Log.d("keeper", "Done2")
            val keeperReplaced = keeper.replace(" ".toRegex(), "")
            val number = keeperReplaced.substringAfter("номер")
            val message = keeper.substringAfter("сообщение").substringBefore(" на")
            Log.d("chk", number + message)
            val mySMSManager = SmsManager.getDefault()
            mySMSManager.sendTextMessage(number.trim { it <= ' ' }, null,
                message.trim { it <= ' ' }, null, null
            )
            speak("Message sent that $message")
        }
    }

    @SuppressLint("Recycle")
    private fun readSMS() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_SMS),
                READSMS
            )
        } else {
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"), null,
                null, null, null
            )
            cursor!!.moveToFirst()
            speak("Your last messages was: " + cursor.getString(12))
        }
    }

    private fun openMessages() {
        val intent =
            packageManager.getLaunchIntentForPackage(Telephony.Sms.getDefaultSmsPackage(this))
        intent?.let { startActivity(it) }
    }

    private fun openWhatsApp() {
        val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
        intent?.let { startActivity(intent) }
    }

    private fun openGmail() {
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
        intent?.let { startActivity(it) }
    }

    private fun openPowerPoint() {
        speak("Открываю приложение PowerPoint")
        val intent = packageManager.getLaunchIntentForPackage("com.microsoft.office.powerpoint")
        intent?.let { startActivity(it) }
    }

    private fun shareFile() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission
                        .READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), SHAREFILE
            )
        } else {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val myFileIntent = Intent(Intent.ACTION_GET_CONTENT)
            myFileIntent.type = "application/pdf"
            startActivityForResult(myFileIntent, REQUEST_CODE_SELECT_DOC)
        }
    }

    private fun shareTextMessage() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest
                    .permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest
                        .permission.WRITE_EXTERNAL_STORAGE
                ), SHARETEXTFILE
            )
        } else {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val message = keeper.split("that").toTypedArray()[1]
            val intentShare = Intent(Intent.ACTION_SEND)
            intentShare.type = "text/plain"
            intentShare.putExtra(Intent.EXTRA_TEXT, message)
            startActivity(Intent.createChooser(intentShare, "Sharing Text"))
        }
    }



    @SuppressLint("Recycle")
    private fun callContact() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest
                    .permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest
                        .permission.READ_CONTACTS
                ), READCONTACT
            )
        } else {
            val nameTemp = keeper.substringAfter("контакту").trim { it <= ' ' }
            val name = if(nameTemp.contains(" ")) {
                nameTemp.substringBefore(' ')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " " + nameTemp.substringAfter(' ')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }else{
                nameTemp.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            Log.d("chk", name)
            try {
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone
                        .CONTENT_URI, arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE
                    ), "DISPLAY_NAME= '$name'",
                    null, null
                )
                cursor!!.moveToFirst()
                val number = cursor.getString(0)
                if (number.trim { it <= ' ' }.isNotEmpty()) {
                    if (ContextCompat.checkSelfPermission(
                            this, android.Manifest
                                .permission.CALL_PHONE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this, arrayOf(
                                android.Manifest
                                    .permission.CALL_PHONE
                            ), REQUESTCALL
                        )
                    } else {
                        val dial = "tel:$number"
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
                    }
                } else {
                    Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                speak("Something went wrong")
            }
        }
    }



    private fun turnOnBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            speak("Turning On Bluetooth")
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, REQUEST_ENABLE_BT)
        } else {
            speak("Bluetooth is already On")
        }
    }

    private fun turnOffBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
            speak("Turning Bluetooth Off")
        } else {
            speak("Bluetooth is already Off")
        }
    }

    private fun getAllPairedDevices() {
        if (bluetoothAdapter.isEnabled) {
            speak("Paired Devices are ")
            var text = ""
            var count = 1
            val devices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            for (device in devices) {
                text += "\nDevice: $count ${device.name}, $device"
                count++
            }
            speak(text)
        } else {
            speak("Turn on bluetooth to get paired devices")
        }
    }

    private fun turnOnFlash() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraID, true)
                speak("Flash turned on")
            }
        } catch (e: java.lang.Exception) {
            speak("Error Occured")
        }
    }

    private fun turnOffFlash() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraID, false)
                speak("Flash turned off")
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun clipBoardCopy() {
        val data = keeper.split("that").toTypedArray()[1].trim { it <= ' ' }
        if (data.isNotEmpty()) {
            val clipData = ClipData.newPlainText("text", data)
            clipboardManager.setPrimaryClip(clipData)
            speak("Data copied to clipboard that is $data")
        }
    }

    fun clipBoardSpeak() {
        val item = clipboardManager.primaryClip!!.getItemAt(0)
        val pasteData = item.text.toString()
        if (pasteData != "") {
            speak("Data stored in last clipboard is $pasteData")
        } else {
            speak("Clipboard is Empty")
        }
    }

    private fun capturePhoto() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest
                    .permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest
                        .permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                CAPTUREPHOTO
            )
        } else {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            imageIndex++
            val file: String = "$imageDirectory$imageIndex.jpg"
            val newFile = File(file)
            try {
                newFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val outputFileUri = Uri.fromFile(newFile)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            startActivity(cameraIntent)
            speak("Photo will be saved to $file")
        }
    }

    private fun playRingtone() {
        speak("Playing Ringtone")
        ringtone.play()
    }

    private fun stopRingtone() {
        speak("Ringtone Stopped")
        ringtone.stop()
    }

    private fun readMe() {
        CropImage.startPickImageActivity(this)
    }

    private fun getTextFromBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).addOnSuccessListener { visionText ->
            val resultText = visionText.text
            if (keeper.contains("summarise")) {
                speak("Reading Image Summarising it: \n" + summariseText(resultText))
            } else {
                speak("Reading Image:\n$resultText")
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error" + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun summariseText(text: String): String {
        val summary: kotlin.jvm.internal.Ref.ObjectRef<*> =
            kotlin.jvm.internal.Ref.ObjectRef<Any?>()
        summary.element = Text2Summary.Companion.summarize(text, 0.4f)
        return summary.element as String
    }

    private fun setAlarm() {}

    private fun medicalApplication() {

    }

    private fun translatePrepare(){
        val options = TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.RUSSIAN).build()
        englishRussianTranslator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder().requireWifi().build()

        englishRussianTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d("translateOpt","Model downloaded successfully. Okay to start translating")

            }
            .addOnFailureListener { exception ->
                Log.d("translateOpt",
                    "Model couldn’t be downloaded or other internal error: $exception")
            }
    }

     fun translateEnglishToRussian(text: String):String{
         var translateText = ""
        translatePrepare()
        englishRussianTranslator.translate(text)
            .addOnSuccessListener { translatedText ->
                translateText = translatedText
                Log.d("translate","Translation successful")
            }
            .addOnFailureListener { exception ->
                Log.d("translate","Error $exception")
            }
         return translateText
    }

    private fun weather() {

        /*
        if (keeper.contains("Fahrenheit")) {
            helper.setUnits(Units.IMPERIAL)
        } else if (keeper.contains("Celsius")) {
            helper.setUnits(Units.METRIC)
        }
        */
        helper.setUnits(Units.METRIC)
        val keeperSplit = keeper.replace(" ".toRegex(), "|").split("|")
            .toTypedArray()
        val city = keeperSplit[1]
        helper.getCurrentWeatherByCityName(city, object : CurrentWeatherCallback {
            override fun onSuccess(currentWeather: CurrentWeather?) {
                if (currentWeather != null) {
                    speak(
                        """ 
                            Осадки: ${currentWeather.weather[0].description}
                            Температура: ${currentWeather.main.temp}
                            Влажность: ${currentWeather.main.humidity}
                            Атмосферное давление: ${currentWeather.main.pressure}
                            Скорость ветра: ${currentWeather.wind.speed}
                             """.trimIndent()
                    )
                }
            }

            override fun onFailure(throwable: Throwable?) {
                if (throwable != null) {
                    speak("Error" + throwable.message)
                }
            }
        })

    }

    private fun joke() {
        speak("Ты разбираешься в электросхемах? Ну, плюс-минус")
    }

    private fun question() {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTCALL) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == SENDSMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSMS()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == READSMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSMS()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == SHAREFILE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareFile()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == SHARETEXTFILE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareTextMessage()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == READCONTACT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callContact()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == CAPTUREPHOTO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturePhoto()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== REQUEST_CODE_SELECT_DOC && resultCode == RESULT_OK) {
            val filePath = data?.data?.path
            Log.d("chk", "path: $filePath")
            val file = File(filePath)
            val intentShare = Intent(Intent.ACTION_SEND)
            intentShare.type = "application/pdf"
            intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://$file"))
            startActivity(Intent.createChooser(intentShare, "Share the file ..."))
        }
        if (requestCode == REQUEST_ENABLE_BT){
            if(requestCode == RESULT_OK){
                speak("Bluetooth is on")
            }else{
                speak("Could not able to turn on Bluetooth")
            }
        }
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && requestCode == RESULT_OK){
            val imageUri = CropImage.getPickImageResultUri(this, data)
            imgUri = imageUri
            startCrop(imageUri)
        }
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            val result: CropImage.ActivityResult = CropImage.getActivityResult(data)
            if(resultCode == RESULT_OK){
                imgUri = result.uri
                try {
                    val inputStream = contentResolver.openInputStream(imgUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    getTextFromBitmap(bitmap)
                }catch (e: FileNotFoundException){
                    e.printStackTrace()
                }
            Toast.makeText(this, "Image Captured Successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCrop(imageUri: Uri){
        CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON)
            .setMultiTouchEnabled(true).start(this)
    }

    private fun circularRevealActivity(){
        val cx: Int = binding.assistantConstraintLayout.right - getDips(44)
        val cy: Int = binding.assistantConstraintLayout.bottom - getDips(44)
        val finalRadius: Int = Math.max(
            binding.assistantConstraintLayout.width,
            binding.assistantConstraintLayout.height,
        )
        val circularReveal = ViewAnimationUtils.createCircularReveal(
            binding.assistantConstraintLayout,
            cx,
            cy,
            0f,
            finalRadius.toFloat()
        )
        circularReveal.duration = 1250
        binding.assistantConstraintLayout.visibility = View.VISIBLE
        circularReveal.start()
    }

    private fun getDips(dps: Int):Int{
        val resources: Resources = resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dps.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onBackPressed() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            val cx: Int = binding.assistantConstraintLayout.width - getDips(44)
            val cy: Int = binding.assistantConstraintLayout.height - getDips(44)
            val finalRadius: Int = Math.max(
                binding.assistantConstraintLayout.width,
                binding.assistantConstraintLayout.height
            )
            val circularReveal = ViewAnimationUtils.createCircularReveal(
                binding.assistantConstraintLayout, cx, cy, finalRadius.toFloat(), 0f
            )

            circularReveal.addListener(object : Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator?) {
                    TODO("Not yet implemented")
                }

                override fun onAnimationEnd(animation: Animator?) {
                    binding.assistantConstraintLayout.visibility = View.INVISIBLE
                    finish()
                }

                override fun onAnimationCancel(p0: Animator?) {
                    TODO("Not yet implemented")
                }

                override fun onAnimationRepeat(p0: Animator?) {
                    TODO("Not yet implemented")
                }
            })
            circularReveal.duration = 1250
            circularReveal.start()
        }else{
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        speechRecognizer.cancel()
        speechRecognizer.destroy()
        Log.i(logsr, "destroy")
        Log.i(logtts, "destroy")
    }


}
