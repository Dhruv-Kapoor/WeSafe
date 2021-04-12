package com.example.wesafe

import android.app.AlertDialog
import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.wesafe.adapters.FamilyAdapter
import com.example.wesafe.customViews.AddFamilyMemberDialog
import com.example.wesafe.dataClasses.FriendRequest
import com.example.wesafe.dataClasses.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_family.*
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "FamilyActivity"

class FamilyActivity : AppCompatActivity() {

    private val friendsList = ArrayList<User>()
    private val activeStatus = ArrayList<Boolean>()

    private val adapter by lazy {
        FamilyAdapter(friendsList, activeStatus)
    }

    private val firestore by lazy {
        FirebaseFirestore.getInstance().collection("users")
    }
    private val database by lazy {
        FirebaseDatabase.getInstance().reference.child("LiveLocation")
    }
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    private val pendingRequests = LinkedList<FriendRequest>() as Queue<FriendRequest>
    private val friendRequestsRef by lazy {
        FirebaseDatabase.getInstance().reference.child("requests")
            .child(firebaseAuth.currentUser!!.uid)
    }
    private val friendRequestListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d(TAG, "onChildAdded: $snapshot")
            val key = snapshot.key
            val request = snapshot.getValue(FriendRequest::class.java)
            if (request != null) {
                pendingRequests.add(request)
            }
            showFriendRequest()
            key?.let { friendRequestsRef.child(it).removeValue() }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onCancelled(error: DatabaseError) {
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvFamily.adapter = adapter
        rvFamily.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        refreshData()
        fabAdd.setOnClickListener {
            val dialog = AddFamilyMemberDialog()
            dialog.show(supportFragmentManager, "DialogAddFriend")
        }

        notificationManager.cancelAll()
    }

    override fun onResume() {
        super.onResume()
        friendRequestsRef.addChildEventListener(friendRequestListener)
    }

    override fun onPause() {
        friendRequestsRef.removeEventListener(friendRequestListener)
        super.onPause()
    }

    private var isDialogDisplayed = false
    private fun showFriendRequest() {
        if (!isDialogDisplayed && !pendingRequests.isEmpty()) {
            val request = pendingRequests.poll()
            val dialog = AlertDialog.Builder(this)
                .setTitle("Friend Request")
                .setMessage("${request!!.name} wants to add you as your emergency contact. This allows you and your friend to track each other.")
                .setPositiveButton("Accept") { dialog, _ ->
                    acceptFriendRequest(request.id)
                    dialog.dismiss()
                    isDialogDisplayed = false
                    showFriendRequest()
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    isDialogDisplayed = false
                    showFriendRequest()
                }
                .create()

            isDialogDisplayed = true
            dialog.show()
        }
    }

    private fun acceptFriendRequest(friendId: String) {
        //Add me in friends id
        firestore.document(friendId).get().addOnSuccessListener {
            val friend = it.toObject(User::class.java)
            if (friend == null) {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            onFriendAdded(friend)
            if(friend.friends==null){
                friend.friends = ArrayList()
            }
            friend.friends?.add(firebaseAuth.currentUser!!.uid)
            firestore.document(friendId).set(friend)

        }

        //Add Friend in my id
        firestore.document(firebaseAuth.currentUser!!.uid).get().addOnSuccessListener { doc->
            val currentUser = doc.toObject(User::class.java)
            if(currentUser!=null){
                if(currentUser.friends==null){
                    currentUser.friends = ArrayList()
                }
                currentUser.friends?.add(friendId)
                firestore.document(currentUser.id).set(currentUser)
            }
        }
    }


    private fun refreshData() {
        friendsList.clear()
        activeStatus.clear()

        val mId = firebaseAuth.currentUser!!.uid
        Log.d(TAG, "refreshData: $mId")
        firestore.document(mId).get().addOnSuccessListener {
            val currentUser = it.toObject(User::class.java)
            if (currentUser == null) {
                finish()
                return@addOnSuccessListener
            }
            if (currentUser.friends.isNullOrEmpty()) {
                Toast.makeText(
                    this@FamilyActivity,
                    "Add some friends or family members to keep them updated",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }
            firestore.whereIn("id", currentUser.friends!!).get().addOnSuccessListener { doc ->
                val result = doc.toObjects(User::class.java)
                result.forEachIndexed { i, friend ->
                    friendsList.add(friend)
                    activeStatus.add(false)
                    database.child(friend.id).addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            if (snapshot.key == "active") {
                                activeStatus[i] = snapshot.getValue(Boolean::class.java) == true
                                adapter.notifyItemChanged(i)
                            }
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })
                }

                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun onFriendAdded(friend: User) {
        friendsList.add(friend)
        database.child(friend.id).child("active").get().addOnSuccessListener {
            val active = it.getValue(Boolean::class.java)
            if (active == true) {
                activeStatus.add(true)
            } else {
                activeStatus.add(false)
            }
            adapter.notifyDataSetChanged()
        }
    }
}