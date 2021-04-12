package com.example.wesafe

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.example.wesafe.dataClasses.LocationUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_travel.*

const val KEY_FRIEND_ID = "friendId"

class TrackFriendsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null

    private val database by lazy {
        FirebaseDatabase.getInstance().reference.child("LiveLocation")
    }
    private val pathCoordinates = ArrayList<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_friends)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val friendId =
            intent.getStringExtra(KEY_FRIEND_ID) ?: throw RuntimeException("Friend ID not passed")
        database.child(friendId).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if(snapshot.key=="active"){
                    return
                }
                val locationUpdate = snapshot.getValue(LocationUpdate::class.java)
                if (locationUpdate != null) {
                    pathCoordinates.add(LatLng(locationUpdate.latitude, locationUpdate.longitude))
                    refreshPath()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

    }

    private fun refreshPath() {
        val startCoordinate = pathCoordinates.firstOrNull()
        mMap?.clear()
        if (startCoordinate != null) {
            val markerOptions = MarkerOptions()
                .position(startCoordinate)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_green))
                .title("Start Location")
            mMap?.addMarker(markerOptions)
        }
        val polylineOptions = PolylineOptions().addAll(pathCoordinates).color(getColor(R.color.green))
        mMap?.addPolyline(polylineOptions)

        val lastCoordinate = pathCoordinates.lastOrNull()
        if(lastCoordinate!=null){
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(lastCoordinate, 15.toFloat()))
            val markerOptions = MarkerOptions()
                .position(lastCoordinate)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_red))
                .title("Current Location")
            mMap?.addMarker(markerOptions)
        }
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
    }


}