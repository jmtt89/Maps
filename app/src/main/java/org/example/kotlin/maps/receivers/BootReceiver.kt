package org.example.kotlin.maps.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.example.kotlin.maps.services.CheckLocationService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED){
            val serviceIntent = Intent(context, CheckLocationService::class.java)
            context.startService(serviceIntent)
        }
    }
}
