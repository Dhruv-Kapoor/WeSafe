package com.example.wesafe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnLogin.setOnClickListener {
            login()
        }

        btnSignUp.setOnClickListener {
            startActivity(
                Intent(
                    this, SignUpActivity::class.java
                )
            )
        }
    }

    private fun login() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()

        if(!validateEmail(email)){
            etEmail.error = "Invalid email"
            return
        }
        if(password.isEmpty()){
            etPassword.error = "Password cannot be empty"
            return
        }

        btnLogin.isEnabled = false
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            startSplashActivity()
        }.addOnFailureListener {
            when (it) {
                is FirebaseAuthInvalidUserException -> {
                    Toast.makeText(this, "Not Registered, Please SignUp", Toast.LENGTH_SHORT).show()
                }
                is FirebaseAuthInvalidCredentialsException -> {
                    Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
            btnLogin.isEnabled = true
            it.printStackTrace()
        }
    }

    private fun startSplashActivity() {
        startActivity(
            Intent(this, SplashActivity::class.java)
        )
    }

    private fun validateEmail(email: String):Boolean {
        if(email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) return true
        return false
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}