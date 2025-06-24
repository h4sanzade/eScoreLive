package com.materialdesign.escorelive.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemLiveMatchBinding
import java.text.SimpleDateFormat
import java.util.*

class LiveMatchAdapter(
    private val onMatchClick: (LiveMatch) -> Unit
) : ListAdapter<LiveMatch, LiveMatchAdapter.LiveMatchViewHolder>(LiveMatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveMatchViewHolder {
        val binding = ItemLiveMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LiveMatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LiveMatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LiveMatchViewHolder(private val binding: ItemLiveMatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: LiveMatch) = with(binding) {
            // League info
            leagueName.text = match.league.name
            loadImage(leagueLogo, match.league.logo)

            // Team names
            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name

            // Team logos
            loadImage(homeTeamLogo, match.homeTeam.logo)
            loadImage(awayTeamLogo, match.awayTeam.logo)

            // Match minute and status indicator
            matchMinute.text = match.matchMinute

            when {
                match.isLive -> {
                    liveIndicator.visibility = android.view.View.VISIBLE
                    matchMinute.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    homeScore.visibility = android.view.View.VISIBLE
                    awayScore.visibility = android.view.View.VISIBLE
                    homeScore.text = match.homeScore.toString()
                    awayScore.text = match.awayScore.toString()
                    detailsBtn.text = "Live Details"
                    detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.holo_red_dark))
                }
                match.isFinished -> {
                    liveIndicator.visibility = android.view.View.GONE
                    matchMinute.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                    homeScore.visibility = android.view.View.VISIBLE
                    awayScore.visibility = android.view.View.VISIBLE
                    // Show final scores for finished matches from API
                    homeScore.text = match.homeScore.toString()
                    awayScore.text = match.awayScore.toString()
                    detailsBtn.text = "Match Report"
                    detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
                match.isUpcoming -> {
                    liveIndicator.visibility = android.view.View.GONE
                    matchMinute.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    homeScore.visibility = android.view.View.VISIBLE
                    awayScore.visibility = android.view.View.VISIBLE
                    homeScore.text = "-"
                    awayScore.text = "-"
                    detailsBtn.text = "Preview"
                    detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.holo_blue_dark))
                }
                else -> {
                    liveIndicator.visibility = android.view.View.GONE
                    matchMinute.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                    homeScore.visibility = android.view.View.VISIBLE
                    awayScore.visibility = android.view.View.VISIBLE
                    // For any other state, show scores if available from API
                    if (match.homeScore > 0 || match.awayScore > 0) {
                        homeScore.text = match.homeScore.toString()
                        awayScore.text = match.awayScore.toString()
                    } else {
                        homeScore.text = "-"
                        awayScore.text = "-"
                    }
                    detailsBtn.text = "Details"
                    detailsBtn.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
            }

            root.setOnClickListener { onMatchClick(match) }
            detailsBtn.setOnClickListener { onMatchClick(match) }
        }

        private fun loadImage(imageView: android.widget.ImageView, url: String) {
            Glide.with(imageView.context)
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }
    }

    class LiveMatchDiffCallback : DiffUtil.ItemCallback<LiveMatch>() {
        override fun areItemsTheSame(oldItem: LiveMatch, newItem: LiveMatch): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LiveMatch, newItem: LiveMatch): Boolean {
            return oldItem == newItem
        }
    }
}