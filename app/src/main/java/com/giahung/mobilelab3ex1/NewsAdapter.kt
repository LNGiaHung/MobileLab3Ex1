package com.giahung.mobilelab3ex1

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.giahung.mobilelab3ex1.databinding.ItemNewsBinding
import com.google.android.material.card.MaterialCardView
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier

class NewsAdapter(private val context: Context) : PagingDataAdapter<Article, NewsAdapter.NewsViewHolder>(ARTICLE_COMPARATOR) {
    private var classifier: NLClassifier? = null
    private val TAG = "NewsAdapter"

    init {
        try {
            classifier = NLClassifier.createFromFile(context, "text_classification_v2.tflite")
            Log.d(TAG, "NLClassifier initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NLClassifier: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = getItem(position)
        if (article != null) {
            holder.bind(article)
            try {
                if (classifier != null) {
                    Log.d(TAG, "Analyzing sentiment for title: ${article.title}")
                    val sentiment = getSentiment(article.title)
                    Log.d(TAG, "Sentiment result: $sentiment")
                    holder.setCardColor(sentiment)
                } else {
                    Log.e(TAG, "Classifier is null, using neutral sentiment")
                    holder.setCardColor("neutral")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing sentiment: ${e.message}")
                e.printStackTrace()
                holder.setCardColor("neutral")
            }
        }
    }

    private fun getSentiment(title: String): String {
        try {
            Log.d(TAG, "Starting sentiment analysis for: $title")
            val results = classifier?.classify(title) ?: return "neutral"
            var positiveScore = 0.0f
            var negativeScore = 0.0f

            for (category in results) {
                Log.d(TAG, "Category: ${category.label}, Score: ${category.score}")
                when (category.label.toLowerCase()) {
                    "positive" -> positiveScore = category.score
                    "negative" -> negativeScore = category.score
                }
            }

            val sentiment = when {
                positiveScore > 0.6 -> "positive"
                negativeScore > 0.6 -> "negative"
                else -> "neutral"
            }
            Log.d(TAG, "Final sentiment: $sentiment (positive: $positiveScore, negative: $negativeScore)")
            return sentiment
        } catch (e: Exception) {
            Log.e(TAG, "Error in getSentiment: ${e.message}")
            e.printStackTrace()
            return "neutral"
        }
    }

    override fun onViewRecycled(holder: NewsViewHolder) {
        super.onViewRecycled(holder)
        holder.setCardColor("neutral")
    }

    class NewsViewHolder(private val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        private val cardView: MaterialCardView = binding.root as MaterialCardView

        fun bind(article: Article) {
            binding.tvTitle.text = article.title
            binding.tvContent.text = article.content ?: "No content available"
            binding.tvPublishedAt.text = article.publishedAt
            binding.ivImage.load(article.urlToImage) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        }

        fun setCardColor(sentiment: String) {
            val color = when (sentiment) {
                "positive" -> Color.argb(50, 76, 175, 80) // Light green with transparency
                "negative" -> Color.argb(50, 244, 67, 54) // Light red with transparency
                "neutral" -> Color.argb(50, 158, 158, 158) // Light gray with transparency
                else -> Color.argb(50, 158, 158, 158)
            }
            cardView.setCardBackgroundColor(color)
        }
    }

    companion object {
        val ARTICLE_COMPARATOR = object : DiffUtil.ItemCallback<Article>() {
            override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean =
                oldItem.title == newItem.title && oldItem.publishedAt == newItem.publishedAt

            override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean =
                oldItem == newItem
        }
    }

    fun close() {
        try {
            classifier?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing classifier: ${e.message}")
        }
    }
}