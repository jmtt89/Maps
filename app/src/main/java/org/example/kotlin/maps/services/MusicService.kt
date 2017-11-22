package org.example.kotlin.maps.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager

import android.media.MediaPlayer
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import org.example.kotlin.maps.MediaPlayerNotification
import org.example.kotlin.maps.R
import org.example.kotlin.maps.model.Station

import java.net.URL


class MusicService : Service() {
    // Binder given to clients
    private val mBinder = LocalBinder()
    private lateinit var player: MediaPlayer
    private var lastStation: Station? = null
    private var actualStation: Station? = null

    inner class FetchStation: AsyncTask<Int, Void, Station>() {
        private val apiKey: String = applicationContext.resources.getString(R.string.dirble_api_key)
        private val gson: Gson = Gson()

        override fun doInBackground(vararg params: Int?): Station {
            val apiCall = "http://api.dirble.com/v2/station/" + params[0] + "?token=" + apiKey
            val result = URL(apiCall).readText()
            return gson.fromJson(result, Station::class.java)
        }

        override fun onPostExecute(result: Station?) {
            super.onPostExecute(result)
            if(result != null){
                play(result, {
                    MediaPlayerNotification.notify(applicationContext, result, true)
                })
            }
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal// Return this instance of MusicService so clients can call public methods
        val service: MusicService
            get() = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        player = MediaPlayer()
        player.setAudioStreamType(AudioManager.STREAM_MUSIC)
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        handleIntent(intent)
        return START_NOT_STICKY //super.onStartCommand(intent, flags, startId)
    }

    fun play(station: Station, callback: (valid: Boolean) -> Unit){
        if(player.isPlaying){
            player.stop()
        }
        player.reset()

        var streamUrl:String? = (0 until station.streams.size)
                .firstOrNull { station.streams[it].content_type.contains("mpeg", true) }
                ?.let { station.streams[it].stream }
        if(streamUrl == null){
            streamUrl = station.streams[0].stream
        }

        player.setDataSource(streamUrl)
        player.setOnPreparedListener {
            MediaPlayerNotification.notify(applicationContext, station, true)
            player.start()
            lastStation = station
            actualStation = station
            applicationContext
                    .getSharedPreferences("SESSION", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("LAST_STATION_ID", station.id)
                    .apply()
            callback(true)
        }
        player.setOnErrorListener { _, _, _->
            Log.e("ERROR","Error Playing Station")
            lastStation = null
            actualStation = null
            Toast.makeText(this, R.string.error_playing, Toast.LENGTH_LONG).show()
            callback(false)
            true
        }
        player.prepareAsync()
    }

    fun actualInPlay():Station? = actualStation

    private fun play(stationId:Int){
        FetchStation().execute(stationId)
    }

    fun pause(){
        actualStation = null
        player.pause()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action == null)
            return

        val defaultId = applicationContext
                .getSharedPreferences("SESSION", Context.MODE_PRIVATE)
                .getInt("LAST_STATION_ID", 56926)

        val action = intent.action
        var id = intent.extras?.getInt("stationID", defaultId)
        if(id == null){
            id = defaultId
        }

        when {
            action.equals("ACTION_PLAY", ignoreCase = true) -> play(id)
            action.equals("ACTION_STOP", ignoreCase = true) -> {
                pause()
                if(lastStation != null){
                    MediaPlayerNotification.notify(applicationContext, lastStation!!, false)
                }
            }
            action.equals("ACTION_DELETE",ignoreCase = true) -> pause()
        }
    }
}
