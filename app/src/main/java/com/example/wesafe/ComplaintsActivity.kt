package com.example.wesafe

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_complaints.*

class ComplaintsActivity : AppCompatActivity() {

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    var complaintTypesList = arrayOf(
        "Harassment",
        "Eve-Teasing",
        "Misbehave",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaints)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvEmail.text = firebaseAuth.currentUser!!.email

        complaintTypeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            complaintTypesList
        )

        btnSubmit.setOnClickListener {
            Toast.makeText(this, "Sumbitted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}