package org.example.kotlin.maps.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.example.kotlin.maps.services.CheckLocationService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED){
            val serviceIntent = Intent(context, CheckLocationService::class.java)
            /*
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(serviceIntent)
            }else{
                context.startService(serviceIntent)
            }
            */
            context.startService(serviceIntent)
        }
    }
}
