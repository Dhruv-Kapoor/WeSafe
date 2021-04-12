package com.example.wesafe.services

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.wesafe.R
import com.example.wesafe.TravelActivity
import com.example.wesafe.dataClasses.LocationUpdate
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TravelService : Service() {

    companion object {
        const val ACTION_STOP_TRAVEL_SERVICE = "stopTravelService"
        private const val TAG = "TravelService"
    }

    private val mBinder = MyBinder()
    override fun onBind(intent: Intent?): IBinder = mBinder

    private val NOTIFICATION_ID = 1
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val stopServiceReceiver by lazy { StopServiceReceiver(this) }

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val databaseRef by lazy {
        FirebaseDatabase.getInstance().reference.child("LiveLocation").child(firebaseAuth.currentUser!!.uid)
    }
    private var locationListener: LocationListener? = null

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var mLocationRequest: LocationRequest
    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()

                val latLng = LatLng(location.latitude, location.longitude)

                locationListener?.onLocationAdded(latLng)

                databaseRef.push().setValue(
                    LocationUpdate(location.latitude,location.longitude, System.currentTimeMillis())
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, TravelActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("service", "Travel Service")
        }

        val stopServiceIntent =
            PendingIntent.getBroadcast(
                this,
                501,
                Intent(ACTION_STOP_TRAVEL_SERVICE),
                0
            )

        registerReceiver(stopServiceReceiver, IntentFilter().apply {
            addAction(ACTION_STOP_TRAVEL_SERVICE)
        })

        notificationBuilder = NotificationCompat.Builder(this, "service")
            .setContentTitle("Travelling")
            .setContentText("Location service running")
            .setSmallIcon(R.drawable.logo_white_32)
            .setContentIntent(pendingIntent)
            .addAction(0, "STOP", stopServiceIntent)

        val notification = notificationBuilder.build()

        startForeground(NOTIFICATION_ID, notification)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 1000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        if (checkLocationPermission()) {
            mFusedLocationClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.myLooper()!!
            )
            databaseRef.removeValue()
            databaseRef.child("active").setValue(true)
        }
        else stop()
        return START_NOT_STICKY
    }

    fun stop() {
        mFusedLocationClient?.removeLocationUpdates(mLocationCallback)
        removeLocationListener()
        unregisterReceiver(stopServiceReceiver)
        databaseRef.child("active").setValue(false)
        stopSelf()
    }


    private fun checkLocationPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    fun setLocationListener(locationListener: LocationListener){
        this.locationListener = locationListener
    }
    fun removeLocationListener(){
        locationListener = null
    }

    inner class MyBinder : Binder() {
        fun getService() = this@TravelService
    }

    interface LocationListener{
        fun onLocationAdded(latLng: LatLng)
    }
}

class StopServiceReceiver(private val service: TravelService) : BroadcastReceiver() {
    override fun onReceive(p0: Context?, intent: Intent?) {
        if (intent?.action == TravelService.ACTION_STOP_TRAVEL_SERVICE) {
            service.stop()
        }
    }
}