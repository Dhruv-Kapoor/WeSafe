package com.example.wesafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wesafe.R
import com.example.wesafe.dataClasses.TravelTip
import kotlinx.android.synthetic.main.article_item_view.view.iv
import kotlinx.android.synthetic.main.article_item_view.view.tvTitle
import kotlinx.android.synthetic.main.travel_tip_item_view.view.*

class TravelTipAdapter(val list: ArrayList<TravelTip>) :
    RecyclerView.Adapter<TravelTipViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelTipViewHolder {
        return TravelTipViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.travel_tip_item_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TravelTipViewHolder, position: Int) {
        holder.bind(list[position], position + 1)
    }

    override fun getItemCount(): Int = list.size
}

class TravelTipViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(tip: TravelTip, tipNo: Int) {
        with(view) {
            iv.setImageResource(tip.image)
            tvTitle.text = tip.title
            val tipNoText = "Tip #$tipNo"
            tvTipNo.text = tipNoText
        }
    }
}