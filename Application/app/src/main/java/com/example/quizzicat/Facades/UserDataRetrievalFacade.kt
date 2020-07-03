package com.example.quizzicat.Facades

import android.util.Log
import com.example.quizzicat.Model.User
import com.example.quizzicat.Utils.CounterCallBack
import com.example.quizzicat.Utils.ModelCallback
import com.google.firebase.firestore.FirebaseFirestore

class UserDataRetrievalFacade(private val firebaseFirestore: FirebaseFirestore, private val uid: String) {
    fun getUserDetails(callback: ModelCallback) {
        firebaseFirestore.collection("Users")
            .whereEqualTo("uid", uid)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var user: User? = null
                    for (document in task.result!!) {
                        val userUID = document.get("uid") as String
                        val userCity = document.get("city") as String
                        val userCountry = document.get("country") as String
                        val userDisplayName = document.get("display_name") as String
                        val userPicture = document.get("avatar_url") as String
                        user = User(userUID, userDisplayName, userPicture, userCountry, userCity)
                    }
                    callback.onCallback(user!!)
                } else {
                    Log.d("USER_ERROR", task.exception!!.message.toString())
                }
            }
    }

    fun getNumberOfUsers(callback: CounterCallBack) {
        firebaseFirestore.collection("Users")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var numberOfUsers = 0
                    for (document in task.result!!) {
                        numberOfUsers += 1
                    }
                    callback.onCallback(numberOfUsers)
                } else {
                    Log.d("USER_ERROR", task.exception!!.message.toString())
                }
            }
    }
}