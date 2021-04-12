package com.example.wesafe

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.wesafe.dataClasses.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity() {

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val firestore by lazy {
        FirebaseFirestore.getInstance().collection("users")
    }
    private var selectedProfileImg: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        btnSignUp.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
        val name = etName.text.toString()
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        val confirmPassword = etPassword.text.toString()
        val phone = etPhone.text.toString()

        if(name.isEmpty()){
            etName.error = "Name cannot be empty"
            return
        }
        if (!validateEmail(email)){
            etEmail.error = "Invalid email"
            return
        }
        if(password.isEmpty()){
            etPassword.error = "Password cannot be empty"
            return
        }
        if(confirmPassword!=password){
            etConfirmPassword.error = "Passwords not same"
            return
        }
        if (!Patterns.PHONE.matcher(phone).matches()){
            etPhone.error = "Invalid phone number"
            return
        }

        btnSignUp.isEnabled = false
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            createUser(it?.user?.uid?:"${System.currentTimeMillis()}", name, email, phone, ivProfilePic.drawable)
            startSplashActivity()
        }.addOnFailureListener {
            if(it is FirebaseAuthUserCollisionException){
                Toast.makeText(this, "Already registered, please login", Toast.LENGTH_SHORT).show()
                return@addOnFailureListener
            }
            Toast.makeText(this, "Sign Up Failed", Toast.LENGTH_SHORT).show()
            btnSignUp.isEnabled = true
            it.printStackTrace()
        }
    }

    private fun createUser(
        id: String,
        name: String,
        email: String,
        phone: String,
        drawable: Drawable
    ) {
        val user = User(
            id,name,null,email, null,phone
        )
        firestore.document(id).set(user)
    }

    private fun validateEmail(email: String):Boolean {
        if(email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) return true
        return false
    }

    private fun startSplashActivity() {
        startActivity(
            Intent(this, SplashActivity::class.java)
        )
    }
}