package com.example.e_commerce.UI.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.ImageViewCompat.setImageTintList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.e_commerce.Model.BrandModel
import com.example.e_commerce.R
import com.example.e_commerce.UI.Fragments.ExplorerFragment
import com.example.e_commerce.databinding.ViewholderBrandBinding



@Suppress("DEPRECATION")
class BrandAdapter (val items: MutableList<BrandModel>, val onBrandSelected: (Int) -> Unit, val onCenterRequested: (Int) -> Unit
): RecyclerView.Adapter<BrandAdapter.ViewHolder>() {
        private var selectedPosition=-1
    private var lastSelectedPosition=-1
    private lateinit var context : Context

    @SuppressLint("NotifyDataSetChanged")

    class ViewHolder (val binding : ViewholderBrandBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandAdapter.ViewHolder {
        context= parent.context
        val binding=ViewholderBrandBinding.inflate(LayoutInflater.from(context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item=items[position]
        holder.binding.titlePic.text=item.name

        Glide.with(holder.itemView.context)
            .load(item.picUrl)
            .into(holder.binding.pic)

        holder.binding.root.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            val currentPosition = position
            var idToSend: Int = -1

            if (selectedPosition == currentPosition) {
                selectedPosition = -1
                idToSend = -1
            } else {

                selectedPosition = currentPosition

                idToSend = item.id
            }

            notifyItemChanged(currentPosition)
            if (previousSelectedPosition != -1 && previousSelectedPosition != selectedPosition) {
                notifyItemChanged(previousSelectedPosition)
            }

            onBrandSelected(idToSend)

            if (idToSend != -1) {
                onCenterRequested(currentPosition)
            }
        }


        holder.binding.titlePic.setTextColor(context.resources.getColor(android.R.color.white))
        if (selectedPosition==position){
            holder.binding.pic.setBackgroundResource(0)
            holder.binding.mainLayout.setBackgroundResource(R.drawable.background_bg)
            ImageViewCompat.setImageTintList(holder.binding.pic, ColorStateList.valueOf(context.getColor(R.color.white))
            )

            holder.binding.titlePic.visibility = View.VISIBLE
        } else {
            holder.binding.pic.setBackgroundResource(R.drawable.grey_bg)
            holder.binding.mainLayout.setBackgroundResource(0)
            ImageViewCompat.setImageTintList(
                holder.binding.pic,
                ColorStateList.valueOf(context.getColor(R.color.black)))

            holder.binding.titlePic.visibility=View.GONE
        }

    }

    override fun getItemCount(): Int = items.size
    }