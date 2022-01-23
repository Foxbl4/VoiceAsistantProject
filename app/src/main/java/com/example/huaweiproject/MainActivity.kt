package com.example.huaweiproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.huaweiproject.assistant.AssistantActivity
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var imageActionButton: ImageButton
    private val recordAudioRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageActionButton = findViewById(R.id.action_button)

        if(ContextCompat.checkSelfPermission(this, Manifest
                .permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){ checkPermission() }
        imageActionButton.setOnClickListener{
            startActivity(Intent(this, AssistantActivity::class.java))
        }

            val file = File("/storage/emulated/0/test.ppt")
            val uri = Uri.fromFile(file)
            if (file.exists()) {
                Log.d("TAG", "$uri")
            }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == recordAudioRequestCode && grantResults.isNotEmpty()){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermission(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission
                .RECORD_AUDIO), recordAudioRequestCode)
        }
    }
}