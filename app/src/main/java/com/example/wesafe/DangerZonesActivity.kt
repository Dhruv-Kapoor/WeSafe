package com.example.wesafe

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wesafe.dataClasses.DangerZone
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_danger_zones.*
import kotlinx.android.synthetic.main.activity_travel.mapView
import kotlinx.android.synthetic.main.activity_travel.toolbar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "DangerZonesActivity"

class DangerZonesActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnCameraMoveListener {

    private var mMap: GoogleMap? = null
    private val firestore by lazy {
        FirebaseFirestore.getInstance().collection("dangerZones")
    }
    private val locationService by lazy {
        getSystemService(LocationManager::class.java)
    }
    private val dangerZonesList = ArrayList<DangerZone>()

    private var currentCameraLatLng = LatLng(0.0, 0.0)
    private var currentPosLatLng = LatLng(0.0, 0.0)

    private val locationManager by lazy {
        getSystemService(LocationManager::class.java)
    }
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private lateinit var mLocationRequest: LocationRequest
    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()

                val latLng = LatLng(location.latitude, location.longitude)

                currentPosLatLng = latLng
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_danger_zones)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (checkLocationPermission()) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showAlertMessageNoGps()
            }
            moveToCurrentLocation()
        }

        fabMarkSafe.setOnClickListener {
            markCurrentLocationSafe()
        }
        fabMarkUnsafe.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Confirm Unsafe")
                .setMessage("Are you sure this area is unsafe?")
                .setPositiveButton("Yes") { dialog, _ ->
                    markCurrentLocationUnsafe()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }

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
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
            if (it != null) {
                val latLng = LatLng(it.latitude, it.longitude)
                currentCameraLatLng = latLng
                mMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        latLng,
                        13.toFloat()
                    )
                )
                refreshDangerZones()
            } else {
                moveToCurrentLocation()
            }
        }
    }

    private fun showAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Please turn on your GPS to use this feature.")
            .setCancelable(false)
            .setPositiveButton("Go to settings",
                DialogInterface.OnClickListener { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) })

        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun markCurrentLocationUnsafe() {
        val t = currentPosLatLng
        val zoneId = getZoneForLatLng(t)
        val localRef = firestore.document(t.latitude.toInt().toString())
            .collection(t.longitude.toInt().toString())
        if (zoneId != null) {
            localRef.document(zoneId).get().addOnSuccessListener {
                val zone = it.toObject(DangerZone::class.java) ?: return@addOnSuccessListener
                zone.unsafeCount += 1
                localRef.document(zoneId).set(zone).addOnSuccessListener {
                    refreshDangerZones()
                }
            }
        } else {
            localRef.document().get().addOnSuccessListener {
                val key = it.id
                val dangerZone = DangerZone(
                    key, 0, 1, t.latitude, t.longitude, 1000
                )
                localRef.document(key).set(dangerZone).addOnSuccessListener {
                    refreshDangerZones()
                }

            }
        }
    }

    private fun markCurrentLocationSafe() {
        val t = currentPosLatLng
        val zoneId = getZoneForLatLng(t)
        if (zoneId != null) {
            val localRef = firestore.document(t.latitude.toInt().toString())
                .collection(t.longitude.toInt().toString())
            localRef.document(zoneId).get().addOnSuccessListener {
                val zone = it.toObject(DangerZone::class.java) ?: return@addOnSuccessListener
                zone.safeCount += 1
                localRef.document(zoneId).set(zone).addOnSuccessListener {
                    refreshDangerZones()
                    Toast.makeText(this, "Marked safe", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "This area is already safe", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getZoneForLatLng(latLng: LatLng): String? {
        val x1 = latLng.latitude
        val y1 = latLng.longitude
        dangerZonesList.forEach {
            val x2 = it.latitude
            val y2 = it.longitude
            val dist = distance(x1, y1, x2, y2)
            if (dist <= it.radius) {
                return it.id
            }
        }
        return null
    }

    private fun distance(lat_a: Double, lng_a: Double, lat_b: Double, lng_b: Double): Double {
        val earthRadius = 3958.75
        val latDiff = Math.toRadians((lat_b - lat_a))
        val lngDiff = Math.toRadians((lng_b - lng_a))
        val a = sin(latDiff / 2) * sin(latDiff / 2) +
                cos(Math.toRadians(lat_a)) * cos(Math.toRadians(lat_b)) *
                sin(lngDiff / 2) * sin(lngDiff / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = earthRadius * c
        val meterConversion = 1609.0
        return (distance * meterConversion)
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onMapReady(map: GoogleMap?) {
        mMap = map
        if (checkLocationPermission()) {
            mMap?.isMyLocationEnabled = true
            locationService.getBestProvider(Criteria(), false)?.let { it1 ->
                locationService.getLastKnownLocation(it1).let {
                    if (it != null) {
                        mMap?.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude), 13F
                            )
                        )
                    }
                }
            }
            mMap?.setOnCameraMoveListener(this)

        }
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

    override fun onCameraMove() {
        val latitude = mMap?.cameraPosition?.target?.latitude
        val longitude = mMap?.cameraPosition?.target?.longitude
        if (latitude == null || longitude == null) return
        if (latitude.toInt() != currentCameraLatLng.latitude.toInt() || longitude.toInt() != currentCameraLatLng.longitude.toInt()) {
            currentCameraLatLng = LatLng(latitude, longitude)
            refreshDangerZones()
        }

    }

    private fun refreshDangerZones() {
        val t = currentCameraLatLng
        firestore.document(t.latitude.toInt().toString()).collection(t.longitude.toInt().toString())
            .get().addOnSuccessListener { it ->
                dangerZonesList.clear()
                it.documents.forEach { doc ->
                    val dangerZone = doc.toObject(DangerZone::class.java)
                    if (dangerZone != null) {
                        dangerZonesList.add(dangerZone)
                    }
                }
                refreshMap()
            }
    }

    private fun refreshMap() {
        mMap?.clear()
        Log.d(TAG, "refreshDangerZones: $dangerZonesList")
        dangerZonesList.forEach {
            if (it.unsafeCount > it.safeCount) {
                val circleOptions = CircleOptions().apply {
                    center(LatLng(it.latitude, it.longitude))
                    radius(it.radius.toDouble())
                    strokeWidth(0.toFloat())
                    fillColor(getColor(R.color.translucentRed))
                }
                mMap?.addCircle(circleOptions)
            }
        }
    }

}
