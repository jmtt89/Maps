package org.example.kotlin.maps

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import org.example.kotlin.maps.model.Station
import com.squareup.picasso.Picasso
import org.example.kotlin.maps.services.MusicService
import org.example.kotlin.maps.views.radio.detail.StationDetailActivity

/**
 * Helper class for showing and canceling media player
 * notifications.
 *
 *
 * This class makes heavy use of the [NotificationCompat.Builder] helper
 * class to create notifications in a backward-compatible way.
 */
object MediaPlayerNotification {
    /**
     * The unique identifier for this type of notification.
     */
    private val NOTIFICATION_TAG = "MediaPlayer"

    /**
     * Shows the notification, or updates a previously shown notification of
     * this type, with the given parameters.
     */
    fun notify(context: Context, station: Station, isPlaying:Boolean) {

        Thread(Runnable {
            val res = context.resources

            // This image is used as the notification's large icon (thumbnail).
            val picture = Picasso.with(context).load(station.image?.thumb?.url).get()

            val title = station.country + " - " + station.name
            val text = station.categories[0].title

            val intent = Intent(context, MusicService::class.java)
            intent.action = "ACTION_DELETE"
            val stopPendingIntent = PendingIntent.getService(context, 1, intent, 0)

            val intentCl = Intent(context, StationDetailActivity::class.java)
            intentCl.putExtra("stationId", station.id)

            val notification = NotificationCompat.Builder(context)
                    //.setDefaults(Notification.DEFAULT_ALL)
                    .setSmallIcon(R.drawable.ic_stat_media_player)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    //Lo que se ejecuta al clickear sobre la notificacion
                    // Set the pending intent to be initiated when the user touches
                    // the notification.
                    .setContentIntent(
                            PendingIntent.getActivity(
                                    context,
                                    0,
                                    intentCl,
                                    PendingIntent.FLAG_UPDATE_CURRENT))
                    //En algunos dispositivos viejos se muestra esto primero
                    .setTicker(title)
                    //Esto es paraver donde va a aparecer
                    .setNumber(station.total_listeners ?: 0)
                    //Cuando el usuario elimina la notificacion llama esto
                    .setDeleteIntent(stopPendingIntent)
                    //Se agrega el estilo de Media Player
                    .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle())
                    .setAutoCancel(false)

            if(picture != null){
                notification.setLargeIcon(picture)
            }

            if(isPlaying){
                //Acion de "Pausar"
                notification.addAction( generateAction(context, android.R.drawable.ic_media_pause, "Pause", "ACTION_STOP" ))
            }else{
                //Acion de "Reanudar"
                notification.addAction( generateAction(context, android.R.drawable.ic_media_play, "Play", "ACTION_PLAY" ))
            }

            notify(context, notification.build())
        }).start()
    }


    private fun generateAction(context: Context, icon: Int, title: String, intentAction: String): NotificationCompat.Action {
        val intent = Intent(context, MusicService::class.java)
        intent.action = intentAction
        val pendingIntent = PendingIntent.getService(context, 1, intent, 0)
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun notify(context: Context, notification: Notification) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_TAG, 0, notification)
    }

    /**
     * Cancels any notifications of this type previously shown using
     * [.notify].
     */
    fun cancel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_TAG, 0)
    }


}
