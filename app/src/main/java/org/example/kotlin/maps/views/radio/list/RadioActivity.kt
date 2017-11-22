package org.example.kotlin.maps.views.radio.list

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_radio.*

import org.example.kotlin.maps.database.Database
import org.example.kotlin.maps.database.DatabasePreferences

import org.example.kotlin.maps.model.Category
import org.example.kotlin.maps.model.Country
import java.net.URL
import android.widget.ArrayAdapter
import android.content.Intent
import android.content.ComponentName
import org.example.kotlin.maps.services.MusicService.LocalBinder
import android.os.IBinder
import android.content.ServiceConnection
import android.net.Uri
import org.example.kotlin.maps.services.MusicService
import org.example.kotlin.maps.R
import org.example.kotlin.maps.model.Station
import org.example.kotlin.maps.views.radio.StationInteractionListener
import org.example.kotlin.maps.views.radio.detail.StationDetailActivity


class RadioActivity : AppCompatActivity(), StationInteractionListener {
    private enum class Filter{
        POPULAR, BY_CATEGORY, BY_COUNTRY
    }

    var mBound = false
    var mService: MusicService? = null
    private var myFilter: Filter = Filter.POPULAR
    private lateinit var database:Database

    /** Defines callbacks for service binding, passed to bindService()  */
    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to MusicService, cast the IBinder and get MusicService instance
            val binder = service as LocalBinder
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

    class FetchCategories(val context: Context): AsyncTask<Boolean, Void, List<Category>>(){
        private val apiKey: String = context.resources.getString(R.string.dirble_api_key)
        private val database: Database = DatabasePreferences.instance(context)
        private val gson: Gson = Gson()

        override fun doInBackground(vararg params: Boolean?): List<Category> {
            var categories: List<Category> = ArrayList()
            if(params[0] == true) {
                categories = database.loadCategories().toList()
            }
            if(categories.isEmpty()){
                val result = URL("http://api.dirble.com/v2/categories?token="+apiKey).readText()
                categories = gson.fromJson(result, Array<Category>::class.java).toList();
                database.store(categories)
            }
            return categories
        }

        override fun onPostExecute(result: List<Category>?) {
            super.onPostExecute(result)
            if(result != null){

            }
        }
    }

    class FetchCountries(val context: Context): AsyncTask<Boolean, Void, List<Country>>(){
        private val apiKey: String = context.resources.getString(R.string.dirble_api_key)
        private val database: Database = DatabasePreferences.instance(context)
        private val gson: Gson = Gson()

        override fun doInBackground(vararg params: Boolean?): List<Country> {
            var country: List<Country> = ArrayList()
            if(params[0] == true) {
                country = database.loadCountries().toList()
            }
            if(country.isEmpty()){
                val result = URL("http://api.dirble.com/v2/countries?token="+apiKey).readText()
                country = gson.fromJson(result, Array<Country>::class.java).toList()
                database.store(country)
            }
            return country
        }

        override fun onPostExecute(result: List<Country>?) {
            super.onPostExecute(result)
            if(result != null){

            }
        }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                filter.visibility = View.GONE
                myFilter = Filter.POPULAR
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                myFilter = Filter.BY_CATEGORY
                // Convierte las categorias a la lista de String que se esta seleccionado
                val categories:List<String> = database.loadCategories().map { category: Category -> category.title }
                // Create an ArrayAdapter
                val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories)
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                filter.adapter = adapter
                filter.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                myFilter = Filter.BY_COUNTRY
                // Convierte las categorias a la lista de String que se esta seleccionado
                val countries:List<String> = database.loadCountries().map { country: Country -> country.name}
                // Create an ArrayAdapter
                val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, countries)
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                filter.adapter = adapter
                filter.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radio)
        database = DatabasePreferences.instance(applicationContext)

        filter.visibility = View.GONE
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.list_fragment, StationFragment.newInstance(null))
                .commit()

        FetchCategories(this).execute(true)
        FetchCountries(this).execute(true)

        filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.list_fragment, StationFragment.newInstance(null))
                        .commit()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(myFilter){
                    Filter.POPULAR -> {
                        supportFragmentManager
                                .beginTransaction()
                                .replace(R.id.list_fragment, StationFragment.newInstance(null))
                                .commit()
                    }
                    Filter.BY_COUNTRY -> {
                        supportFragmentManager
                                .beginTransaction()
                                .replace(R.id.list_fragment, StationFragment.newInstance(database.loadCountries().elementAt(position)))
                                .commit()
                    }
                    Filter.BY_CATEGORY -> {
                        supportFragmentManager
                                .beginTransaction()
                                .replace(R.id.list_fragment, StationFragment.newInstance(database.loadCategories().elementAt(position)))
                                .commit()
                    }
                }
            }
        }
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
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

    override fun play(station: Station, callback: (valid: Boolean) -> Unit) {
        if(mBound){
            mService?.play(station, callback)
        }else{
            callback(false)
        }
    }

    override fun pause() {
        if(mBound){
            mService?.pause()
        }
    }

    override fun share(station: Station) {
        val sendIntent = Intent(Intent.ACTION_VIEW)
        sendIntent.data = Uri.parse("sms:")
        sendIntent.putExtra("sms_body", "StationId "+station.id)
//                    sendIntent.type = "vnd.android-dir/mms-sms"
        startActivity(sendIntent)
    }

    override fun goDetails(station: Station) {
        val intent = Intent(this, StationDetailActivity::class.java)
        intent.putExtra("stationId", station.id)
        startActivity(intent)
    }
}
