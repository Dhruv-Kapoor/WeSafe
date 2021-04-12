package com.example.wesafe

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.wesafe.adapters.TravelTipAdapter
import com.example.wesafe.customViews.ItemDecoratorDots
import com.example.wesafe.dataClasses.Driver
import com.example.wesafe.dataClasses.TravelTip
import com.example.wesafe.services.TravelService
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_travel.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RC_CAPTURE = 100
private const val TAG = "TravelActivity"

class TravelActivity : AppCompatActivity(), OnMapReadyCallback, TravelService.LocationListener {
    private var mMap: GoogleMap? = null

    private val travelTips = arrayListOf(
        TravelTip("Always Carry pepper spray", R.drawable.traveltip1),
        TravelTip("Learn some self defense moves", R.drawable.traveltip2),
        TravelTip("Take a pic of the cab number plate", R.drawable.traveltip3),
        TravelTip("Don't doze off in a cab", R.drawable.traveltip4),
        TravelTip("Try to Be on phone with a friend or family member", R.drawable.traveltip5),
        TravelTip("Always Keep a Swiss knife", R.drawable.traveltip6),
    )
    private val driverSheetBehavior by lazy {
        BottomSheetBehavior.from(driverSheet)
    }
    private val vehicleNoSheetBehavior by lazy {
        BottomSheetBehavior.from(vehicleNumberSheet)
    }
    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var viewModel: TravelActivityViewModel? = null

    private var mService: TravelService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is TravelService.MyBinder) {
                mService = service.getService()
                mService?.setLocationListener(this@TravelActivity)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            hideDriverSheet()
            showVehicleNoSheet()
            Log.d(TAG, "onServiceDisconnected: ")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvTravelTips.adapter = TravelTipAdapter(travelTips)
        PagerSnapHelper().attachToRecyclerView(rvTravelTips)
        rvTravelTips.addItemDecoration(
            ItemDecoratorDots(
                10,
                20,
                -10,
                getColor(R.color.gray),
                getColor(R.color.green)
            )
        )

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        )[TravelActivityViewModel::class.java]

        tvStartButton.setOnClickListener {
            startTravelService()
        }

        var firstExpand = true
        driverSheetBehavior.isHideable = true
        driverSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        driverSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        if (firstExpand) {
                            firstExpand = false
                            startTipsAnimation()
                        }
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

        })

        scan.setOnClickListener {
            startActivityForResult(
                Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                RC_CAPTURE
            )
        }


        viewModel?.getPathCoordinated()?.observe(this, {
            val startCoordinate = it.firstOrNull()
            mMap?.clear()
            if (startCoordinate != null) {
                val markerOptions = MarkerOptions()
                    .position(startCoordinate)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_green))
                    .title("Start Location")
                mMap?.addMarker(markerOptions)
            }
            val polylineOptions = PolylineOptions().addAll(it).color(getColor(R.color.green))
            mMap?.addPolyline(polylineOptions)
        })

        fabStop.setOnClickListener {
            mService?.stop()
        }
        if (checkLocationPermission()) {
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
                mMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        13.toFloat()
                    )
                )
            }
        }
    }

    private fun startTravelService() {
        hideVehicleNoSheet()
        val intent = Intent(this, TravelService::class.java)
        startService(intent)
        val vehicleNo = etVehicleNo.text.toString().replace(" ", "").replace("-", "").capitalize()
        firestore.collection("drivers").document(vehicleNo).get()
            .addOnSuccessListener {
                val driver = it.toObject(Driver::class.java)
                if (driver == null) {
                    Toast.makeText(this, "Driver not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                showDriverSheet(driver)
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
            .addOnCompleteListener {
                showDriverSheetBtns()
            }
    }

    private fun scanVehicleNumber(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val textRecogniser = TextRecognition.getClient()
        textRecogniser.process(image).addOnSuccessListener {
            val text = it.text
//            val regex = "^[A-Z|a-z]{2}\\s?[0-9]{1,2}\\s?[A-Z|a-z]{0,3}\\s?[0-9]{4}\$"
//            val regex = "^[A-Z]{2}[ -][0-9]{1,2}(?: [A-Z])?(?: [A-Z]*)? [0-9]{4}\$"
//            val pattern = Pattern.compile(regex)
//            val matcher = pattern.matcher(text)
//            if(matcher.find()){
//                etVehicleNo.setText(matcher.group(1))
//            }else{
//                Toast.makeText(this, "Try Again", Toast.LENGTH_SHORT).show()
//                Log.d(TAG, "scanVehicleNumber: $text")
//            }
            etVehicleNo.setText(text)
            etVehicleNo.requestFocus()

        }.addOnFailureListener {
            Toast.makeText(this, "Scan Failed, Try Again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideVehicleNoSheet() {
        vehicleNoSheetBehavior.isHideable = true
        vehicleNoSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun showVehicleNoSheet() {
        vehicleNoSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        vehicleNoSheetBehavior.isHideable = false
    }

    private fun hideDriverSheet() {
        driverSheetBehavior.isHideable = true
        driverSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        hideDriverSheetBtns()
    }

    private fun showDriverSheet(driver: Driver) {
        driverSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        driverSheetBehavior.isHideable = false

        tvName.text = driver.name
        tvVehicleModel.text = driver.vehicleModel
        tvVehicleNumber.text = driver.vehicleNo
        rating.rating = driver.rating.toFloat()
        if (driver.picUrl != null) {
            Picasso.get().load(driver.picUrl).placeholder(R.drawable.sample_avatar)
                .into(ivProfilePic)
        }
    }

    private fun hideDriverSheetBtns() {
        fabStop.visibility = View.GONE
        fabSos.visibility = View.GONE
    }

    private fun showDriverSheetBtns() {
        fabStop.visibility = View.VISIBLE
        fabSos.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                        val bitmap = data.extras!!["data"] as Bitmap
                        scanVehicleNumber(bitmap)
                    }
                } else {
                    Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        unbindService(serviceConnection)
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
        bindService(Intent(this, TravelService::class.java), serviceConnection, 0)
    }

    override fun onMapReady(map: GoogleMap?) {
        mMap = map
        if (checkLocationPermission()) {
            mMap?.isMyLocationEnabled = true
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

    private fun startTipsAnimation() {
        GlobalScope.launch {
            val lm = rvTravelTips.layoutManager as LinearLayoutManager
            for (i in travelTips.indices) {
                delay(2000)
                var nextPos = lm.findFirstVisibleItemPosition() + 1
                if (nextPos >= travelTips.size) nextPos = 0
                rvTravelTips.smoothScrollToPosition(nextPos)
            }
        }
    }

    override fun onLocationAdded(latLng: LatLng) {
        viewModel?.addPathCoordinate(latLng)
        mMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 13.toFloat())
        )
    }

}