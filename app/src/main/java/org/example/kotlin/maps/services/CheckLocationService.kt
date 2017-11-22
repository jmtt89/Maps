package org.example.kotlin.maps.services

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import org.example.kotlin.maps.NewMessageNotification

class CheckLocationService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private var myLocation: Location? = null
    private val DOS_MINUTOS = (2 * 60 * 1000).toLong()
    private var isInRegion = false

    override fun onCreate() {
        super.onCreate()
        startLocationRequest()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startLocationRequest() {
        locationManager = getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Siempre usa el pasivo
            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,1000, 0.0f, this)
            }

            // Si esta disponible utilizar el GPS
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20 * 1000, 5.0f, this)
                updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
            }

            // Si esta disponible Utilizar la Network
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10 * 1000, 10.0f, this)
                updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
            }
        }
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

            if(location.latitude > 66.55){
                if(!isInRegion){
                    NewMessageNotification.notify(applicationContext)
                    isInRegion = true
                }
            }else{
                isInRegion = false
            }
        }
    }
}
