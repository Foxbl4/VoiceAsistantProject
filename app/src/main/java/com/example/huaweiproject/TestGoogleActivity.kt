package com.example.huaweiproject

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import com.gun0912.tedpermission.PermissionListener
//import com.gun0912.tedpermission.TedPermission
//import com.itsrts.pptviewer.PPTViewer


class TestGoogleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_google)
/*
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(this@TestGoogleActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
                val pptViewer: PPTViewer = findViewById(R.id.pptviewer)
                pptViewer.setNext_img(R.drawable.ic_baseline_navigate_next_24)
                    .setPrev_img(R.drawable.ic_baseline_arrow_back_ios_24)
                    .setSettings_img(R.drawable.ic_baseline_settings_24)
                    .setZoomin_img(R.drawable.ic_baseline_zoom_in_24)
                    .setZoomout_img(R.drawable.ic_baseline_zoom_out_24)
                pptViewer.loadPPT(this@TestGoogleActivity, "/storage/emulated/0/bluetooth/test.ppt")
            }

            override fun onPermissionDenied(deniedPermissions: List<String?>) {
                Toast.makeText(
                    this@TestGoogleActivity,
                    "Permission Denied\n$deniedPermissions",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .check()

 */
    }
}

