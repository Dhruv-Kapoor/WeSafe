package com.example.wesafe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.wesafe.dataClasses.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val firestore by lazy {
        FirebaseFirestore.getInstance().collection("users")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val mUid = firebaseAuth.currentUser!!.uid

        firestore.document(mUid).get().addOnSuccessListener {
            val user = it.toObject(User::class.java)
            if(user == null){
                Toast.makeText(this, "Error finding user", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            etName.setText(user.name)
            etEmail.setText(user.email)
            etPhone.setText(user.phone)

            Picasso.get()
                .load(user.picUrl)
                .placeholder(R.drawable.sample_avatar)
                .into(ivProfilePic)
        }

        btnSave.setOnClickListener {
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

    }
}