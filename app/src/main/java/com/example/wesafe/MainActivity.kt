package com.example.wesafe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.wesafe.adapters.ArticleAdapter
import com.example.wesafe.customViews.ItemDecoratorDots
import com.example.wesafe.dataClasses.Article
import com.example.wesafe.dataClasses.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val FRIEND_REQ_NOTI_CHANNEL = "friendreq"
const val RC_ACCEPT_REQUEST = 305

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    private val articles = arrayListOf(
        Article(
            "9 Ways to Stay Safe",
            R.drawable.article1,
            "https://www.nomadicmatt.com/travel-blogs/female-travel-safety/"
        ),
        Article(
            "How to Protect Yourself",
            R.drawable.article2,
            "https://www.india.com/lifestyle/safety-tips-for-women-here-is-how-you-can-protect-yourself-in-every-situation-4178565/"
        ),
        Article(
            "Women's Safety",
            R.drawable.article3,
            "https://www.thehindu.com/opinion/letters/womens-safety/article30170186.ece"
        )
    )
    private var sosPressCount = 0
    private var lastSosPressTime = 0L
    private var sosToast: Toast? = null

    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }
    private var notificationId = 2

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val friendRequestsRef by lazy {
        FirebaseDatabase.getInstance().reference.child("requests")
            .child(firebaseAuth.currentUser!!.uid)
    }

    private val friendRequestListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d(TAG, "onChildAdded: $snapshot")
            val key = snapshot.key
            val request = snapshot.getValue(FriendRequest::class.java)
            if (request!=null) {
                showFriendRequest(request.name)
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

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvArticles.adapter = ArticleAdapter(articles)
        PagerSnapHelper().attachToRecyclerView(rvArticles)
        rvArticles.addItemDecoration(
            ItemDecoratorDots(
                10,
                20,
                -10,
                getColor(R.color.gray),
                getColor(R.color.green)
            )
        )

        setupListeners()
    }

    private fun setupListeners() {
        btnSos.setOnClickListener {
            if (System.currentTimeMillis() > lastSosPressTime + 1000) {
                sosPressCount = 1
            } else {
                sosPressCount++
            }
            lastSosPressTime = System.currentTimeMillis()
            if (sosPressCount >= 3) {
                sendSos()
                sosToast?.cancel()
                sosToast = null
            } else {
                sosToast?.cancel()
                sosToast = Toast.makeText(
                    this,
                    "Press SOS ${3 - sosPressCount} more times to trigger.",
                    Toast.LENGTH_SHORT
                )
                sosToast?.show()
            }
        }

        btnTravel.setOnClickListener {
            startActivity(
                Intent(
                    this, TravelActivity::class.java
                )
            )
        }

        btnDangerZones.setOnClickListener {
            startActivity(
                Intent(
                    this, DangerZonesActivity::class.java
                )
            )
        }

        btnComplaint.setOnClickListener {
            startActivity(
                Intent(
                    this, ComplaintsActivity::class.java
                )
            )
        }

        btnFeedback.setOnClickListener {
            startActivity(
                Intent(
                    this, FeedbackActivity::class.java
                )
            )
        }

        btnFamily.setOnClickListener {
            startActivity(
                Intent(
                    this, FamilyActivity::class.java
                )
            )
        }

        btnProfile.setOnClickListener {
            startActivity(
                Intent(
                    this, ProfileActivity::class.java
                )
            )
        }

        var firstAttach = true
        scrollView.setOnScrollChangeListener { v, _, _, _, _ ->
            val scrollBounds = Rect()
            v.getHitRect(scrollBounds)
            if (rvArticles.getLocalVisibleRect(scrollBounds)) {
                if (firstAttach) {
                    firstAttach = false
                    startArticleAnimation()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        friendRequestsRef.addChildEventListener(friendRequestListener)
    }

    override fun onPause() {
        friendRequestsRef.removeEventListener(friendRequestListener)
        super.onPause()
    }

    private fun showFriendRequest(name: String) {
        val intent = Intent(this, FamilyActivity::class.java)
        val acceptReqPendingIntent = PendingIntent.getActivity(this, RC_ACCEPT_REQUEST, intent, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(FRIEND_REQ_NOTI_CHANNEL, "Friend Requests")
        }

        val notificationBuilder = NotificationCompat.Builder(this, FRIEND_REQ_NOTI_CHANNEL)
            .setContentTitle("Friend Request")
            .setContentText("$name wants to add you as an emergency contact")
            .setSmallIcon(R.drawable.logo_white_32)
            .setContentIntent(acceptReqPendingIntent)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId++, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(chan)
        return channelId
    }

    private fun sendSos() {
        Toast.makeText(this, "Sending SOS", Toast.LENGTH_SHORT).show()
    }

    private fun startArticleAnimation() {
        GlobalScope.launch {
            val lm = rvArticles.layoutManager as LinearLayoutManager
            for (i in articles.indices) {
                delay(2000)
                var nextPos = lm.findFirstVisibleItemPosition() + 1
                if (nextPos >= articles.size) nextPos = 0
                rvArticles.smoothScrollToPosition(nextPos)
            }
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}