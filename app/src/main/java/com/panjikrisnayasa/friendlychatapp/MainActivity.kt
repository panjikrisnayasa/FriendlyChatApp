package com.panjikrisnayasa.friendlychatapp

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, TextWatcher, ChildEventListener {

    companion object {
        const val ANONYMOUS = "Anonymous"
        const val RC_SIGN_IN = 1
        const val RC_PHOTO_PICKER = 2
        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
        const val NOTIFICATION_ID = 0
    }

    private var list: ArrayList<Message> = arrayListOf()
    private lateinit var mMessageAdapter: MessageAdapter
    private lateinit var mUsername: String

    //firebase messages
    private lateinit var mDatabaseReference: DatabaseReference

    //firebase auth
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAuthStateListener: AuthStateListener

    //firebase images storage
    private lateinit var mImagesStorageReference: StorageReference

    //notification
    private lateinit var mNotificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsername = ANONYMOUS

        //firebase auth
        val provider = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        mAuth = FirebaseAuth.getInstance()
        mAuthStateListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                //user logged in
                onSignedInInit(user.displayName)
            } else {
                //user logged out
                onSignOutClean()
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(provider)
                        .build(),
                    RC_SIGN_IN
                )
            }
        }

        //firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("messages")
        mImagesStorageReference = FirebaseStorage.getInstance().getReference("chat_images")

        edit_text_main_chat_message.addTextChangedListener(this)
        button_main_send_chat.setOnClickListener(this)
        image_view_main_insert_image.setOnClickListener(this)

        //to keep recycler view showing bottom item when keyboard opens
        recycler_view_main_chat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                recycler_view_main_chat.postDelayed({
                    recycler_view_main_chat.scrollToPosition(mMessageAdapter.itemCount - 1)
                }, 100)
            }
        }

        createNotificationChannel()
    }

    override fun onResume() {
        super.onResume()
        mAuth.addAuthStateListener(mAuthStateListener)
    }

    override fun onPause() {
        super.onPause()
        mAuth.removeAuthStateListener(mAuthStateListener)
        mDatabaseReference.removeEventListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Sign in SUCCESS", Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Sign in CANCELED", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data
            val photoRef = mImagesStorageReference.child(selectedImageUri?.lastPathSegment!!)
            photoRef.putFile(selectedImageUri).addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener {
                    val downloadUrl = it.toString()

                    val keyPath = mDatabaseReference.push()
                    val message = Message(keyPath.key, "", mUsername, downloadUrl, false)
                    keyPath.setValue(message)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_main_send_chat -> {
                //firebase
                val keyPath = mDatabaseReference.push()
                val message = Message(
                    keyPath.key,
                    edit_text_main_chat_message.text.toString(),
                    mUsername,
                    "",
                    false
                )
                keyPath.setValue(message)

                edit_text_main_chat_message.text.clear()
            }
            R.id.image_view_main_insert_image -> {
                val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
                imageIntent.type = "image/jpeg"
                imageIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                startActivityForResult(Intent.createChooser(imageIntent, "Complete action using"), RC_PHOTO_PICKER)
            }
        }
    }

    //firebase
    override fun onCancelled(p0: DatabaseError) {}

    override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

    override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

    override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        val message = p0.getValue(Message::class.java)
        if (message != null) {
            if (message.read == false) {
                if (message.sender != mUsername) {
                    Log.d("Panji", message.sender.toString())
                    Log.d("Panji", message.message.toString())
                    sendNotification(message.sender, message.message)
                }
                mDatabaseReference.child(message.id.toString()).child("read").setValue(true)
            }
            list.add(message)
            showRecyclerView()
        }
    }

    //firebase
    override fun onChildRemoved(p0: DataSnapshot) {}

    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        button_main_send_chat.isEnabled = s.toString().trim().isNotEmpty()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_main_logout -> {
                FirebaseAuth.getInstance().signOut()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showRecyclerView() {
        mMessageAdapter = MessageAdapter(list)
        recycler_view_main_chat.layoutManager = LinearLayoutManager(this)
        recycler_view_main_chat.adapter = mMessageAdapter
        recycler_view_main_chat.scrollToPosition(mMessageAdapter.itemCount - 1)
        recycler_view_main_chat.recycledViewPool.setMaxRecycledViews(0, 0)
    }

    private fun onSignedInInit(displayName: String?) {
        list.clear()
        if (displayName != null) {
            mUsername = displayName
        }
        mDatabaseReference.addChildEventListener(this)
        showRecyclerView()
    }

    private fun onSignOutClean() {
        mUsername = ANONYMOUS
        mDatabaseReference.removeEventListener(this)
        list.clear()
    }

    private fun sendNotification(sender: String?, message: String?) {
        val notificationBuilder = getNotificationBuilder(sender, message)
        mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(PRIMARY_CHANNEL_ID, "New Message Notification", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.description = "New message"
            mNotificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun getNotificationBuilder(sender: String?, message: String?): NotificationCompat.Builder {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val notificationPendingIntent =
            PendingIntent.getActivity(this, NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
            .setContentTitle(sender)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_mood_24dp)
            .setContentIntent(notificationPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        return notificationBuilder
    }
}
