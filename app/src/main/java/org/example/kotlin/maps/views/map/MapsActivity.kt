package org.example.kotlin.maps.views.map

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import kotlinx.android.synthetic.main.activity_maps.*
import android.location.LocationManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.example.kotlin.maps.R
import org.example.kotlin.maps.model.Country
import org.example.kotlin.maps.services.CheckLocationService
import org.example.kotlin.maps.views.radio.list.RadioActivity
import java.net.URL
import java.security.PrivateKey
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {
    private val TAG: String = " MapsActivity"
    private val PERMISSIONS_REQUEST_LOCATION = 45
    private val LocationPermissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)

    private val PERMISSIONS_REQUEST_PHONE_STATE = 46
    private val PhoneStatePermissions = arrayOf(android.Manifest.permission.READ_PHONE_STATE)

    private val PERMISSIONS_REQUEST_SMS_READ = 47
    private val SmsReadPermissions = arrayOf(android.Manifest.permission.READ_SMS)

    private val DOS_MINUTOS = (2 * 60 * 1000).toLong()

    private lateinit var mMap: GoogleMap
    private lateinit var telephonyManager: TelephonyManager

    private lateinit var locationManager: LocationManager
    private var myLocation: Location? = null

    //Esto deberia agregarse a nivel de receivers para que funcionase cuando el app no este en ejecucion
    inner class Receiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val state = intent?.extras?.getString(TelephonyManager.EXTRA_STATE, null)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val phone = intent?.extras?.getString(TelephonyManager.EXTRA_INCOMING_NUMBER, null)
                Log.d("BroadcastReceiver", "state=$state phone=$phone intent=$intent")
                val location = myLocation
                if(phone != null && location != null){
                    val preferences = context?.getSharedPreferences("MAP_MARKERS", Context.MODE_PRIVATE)
                    val txt = "{latitude:" +location.latitude+ " ,longitude:" +location.longitude+ ", text:"+phone+"}"
                    preferences?.edit()?.putString(Date().time.toString(), txt)?.apply()
                    putMarket(location.latitude, location.longitude, phone)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        startService(Intent(applicationContext, CheckLocationService::class.java))
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        loading.visibility = View.VISIBLE

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        startPhoneCallTracking()

        fab_to_radio.setOnClickListener { _ ->
            val intent = Intent(this, RadioActivity::class.java)
            startActivity(intent)
        }

        //Registramos un receptop de anuncios
        val filter = IntentFilter("android.intent.action.PHONE_STATE")
        filter.addCategory(Intent.CATEGORY_DEFAULT)

        val preferences = getSharedPreferences("MAP_MARKERS", Context.MODE_PRIVATE)
        preferences.all.values
                .map { it as String }
                .map { JsonParser().parse(it).asJsonObject }
                .forEach { putMarket(it.get("latitude").asDouble, it.get("longitude").asDouble, it.get("text").asString) }

        registerReceiver(Receiver(), filter)
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.isMyLocationEnabled = true
                // Siempre usa el pasivo
                if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,1000, 0.0f, this)
                }

                // GPS
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20 * 1000, 5.0f, this)
                    updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
                }

                // Network
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000, 10.0f, this)
                    updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
                }
            }catch (_:UninitializedPropertyAccessException){

            }
        }
    }

    override fun onPause() {
        stopLocationRequest()
        super.onPause()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        loading.visibility = View.GONE
        mMap = googleMap
        startLocationRequest()
    }

    private fun putMarket(lat: Double, lng: Double, lbl: String = "ME") {
        // Add a marker in Sydney and move the camera
        val pos = LatLng(lat, lng)
        mMap.addMarker(MarkerOptions().position(pos).title(lbl))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos))
    }

    private fun startLocationRequest() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions(this)
        } else {
            mMap.isMyLocationEnabled = true

            // Siempre usa el pasivo
            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,1000, 0.0f, this)
            }

            // Si hay alguno mas utiliza ese
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                showEnableLocationProvider(this)
            }else{
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20 * 1000, 5.0f, this)
                    updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000, 10.0f, this)
                    updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
                }
            }
        }
    }

    private fun showEnableLocationProvider(activity: Activity) {
        AlertDialog.Builder(activity)
                .setTitle(R.string.enable_location_provider_title)
                .setMessage(R.string.enable_location_provider_description)
                .setPositiveButton(R.string.configure, { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                })
                .setNegativeButton(R.string.cancel, { _, _ ->
                    requestLocationByIP()
                })
                .show()
    }

    override fun onLocationChanged(location: Location?) {
        updateLocation(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onProviderDisabled(provider: String?) {

    }

    private fun updateLocation(location: Location?) {
        if (location != null &&
                (myLocation == null ||
                        location.accuracy < 2 * myLocation!!.accuracy ||
                        location.time - myLocation!!.time > DOS_MINUTOS)) {
            myLocation = location

            val position = LatLng(myLocation!!.latitude, myLocation!!.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom( position, 15.0f))
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopLocationRequest() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(this);
                mMap.isMyLocationEnabled = false
            }
        } catch (_e: UninitializedPropertyAccessException) {

        }
    }

    private fun startPhoneCallTracking() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPhoneStatePermissions(this)
        } else {
            /*
            // Esto es mas facil que agregar el receptor de anuncions por codigo, pero mejor
            // lo dejo como lo piden en la tarea
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, incomingNumber: String) {
                    when (state) {
                        TelephonyManager.CALL_STATE_IDLE -> startLocationRequest()
                        TelephonyManager.CALL_STATE_OFFHOOK -> stopLocationRequest()
                        TelephonyManager.CALL_STATE_RINGING -> putMarket(-34.0, 151.0, incomingNumber)
                        else -> Log.e(TAG, "" + state + " " + incomingNumber)
                    }
                }
            }
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            */
        }
    }

    private fun requestLocationPermissions(activity: Activity) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.location_request_title)
                    .setMessage(R.string.location_request_message)
                    .setPositiveButton("Ok", { _, _ -> ActivityCompat.requestPermissions(activity, LocationPermissions, PERMISSIONS_REQUEST_LOCATION) })
                    .show()
        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity, LocationPermissions, PERMISSIONS_REQUEST_LOCATION)
        }
    }

    private fun requestPhoneStatePermissions(activity: Activity) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_PHONE_STATE)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.phone_state_request_title)
                    .setMessage(R.string.phone_state_request_message)
                    .setPositiveButton("Ok", { _, _ -> ActivityCompat.requestPermissions(activity, PhoneStatePermissions, PERMISSIONS_REQUEST_PHONE_STATE) })
                    .show()
        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity, PhoneStatePermissions, PERMISSIONS_REQUEST_PHONE_STATE)
        }
    }

    private fun requestReadSMSPermissions(activity: Activity) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.RECEIVE_SMS)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.receive_sms_request_title)
                    .setMessage(R.string.receive_sms_request_message)
                    .setPositiveButton("Ok", { _, _ -> ActivityCompat.requestPermissions(activity, SmsReadPermissions, PERMISSIONS_REQUEST_SMS_READ) })
                    .show()
        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity, SmsReadPermissions, PERMISSIONS_REQUEST_SMS_READ)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationRequest()
                } else {
                    showPermissionError(requestCode)
                    requestLocationByIP()
                }
            }
            PERMISSIONS_REQUEST_PHONE_STATE -> {
                if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startPhoneCallTracking()
                } else {
                    showPermissionError(requestCode)
                }
            }
            PERMISSIONS_REQUEST_SMS_READ -> {
                if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    showPermissionError(requestCode)
                }
            }
        }
    }

    private fun requestLocationByIP() {
        Thread(Runnable {
            val locationIP = "https://freegeoip.net/json/"
            val jsonSt = URL(locationIP).readText()
            val jsonOb = JsonParser().parse(jsonSt).asJsonObject
            val position = LatLng(jsonOb.get("latitude").asDouble, jsonOb.get("longitude").asDouble)
            runOnUiThread {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom( position, 10.0f))
            }
        }).start()
    }

    private fun showPermissionError(requestCode: Int) {
        val message: Int = when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> R.string.no_location_permission
            PERMISSIONS_REQUEST_PHONE_STATE -> R.string.no_phone_state_permission
            PERMISSIONS_REQUEST_SMS_READ -> R.string.no_sms_read_permission
            else -> R.string.default_error
        }
        Snackbar.make(activity_wrapper, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showIncomingCall(number: String) {
        val message = resources.getString(R.string.incommingCall, number)
        Snackbar.make(activity_wrapper, message, Snackbar.LENGTH_INDEFINITE).setAction("OK", { _ -> })
    }
}