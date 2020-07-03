package com.example.quizzicat.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quizzicat.Facades.ImageLoadingFacade
import com.example.quizzicat.Facades.UserDataRetrievalFacade
import com.example.quizzicat.Model.ModelEntity
import com.example.quizzicat.Model.MultiPlayerUserJoined
import com.example.quizzicat.Model.User
import com.example.quizzicat.R
import com.example.quizzicat.Utils.ModelCallback
import com.google.firebase.firestore.FirebaseFirestore

class LobbyUsersAdapter(
    private val source: String,
    private val mainContext: Context?,
    private val firebaseFirestore: FirebaseFirestore,
    private val list: List<MultiPlayerUserJoined>): RecyclerView.Adapter<LobbyUsersAdapter.LobbyUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyUserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return LobbyUserViewHolder(inflater, parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: LobbyUserViewHolder, position: Int) {
        val userJoined = list[position]
        holder.bind(source, firebaseFirestore, mainContext!!, userJoined)
    }

    class LobbyUserViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.view_multi_player_lobby_user, parent, false)) {

        var lobby_user_icon: ImageView? = null
        var lobby_user_name: TextView? = null
        var lobby_user_host: ImageView? = null
        var lobby_user_points: TextView? = null

        init {
            lobby_user_icon = itemView.findViewById(R.id.view_user_lobby_icon)
            lobby_user_name = itemView.findViewById(R.id.view_user_lobby_name)
            lobby_user_host = itemView.findViewById(R.id.view_user_lobby_host)
            lobby_user_points = itemView.findViewById(R.id.view_user_lobby_points)
        }

         fun bind(source: String, firebaseFirestore: FirebaseFirestore, mainContext: Context, user: MultiPlayerUserJoined) {
             UserDataRetrievalFacade(firebaseFirestore, user.uid)
                 .getUserDetails(object: ModelCallback {
                     override fun onCallback(value: ModelEntity) {
                         val userDetails = value as User
                         ImageLoadingFacade(mainContext).loadImage(userDetails.avatar_url, lobby_user_icon!!)
                         lobby_user_name!!.text = userDetails.display_name
                         if (source == "LOBBY") {
                             if (user.role == "CREATOR") {
                                 lobby_user_host!!.visibility = View.VISIBLE
                             }
                         } else {
                             lobby_user_points!!.text = user.score.toString()
                             lobby_user_points!!.visibility = View.VISIBLE
                         }

                     }
                 })
         }
    }
}