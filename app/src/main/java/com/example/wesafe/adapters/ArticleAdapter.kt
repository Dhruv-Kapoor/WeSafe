package com.example.wesafe.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wesafe.ArticleViewActivity
import com.example.wesafe.KEY_ARTICLE
import com.example.wesafe.R
import com.example.wesafe.dataClasses.Article
import kotlinx.android.synthetic.main.article_item_view.view.*

class ArticleAdapter(val list: ArrayList<Article>): RecyclerView.Adapter<ArticleViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        return ArticleViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.article_item_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}

class ArticleViewHolder(val view: View): RecyclerView.ViewHolder(view){

    fun bind(article: Article){
        with(view){
            iv.setImageResource(article.image)
            tvTitle.text = article.title

            setOnClickListener {
                it.context.startActivity(
                    Intent(
                        it.context,
                        ArticleViewActivity::class.java
                    ).apply {
                        putExtra(KEY_ARTICLE, article)
                    }
                )
            }
        }
    }
}