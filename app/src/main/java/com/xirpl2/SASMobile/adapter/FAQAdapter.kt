package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.FAQItem

class FAQAdapter(private val faqList: List<FAQItem>) : RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    class FAQViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val tvAnswer: TextView = view.findViewById(R.id.tvAnswer)
        val ivArrow: ImageView = view.findViewById(R.id.ivArrow)
        val layoutQuestion: View = view.findViewById(R.id.layoutQuestion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false)
        return FAQViewHolder(view)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val item = faqList[position]
        holder.tvQuestion.text = item.question
        holder.tvAnswer.text = item.answer

        val isExpanded = expandedPositions.contains(position)

        // Toggle visibility
        holder.tvAnswer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivArrow.rotation = if (isExpanded) 90f else 0f

        holder.layoutQuestion.setOnClickListener {
            if (isExpanded) expandedPositions.remove(position) else expandedPositions.add(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = faqList.size
}
