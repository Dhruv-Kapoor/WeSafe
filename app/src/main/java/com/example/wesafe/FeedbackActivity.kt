package com.example.wesafe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_feedback.*

class FeedbackActivity : AppCompatActivity() {

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    var feedbackTypes = arrayOf(
        "Suggestion",
        "Bug Report",
        "App Crash",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvEmail.text = firebaseAuth.currentUser!!.email

        feedbackTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, feedbackTypes)

        btnSubmit.setOnClickListener {
            Toast.makeText(this, "Sumbitted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}