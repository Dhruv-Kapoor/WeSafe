package com.example.wesafe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

private const val RC_LOCATION_PERMISSION = 300
private const val TAG = "SplashActivity"
class SplashActivity : AppCompatActivity() {

    private val permissions = arrayListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).also {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            it.add(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(checkPermissions()){
            startMainActivity()
        }else{
            requestPermissions(this, permissions.toArray(arrayOf<String>()), RC_LOCATION_PERMISSION)
        }
    }

    private fun startLoginActivity() {
        startActivity(
            Intent(this, LoginActivity::class.java)
        )
    }

    private fun checkPermissions(): Boolean {

        permissions.forEach {
            if(checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    private fun startMainActivity(){
        val currentUser = firebaseAuth.currentUser
        if(currentUser!=null) {
            Log.d(TAG, "onCreate: ${currentUser.email}")
            startActivity(
                Intent(this, MainActivity::class.java)
            )
        }else{
            startLoginActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == RC_LOCATION_PERMISSION){
            grantResults.forEach {
                Log.d(TAG, "onRequestPermissionsResult: $it")
            }
            var allPermissionsGranted = true
            grantResults.forEach {
                if(it != PackageManager.PERMISSION_GRANTED){
                    allPermissionsGranted=false
                }
            }
            if(allPermissionsGranted){
                startMainActivity()
            }else{
                Toast.makeText(this, "Permissions Required", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}