package com.example.wesafe.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.wesafe.KEY_FRIEND_ID
import com.example.wesafe.R
import com.example.wesafe.TrackFriendsActivity
import com.example.wesafe.dataClasses.User
import kotlinx.android.synthetic.main.family_item_view.view.*

class FamilyAdapter(private val users: ArrayList<User>, private val activeStatus: ArrayList<Boolean>)
    : RecyclerView.Adapter<FriendViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        return FriendViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.family_item_view, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(users[position], activeStatus[position])
    }

    override fun getItemCount(): Int = users.size
}
class FriendViewHolder(private val view: View): RecyclerView.ViewHolder(view){
    fun bind(user: User, active: Boolean){
        with(view){
            tvName.text = "${adapterPosition+1}. ${user.name}"
            ivActive.isVisible = active

            setOnClickListener {
                if(active){
                    context.startActivity(
                        Intent(
                            context,
                            TrackFriendsActivity::class.java
                        ).putExtra(
                            KEY_FRIEND_ID, user.id
                        )
                    )
                }else{
                    Toast.makeText(context, "${user.name} is not travelling currently", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}