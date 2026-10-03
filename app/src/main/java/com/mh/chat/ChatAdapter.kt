package com.mh.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.VH>() {

    class VH(val root: LinearLayout, val text: TextView) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false) as LinearLayout
        return VH(root, root.findViewById(R.id.messageText))
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = messages[position]
        val ctx = holder.itemView.context
        holder.text.text = m.content
        holder.text.alpha = if (m.thinking) 0.6f else 1.0f

        val lp = holder.text.layoutParams as LinearLayout.LayoutParams
        when (m.role) {
            "user" -> {
                lp.gravity = Gravity.END
                holder.text.background =
                    ContextCompat.getDrawable(ctx, R.drawable.message_bubble_user)
                holder.text.setTextColor(0xFFFFFFFF.toInt())
            }
            "system" -> {
                lp.gravity = Gravity.CENTER
                holder.text.background = null
                holder.text.setTextColor(0xFF9E9E9E.toInt())
            }
            else -> { // assistant
                lp.gravity = Gravity.START
                holder.text.background =
                    ContextCompat.getDrawable(ctx, R.drawable.message_bubble_assistant)
                holder.text.setTextColor(0xFFFFFFFF.toInt())
            }
        }
        holder.text.layoutParams = lp
    }
}
