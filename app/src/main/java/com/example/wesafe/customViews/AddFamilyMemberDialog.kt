package com.example.wesafe.customViews

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.wesafe.R
import com.example.wesafe.dataClasses.FriendRequest
import com.example.wesafe.dataClasses.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.dialog_add_family_member.*

class AddFamilyMemberDialog() :
    BottomSheetDialogFragment() {

    private val firestore by lazy {
        FirebaseFirestore.getInstance().collection("users")
    }
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val database by lazy {
        FirebaseDatabase.getInstance().reference.child("requests")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.dialog_add_family_member, container, false
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btnAdd.setOnClickListener {
            if (validateEmail(etEmail.text.toString())) {
                addFriend(etEmail.text.toString())
            } else {
                etEmail.error = "Invalid email"
            }
        }
    }

    private fun addFriend(email: String) {
        firestore.whereEqualTo("email", email).get().addOnSuccessListener {
            val users = it.toObjects(User::class.java)
            if (users.isEmpty()) {
                Toast.makeText(context, "User does not exist", Toast.LENGTH_SHORT).show()
            } else {
                firestore.document(firebaseAuth.currentUser!!.uid).get().addOnSuccessListener { user->
                    val currentUser = user.toObject(User::class.java) ?: return@addOnSuccessListener
                    val friend = users[0]
                    if(friend.id == firebaseAuth.currentUser!!.uid){
                        Toast.makeText(context, "Really, you want to add yourself as your friend?", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    database.child(friend.id).push().setValue(FriendRequest(currentUser.id, currentUser.name))
                    Toast.makeText(
                        context,
                        "Request sent, ask ${
                            friend.name
                        } to accept",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

//    interface OnFriendAddedCallback {
//        fun onFriendAdded(friend: User)
//    }
}