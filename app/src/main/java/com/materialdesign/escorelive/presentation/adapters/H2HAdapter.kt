package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemH2hMatchBinding
import com.materialdesign.escorelive.domain.model.Match
import java.text.SimpleDateFormat
import java.util.*

class H2HAdapter : ListAdapter<Match, H2HAdapter.H2HViewHolder>(H2HDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H2HViewHolder {
        val binding = ItemH2hMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return H2HViewHolder(binding)
    }

    override fun onBindViewHolder(holder: H2HViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class H2HViewHolder(private val binding: ItemH2hMatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: Match) = with(binding) {
            displayMatchDate(match)

            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name

            loadImage(homeTeamLogo, match.homeTeam.logo)
            loadImage(awayTeamLogo, match.awayTeam.logo)

            homeScore.text = match.homeScore.toString()
            awayScore.text = match.awayScore.toString()

            leagueName.text = match.league.name

            highlightWinner(match)
        }

        private fun displayMatchDate(match: Match) = with(binding) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            try {
                match.kickoffTime?.let { kickoffTime ->
                    val date = inputFormat.parse(kickoffTime)
                    date?.let {
                        matchDate.text = outputFormat.format(it)
                    }
                }
            } catch (e: Exception) {
                matchDate.text = "Date N/A"
            }
        }

        private fun highlightWinner(match: Match) = with(binding) {
            when {
                match.homeScore > match.awayScore -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

                    homeTeamName.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    awayTeamName.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

                    homeScore.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(200)
                        .withEndAction {
                            homeScore.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                }
                match.awayScore > match.homeScore -> {
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

                    awayTeamName.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    homeTeamName.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

                    awayScore.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(200)
                        .withEndAction {
                            awayScore.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                }
                else -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    homeTeamName.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    awayTeamName.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
            }
        }

        private fun loadImage(imageView: ImageView, url: String?) {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)

            Glide.with(imageView.context)
                .load(url)
                .apply(requestOptions)
                .into(imageView)
        }
    }

    class H2HDiffCallback : DiffUtil.ItemCallback<Match>() {
        override fun areItemsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem == newItem
        }
    }
}