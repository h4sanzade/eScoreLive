package com.materialdesign.escorelive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class LiveMatchAdapter(
    private val onMatchClick: (LiveMatch) -> Unit
) : ListAdapter<LiveMatch, LiveMatchAdapter.LiveMatchViewHolder>(LiveMatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveMatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_live_match, parent, false)
        return LiveMatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: LiveMatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LiveMatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val leagueLogo: ImageView = itemView.findViewById(R.id.league_logo)
        private val leagueName: TextView = itemView.findViewById(R.id.league_name)
        private val matchMinute: TextView = itemView.findViewById(R.id.match_minute)
        private val liveIndicator: View = itemView.findViewById(R.id.live_indicator)
        private val homeTeamLogo: ImageView = itemView.findViewById(R.id.home_team_logo)
        private val homeTeamName: TextView = itemView.findViewById(R.id.home_team_name)
        private val homeScore: TextView = itemView.findViewById(R.id.home_score)
        private val awayTeamLogo: ImageView = itemView.findViewById(R.id.away_team_logo)
        private val awayTeamName: TextView = itemView.findViewById(R.id.away_team_name)
        private val awayScore: TextView = itemView.findViewById(R.id.away_score)
        private val detailsBtn: TextView = itemView.findViewById(R.id.details_btn)

        fun bind(match: LiveMatch) {
            // League info
            leagueName.text = match.league.name
            loadImage(leagueLogo, match.league.logo)

            // Match minute and live indicator
            matchMinute.text = match.matchMinute
            liveIndicator.visibility = if (match.isLive) View.VISIBLE else View.GONE

            // Home team
            homeTeamName.text = match.homeTeam.name
            homeScore.text = match.homeScore.toString()
            loadImage(homeTeamLogo, match.homeTeam.logo)

            // Away team
            awayTeamName.text = match.awayTeam.name
            awayScore.text = match.awayScore.toString()
            loadImage(awayTeamLogo, match.awayTeam.logo)

            itemView.setOnClickListener { onMatchClick(match) }
            detailsBtn.setOnClickListener { onMatchClick(match) }
        }

        private fun loadImage(imageView: ImageView, url: String) {
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