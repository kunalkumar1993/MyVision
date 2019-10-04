package com.kk.myvision

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import android.location.LocationManager
import androidx.core.content.ContextCompat.getSystemService
import android.R.string.cancel
import android.content.DialogInterface

import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import android.location.LocationListener
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
/**
 * Created by KwikPay Android Team Pune on 27-09-2019.
 * Copyright (c) 2019 KwikPay Limited. All rights reserved.
 */
public class OfflineLocation :AppCompatActivity() {
    var locationManager: LocationManager? = null
    var mContext: Context? = null
    var tv_lat_long: TextView? = null

/*
    override fun onLocationChanged(location: Location?) {
        val latitude = location!!.getLatitude()
        val longitude = location.getLongitude()
        val msg = "New Latitude: " + latitude + "New Longitude: " + longitude
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show()//To change body of created functions use File | Settings | File Templates.
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderEnabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            setContentView(R.layout.offline_location)
        mContext = this
          tv_lat_long=findViewById<TextView>(R.id.tv_lat_long)
        /*locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000,
            10, this)*/
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?;
        try {
            // Request location updates
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener);
        } catch(ex: SecurityException) {
            Log.d("OfflineLocation", "Security Exception, no location available");
        }
    }


    override fun onResume() {
        super.onResume()
    }



    private fun isLocationEnabled() {

        if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Enable Location")
            alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.")
            alertDialog.setPositiveButton("Location Settings",
                DialogInterface.OnClickListener { dialog, which ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                })
            alertDialog.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
            val alert = alertDialog.create()
            alert.show()
        } else {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Confirm Location")
            alertDialog.setMessage("Your Location is enabled, please enjoy")
            alertDialog.setNegativeButton("Back to interface",
                DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
            val alert = alertDialog.create()
            alert.show()
        }
    }

}



private val locationListener: LocationListener = object : LocationListener {
    override fun onLocationChanged(location: Location) {
     Log.e("LOCATION","" + location.longitude + ":" + location.latitude);
    }
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
