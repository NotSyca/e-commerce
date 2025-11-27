package com.example.e_commerce.UI.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerce.Model.User
import com.example.e_commerce.databinding.ItemUserBinding

class UserAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        with(holder.binding) {
            tvUserName.text = "${user.firstName} ${user.lastName}"
            tvUserEmail.text = user.email ?: "Sin email"

            if (user.phone.isNullOrEmpty()) {
                tvUserPhone.visibility = View.GONE
            } else {
                tvUserPhone.visibility = View.VISIBLE
                tvUserPhone.text = user.phone
            }

            if (user.isAdmin == true) {
                tvAdminBadge.visibility = View.VISIBLE
            } else {
                tvAdminBadge.visibility = View.GONE
            }

            cardUser.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
