package org.example.kotlin.maps.views

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.example.kotlin.maps.R

class MainActivity : AppCompatActivity() {
    val requestLocation:Int = 34
    val requestSms = 35
    val requestTeleponyManager = 36

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }
}
