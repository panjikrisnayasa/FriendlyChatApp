package com.panjikrisnayasa.friendlychatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
                    val message = Message("", mUsername, downloadUrl)
                    mDatabaseReference.push().setValue(message)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_main_send_chat -> {
                //firebase
                val message = Message(edit_text_main_chat_message.text.toString(), mUsername, "")
                mDatabaseReference.push().setValue(message)

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
            list.add(message)
            showRecyclerView()
        }
    }

    override fun onChildRemoved(p0: DataSnapshot) {}
    //firebase

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
}
