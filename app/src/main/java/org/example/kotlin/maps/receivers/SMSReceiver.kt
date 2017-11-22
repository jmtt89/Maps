package org.example.kotlin.maps.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import org.example.kotlin.maps.services.MusicService


class SMSReceiver : BroadcastReceiver() {
    private val TAG: String = "SMSReceiver"

    override fun onReceive(context: Context, intent: Intent) {

        val bundle = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<Any>
            val messages = arrayOfNulls<SmsMessage>(pdus.size)
            for (i in pdus.indices) {
                messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
            }

            for(message in messages){
                Log.i(TAG, "Message received: " + message?.messageBody + " from: " + message?.originatingAddress)
                /* Reproduce la musica */
                val musicIntent = Intent(context, MusicService::class.java)
                val stationId:Int? = message?.messageBody?.split(" ")?.last()?.toIntOrNull()

                if(stationId != null) {
                    musicIntent.putExtra("stationID", stationId)
                }

                musicIntent.action = "ACTION_PLAY"
                context.startService(musicIntent)
            }
        }
    }
}
