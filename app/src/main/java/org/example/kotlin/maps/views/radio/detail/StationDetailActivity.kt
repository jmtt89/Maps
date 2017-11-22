package org.example.kotlin.maps.views.radio.detail

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.gson.Gson
import org.example.kotlin.maps.R

import kotlinx.android.synthetic.main.activity_station_detail.*
import kotlinx.android.synthetic.main.content_station_detail.*

import org.example.kotlin.maps.model.Station
import org.example.kotlin.maps.views.map.MapsActivity

import java.net.URL
import com.squareup.picasso.Picasso
import org.example.kotlin.maps.MediaPlayerNotification
import org.example.kotlin.maps.services.MusicService


class StationDetailActivity : AppCompatActivity() {
    var mBound = false
    var mService: MusicService? = null

    /** Defines callbacks for service binding, passed to bindService()  */
    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to MusicService, cast the IBinder and get MusicService instance
            val binder = service as MusicService.LocalBinder
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    /*
 * Lo ideal aqui fuese crear una clase que controlara la comunicacion con el api, pero por cuestiones
 * de tiempo lo dejo dentro de los AsyncTask directamente
 */
    inner class FetchStation(val context: Context): AsyncTask<Int, Void, Station>(){
        private val picasso: Picasso = Picasso.with(context)
        private val apiKey: String = context.resources.getString(R.string.dirble_api_key)
        private val gson: Gson = Gson()

        override fun doInBackground(vararg params: Int?): Station {
            var apiCall = "http://api.dirble.com/v2/station/" + params[0] + "?token="+apiKey
            val result = URL(apiCall).readText()
            return gson.fromJson(result, Station::class.java)
        }

        override fun onPostExecute(result: Station?) {
            super.onPostExecute(result)
            if(result != null){
                station_name.text = result.name

                if(result.image?.url != null){
                    logo.visibility = View.VISIBLE
                    picasso.load(result.image.url).into(logo)
                }else{
                    logo.visibility = View.GONE
                }

                listeners.text = context.resources.getString(R.string.lbl_listeners, result.total_listeners)
                category.text = result.categories[0].title
                description.text = result.categories[0].description

                create_at.text = result.create_at.toString()
                update_at.text = result.update_at.toString()


                btn_music_action.setText(if(mService?.actualInPlay()?.id == result.id) R.string.btn_stop else R.string.btn_play)

                btn_music_action.setOnClickListener {
                    if(mService?.actualInPlay()?.id != result.id){
                        btn_music_action.setText(R.string.btn_buffering)
                        btn_music_action.isEnabled = false

                        //Se detiene la reproduccion anterior y se ejecuta la nueva
                        play(result, { valid: Boolean ->
                            btn_music_action.isEnabled = true
                            if(valid){
                                btn_music_action.setText(R.string.btn_stop)
                            }else{
                                btn_music_action.setText(R.string.btn_play)
                            }
                        })
                    }else{
                        pause()
                        btn_music_action.text = context.getString(R.string.btn_play)
                        MediaPlayerNotification.cancel(context)
                    }
                }


                val pageId = result.facebook?.split("/")?.last()
                if(pageId != null){
                    btn_open_facebook.visibility = View.VISIBLE
                    btn_open_facebook.setOnClickListener {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + pageId)))
                        } catch (e: Exception) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + pageId)))
                        }

                    }
                }else{
                    btn_open_facebook.visibility = View.GONE
                }


                val twitterId = result.twitter
                if(twitterId != null){
                    btn_open_twitter.visibility = View.VISIBLE
                    btn_open_twitter.setOnClickListener {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=" + twitterId)))
                        } catch (e: Exception) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/#!/" + twitterId)))
                        }
                    }
                }else{
                    btn_open_twitter.visibility = View.GONE
                }


                val webUrl = result.website
                if(webUrl != null){
                    btn_open_web.visibility = View.VISIBLE
                    btn_open_web.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                        startActivity(intent)
                    }
                }else{
                    btn_open_web.visibility = View.GONE
                }


                btn_share.setOnClickListener {
                    val sendIntent = Intent(Intent.ACTION_VIEW)
                    sendIntent.data = Uri.parse("sms:")
                    sendIntent.putExtra("sms_body", "StationId "+result.id)
//                    sendIntent.type = "vnd.android-dir/mms-sms"
                    startActivity(sendIntent)
                }

            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_detail)
        setSupportActionBar(toolbar)

        val stationId = intent.extras.getInt("stationId", -1)

        FetchStation(this).execute(stationId)

        fab.setOnClickListener { _ ->
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        // Bind to MusicService
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection)
            mBound = false
        }
        super.onStop()
    }

    fun play(station: Station, callback: (valid: Boolean) -> Unit) {
        if(mBound){
            mService?.play(station, callback)
        }else{
            callback(false)
        }
    }

    fun pause() {
        if(mBound){
            mService?.pause()
        }
    }

    fun share(station: Station) {
        val sendIntent = Intent(Intent.ACTION_VIEW)
        sendIntent.data = Uri.parse("sms:")
        sendIntent.putExtra("sms_body", "StationId " + station.id)
//                    sendIntent.type = "vnd.android-dir/mms-sms"
        startActivity(sendIntent)
    }

}
