package com.panjikrisnayasa.friendlychatapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MessageAdapter(private val listMessage: ArrayList<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MessageHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_recycler_view_main_chat, p0, false)
        return MessageHolder(view)
    }

    override fun getItemCount(): Int {
        return listMessage.size
    }

    override fun onBindViewHolder(p0: MessageHolder, p1: Int) {
        val message = listMessage[p1]

        p0.mTextSender.text = message.sender
        if (message.image != "") {
            p0.mTextMessage.visibility = View.GONE
            Glide.with(p0.mImage.context)
                .load(message.image)
                .into(p0.mImage)
        } else {
            p0.mImage.visibility = View.GONE
            p0.mTextMessage.text = message.message
        }

        p0.mImage.setOnClickListener {

        }
        p0.mImage.clipToOutline = true
    }

    inner class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mTextSender: TextView = itemView.findViewById(R.id.text_item_recycler_view_main_chat_sender)
        var mTextMessage: TextView = itemView.findViewById(R.id.text_item_recycler_view_main_chat_message)
        var mImage: ImageView = itemView.findViewById(R.id.image_item_recycler_view_main_chat_image)
    }
}