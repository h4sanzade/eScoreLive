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
import com.materialdesign.escorelive.domain.model.Match
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemLiveMatchBinding

class LiveMatchAdapter(
    private val onMatchClick: (Match) -> Unit
) : ListAdapter<Match, LiveMatchAdapter.LiveMatchViewHolder>(LiveMatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveMatchViewHolder {
        val binding = ItemLiveMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LiveMatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LiveMatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LiveMatchViewHolder(private val binding: ItemLiveMatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: Match) = with(binding) {
            leagueName.text = match.league.name
            loadImage(leagueLogo, match.league.logo)

            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name

            loadImage(homeTeamLogo, match.homeTeam.logo)
            loadImage(awayTeamLogo, match.awayTeam.logo)

            setupMatchDisplay(match)

            root.setOnClickListener { onMatchClick(match) }
            detailsBtn.setOnClickListener { onMatchClick(match) }
        }

        private fun setupMatchDisplay(match: Match) = with(binding) {
            matchMinute.text = match.matchMinute

            when {
                match.isLive -> {
                    setupLiveMatch(match)
                }
                match.isFinished -> {
                    setupFinishedMatch(match)
                }
                match.isUpcoming -> {
                    setupUpcomingMatch(match)
                }
                else -> {
                    setupDefaultMatch(match)
                }
            }
        }

        private fun setupLiveMatch(match: Match) = with(binding) {
            liveIndicator.visibility = android.view.View.VISIBLE
            matchMinute.setTextColor(ContextCompat.getColor(root.context, R.color.white))

            homeScore.visibility = android.view.View.VISIBLE
            awayScore.visibility = android.view.View.VISIBLE
            homeScore.text = match.homeScore.toString()
            awayScore.text = match.awayScore.toString()

            detailsBtn.text = "Live Details"
            detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.holo_red_dark))

            liveIndicator.animate()
                .alpha(0.3f)
                .setDuration(1000)
                .withEndAction {
                    liveIndicator.animate()
                        .alpha(1.0f)
                        .setDuration(1000)
                        .start()
                }
                .start()
        }

        private fun setupFinishedMatch(match: Match) = with(binding) {
            liveIndicator.visibility = android.view.View.GONE
            matchMinute.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

            homeScore.visibility = android.view.View.VISIBLE
            awayScore.visibility = android.view.View.VISIBLE
            homeScore.text = match.homeScore.toString()
            awayScore.text = match.awayScore.toString()

            detailsBtn.text = "Match Report"
            detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

            highlightWinner(match)
        }

        private fun setupUpcomingMatch(match: Match) = with(binding) {
            liveIndicator.visibility = android.view.View.GONE
            matchMinute.setTextColor(ContextCompat.getColor(root.context, R.color.white))

            homeScore.visibility = android.view.View.VISIBLE
            awayScore.visibility = android.view.View.VISIBLE
            homeScore.text = "VS"
            awayScore.text = ""

            detailsBtn.text = "Preview"
            detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.holo_blue_dark))
        }

        private fun setupDefaultMatch(match: Match) = with(binding) {
            liveIndicator.visibility = android.view.View.GONE
            matchMinute.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

            homeScore.visibility = android.view.View.VISIBLE
            awayScore.visibility = android.view.View.VISIBLE

            if (match.homeScore > 0 || match.awayScore > 0) {
                homeScore.text = match.homeScore.toString()
                awayScore.text = match.awayScore.toString()
                highlightWinner(match)
            } else {
                homeScore.text = "-"
                awayScore.text = "-"
            }

            detailsBtn.text = "Details"
            detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
        }

        private fun highlightWinner(match: Match) = with(binding) {
            when {
                match.homeScore > match.awayScore -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
                match.awayScore > match.homeScore -> {
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
                else -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
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

    class LiveMatchDiffCallback : DiffUtil.ItemCallback<Match>() {
        override fun areItemsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem == newItem
        }
    }
}