package com.example.huaweiproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.huaweiproject.assistant.AssistantActivity


class MainActivity : AppCompatActivity() {

    private lateinit var imageActionButton: ImageButton
    val recordAudioRequestCode: Int = 1

    /**Тестим FloatWindows*/
    //private lateinit var dialog: AlertDialog
    //private lateinit var btnMin: Button
    //private lateinit var editDes: EditText
    /**Конец теста FloatWindows*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageActionButton = findViewById(R.id.action_button)

        if(ContextCompat.checkSelfPermission(this, Manifest
                .permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            checkPermission()
        }
        imageActionButton.setOnClickListener{
            startActivity(Intent(this, AssistantActivity::class.java))
        }

/*
        /**Тестим FloatWindows*/
        btnMin = findViewById(R.id.send_btn)
        editDes = findViewById(R.id.edit_des)

        if(isServiceRunning()){
            stopService(Intent(this@MainActivity, FloatingWindowActivity::class.java))
        }

        editDes.setText(currDes)
        editDes.setSelection(editDes.text.toString().length)

        editDes.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                currDes = editDes.text.toString()
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        btnMin.setOnClickListener{
            if(checkOverlayPermission()){
                startService(Intent(this@MainActivity, FloatingWindowActivity::class.java))
                finish()
            }else{
                requestFloatingWindowPermission()
            }
        }
        /**Конец теста FloatWindows*/
   */
    }
    /**Тестим FloatWindows*/
/*
    private fun isServiceRunning(): Boolean{
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)){
            if (FloatingWindowActivity::class.java.name == service.service.className){
                return true
            }
        }
        return false
    }

    private fun requestFloatingWindowPermission(){
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle("Screen Overlay Permission Needed")
        builder.setMessage("Enable 'Display over the App' from settings")
        builder.setPositiveButton("Open Settings", DialogInterface.OnClickListener{dialog, which ->
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package: $packageName"))
            startActivityForResult(intent, RESULT_OK)
        })
        dialog = builder.create()
        dialog.show()
    }

    private fun checkOverlayPermission(): Boolean{
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            Settings.canDrawOverlays(this)
        }else{
            return true
        }
    }
*/
    /**Конец теста FloatWindows*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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