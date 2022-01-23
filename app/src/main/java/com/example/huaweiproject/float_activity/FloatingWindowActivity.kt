package com.example.huaweiproject.float_activity

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import com.example.huaweiproject.R
import com.example.huaweiproject.assistant.AssistantActivity
import com.example.huaweiproject.assistant.AssistantViewModelFactory
import com.example.huaweiproject.data.AssistantDB
import com.example.huaweiproject.float_activity.Common.Companion.currDes
import java.util.*

class FloatingWindowActivity: Service() {

    private lateinit var floatView: ViewGroup
    private lateinit var floatWindowLayoutParams: WindowManager.LayoutParams
    private var LAYOUT_TYPE: Int? = null
    private lateinit var windowManager: WindowManager
    lateinit var speechRecognizer: SpeechRecognizer
    lateinit var textToSpeech: TextToSpeech
    lateinit var recognizerIntent: Intent

    private lateinit var btnClose: ImageButton
    lateinit var btnFloat: ImageView
    private lateinit var keeper: String

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        val metrics = applicationContext.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = baseContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        floatView = inflater.inflate(R.layout.floating_layout, null) as ViewGroup


        btnClose = floatView.findViewById(R.id.window_close)
        btnFloat = floatView.findViewById(R.id.content_button)

        btnFloat.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopListening()
                }

                MotionEvent.ACTION_DOWN -> {
                    btnFloat.setImageResource(R.drawable.ic_baseline_mic_24_2)
                    textToSpeech.stop()
                    speechRecognizer.startListening(recognizerIntent)
                }
            }
            false
        }

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result: Int = textToSpeech.setLanguage(Locale.ENGLISH)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("logtts", "Language not supported")
                } else {
                    Log.e("logtts", "Language supported")
                }
            } else {
                Log.e("logtts", "Initialization failed")
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
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
                btnFloat.setImageResource(R.drawable.ic_baseline_mic_24)
            }

            override fun onResults(bundle: Bundle?) {
                val data = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (data != null) {
                    keeper = data[0].lowercase(Locale.getDefault())
                    currDes = keeper
                    Log.d("logkeeper", keeper)
                    when {
                        /** Общение **/
                        keeper.contains("спасибо") -> speak("Это моя работа! Дай знать, если тебе понадобится что-то ещё")
                        keeper.contains("вернись") -> returnHome()
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

        LAYOUT_TYPE = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }else{
            WindowManager.LayoutParams.TYPE_TOAST
        }

        floatWindowLayoutParams = WindowManager.LayoutParams(
            (width*0.25f).toInt(),
            (height*0.1f).toInt(),
            LAYOUT_TYPE!!,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        floatWindowLayoutParams.gravity = Gravity.CENTER
        floatWindowLayoutParams.x = 0
        floatWindowLayoutParams.y = 0

        windowManager.addView(floatView, floatWindowLayoutParams)

        btnClose.setOnClickListener{ onDestroy() }

        floatView.setOnTouchListener(object : View.OnTouchListener{

            val updatedFloatWindowLayoutParam = floatWindowLayoutParams
            var x = 0.0
            var y = 0.0
            var px = 0.0
            var py = 0.0

            override fun onTouch(p0: View?, event: MotionEvent?): Boolean {

                when(event!!.action){
                    MotionEvent.ACTION_DOWN -> {
                        x = updatedFloatWindowLayoutParam.x.toDouble()
                        y = updatedFloatWindowLayoutParam.y.toDouble()
                        px = event.rawX.toDouble()
                        py = event.rawY.toDouble()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        updatedFloatWindowLayoutParam.x =( x + event.rawX - px).toInt()
                        updatedFloatWindowLayoutParam.y =( y + event.rawY - py).toInt()
                        windowManager.updateViewLayout(floatView, updatedFloatWindowLayoutParam)
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
    }

    private fun returnHome() {
        speak("Возвращаюсь в наше приложение")
        stopAll()
        val goBack = Intent(this@FloatingWindowActivity, AssistantActivity::class.java)
        goBack.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(goBack)
    }

    fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        currDes += "\n$text"
    }

    private fun stopAll(){
        stopSelf()
        windowManager.removeView(floatView)
        textToSpeech.stop()
        textToSpeech.shutdown()
        speechRecognizer.cancel()
        speechRecognizer.destroy()
    }
}