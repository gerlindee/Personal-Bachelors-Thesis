package com.example.quizzicat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.location.Criteria
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.Volley
import com.example.quizzicat.Exceptions.AbstractException
import com.example.quizzicat.Exceptions.EmptyFieldsException
import com.example.quizzicat.Exceptions.UnmatchedPasswordsException
import com.example.quizzicat.Model.User
import com.example.quizzicat.Utils.DesignUtils
import com.example.quizzicat.Utils.HttpRequestUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.net.URL
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private var mWebView: WebView? = null

    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseStorage: FirebaseStorage? = null
    private var mFirestoreDatabase: FirebaseFirestore? = null

    private var password: String? = null
    private var repeatedPassword: String? = null
    private var email: String? = null
    private var displayName: String? = null
    private var countryName: String? = null
    private var cityName: String? = null

    private var selectedPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseStorage = FirebaseStorage.getInstance()
        mFirestoreDatabase = Firebase.firestore

        mWebView = WebView(this)

        val termsOfServiceMessage = "I have read and therefore agree with the " + "<u>" + "Terms of Service" + "</u>" + "."
        text_terms_service.text = HtmlCompat.fromHtml(termsOfServiceMessage, HtmlCompat.FROM_HTML_MODE_LEGACY)

        getUserCountryDetails()

        checkbox_terms_service.setOnCheckedChangeListener { _, isChecked ->
            register_button.isEnabled = isChecked
        }

        text_terms_service.setOnClickListener {
            loadTermsAndConditions()
        }

        register_button.setOnClickListener {
            try {
                bindData()
                checkFieldsEmpty()
                checkPasswordsSame()
                registerUserWithEmailPassword()
            } catch (ex: AbstractException) {
                ex.displayMessageWithSnackbar(window.decorView.rootView, this)
            }
        }

        register_avatar_civ.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        register_avatar_remove.setOnClickListener {
            register_avatar_remove.visibility = View.GONE
            selectedPhotoUri = null
            register_avatar_civ.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.default_icon, null))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            register_avatar_remove.visibility = View.VISIBLE
            setSelectedAvatar(data)
        }
    }

    private fun bindData() {
        password = register_password.text.toString()
        repeatedPassword = register_r_password.text.toString()
        email = register_email.text.toString()
        displayName = register_username.text.toString()
    }

    private fun loadTermsAndConditions() {
        setContentView(mWebView)
        mWebView?.loadUrl("https://www.websitepolicies.com/policies/view/FVj4pExJ")
    }

    private fun checkFieldsEmpty() {
        if (password!!.isEmpty() || repeatedPassword!!.isEmpty() || email!!.isEmpty() || displayName!!.isEmpty())
            throw EmptyFieldsException()
    }

    private fun checkPasswordsSame() {
        if (password != repeatedPassword)
            throw UnmatchedPasswordsException()
    }

    private fun registerUserWithEmailPassword() {
        register_progress_bar.visibility = View.VISIBLE

        mFirebaseAuth!!.createUserWithEmailAndPassword(email!!, password!!)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    mFirebaseAuth!!.currentUser?.sendEmailVerification()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                uploadAvatarToFirebaseStorage()
                                register_progress_bar.visibility = View.GONE
                                AlertDialog.Builder(this)
                                    .setTitle("Success")
                                    .setMessage("Account created successfully! To complete the registration process, please verify your e-mail address. Otherwise, you will not be able to access your Quizzicat account! If you do not receive a verification e-mail, please contact the support team.")
                                    .setPositiveButton("Confirm", null)
                                    .show()
                            } else {
                                DesignUtils.showSnackbar(window.decorView.rootView, it.exception?.message.toString(), this)
                            }
                        }
                } else {
                    register_progress_bar.visibility = View.GONE
                    DesignUtils.showSnackbar(window.decorView.rootView, it.exception?.message.toString(), this)
                }
            }
    }

    @SuppressLint("NewApi")
    private fun setSelectedAvatar(data: Intent) {
        selectedPhotoUri = data.data
        val source = ImageDecoder.createSource(this.contentResolver, selectedPhotoUri!!)
        val bitmap = ImageDecoder.decodeBitmap(source)
        register_avatar_civ.setImageBitmap(bitmap)
    }

    private fun uploadAvatarToFirebaseStorage() {
        if (selectedPhotoUri == null) {
            // if no profile picture has been uploaded just set the default picture for the user
            saveUserToFirebaseDatabase("https://firebasestorage.googleapis.com/v0/b/quizzicat-ca219.appspot.com/o/Avatars%2Fdefault_icon.png?alt=media&token=bfb09bfc-df91-4027-8242-5480e6c27e3f")
            return
        }
        val filename = UUID.randomUUID().toString()
        val ref = mFirebaseStorage!!.getReference("/Avatars/$filename")
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener {
                DesignUtils.showSnackbar(window.decorView.rootView, it.message!!, this)
            }
    }

    private fun getUserCountryDetails() {
        val locationDetailsURL = "http://ip-api.com/json"

        // request a json request from the provided url
        val request = JsonObjectRequest(Request.Method.GET, locationDetailsURL, null,
            Response.Listener {
                countryName = it.getString("country")
                cityName = it.getString("city")
            },
            Response.ErrorListener {
                DesignUtils.showSnackbar(window.decorView, it.message.toString(), this)
            }
        )

        HttpRequestUtils.getInstance(this).addToRequestQueue(request)
    }

    private fun saveUserToFirebaseDatabase(profileImageURL: String) {
        val uid = mFirebaseAuth!!.uid ?: ""
        val user = User(uid, displayName!!, profileImageURL, countryName!!, cityName!!)
        mFirestoreDatabase!!.collection("Users").document(uid)
            .set(user)
            .addOnSuccessListener {
                Log.d("SaveUser", "User information successfully saved to the cloud")
            }
            .addOnFailureListener {
                DesignUtils.showSnackbar(window.decorView.rootView, "An internal error occured when saving user information. Please try again!", this)
                Log.d("SaveUser", "User info could not be saved due to error " + it.message.toString())
            }
    }
}
