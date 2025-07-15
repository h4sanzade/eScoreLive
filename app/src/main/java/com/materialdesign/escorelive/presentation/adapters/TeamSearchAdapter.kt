package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemTeamSearchBinding
import com.materialdesign.escorelive.presentation.search.TeamSearchResult

class TeamSearchAdapter(
    private val onTeamClick: (TeamSearchResult) -> Unit,
    private val onFavoriteClick: (TeamSearchResult) -> Unit,
    private val onStandingsClick: (TeamSearchResult) -> Unit,
    private val isTeamFavorite: (Long) -> Boolean = { false }
) : ListAdapter<TeamSearchResult, TeamSearchAdapter.TeamSearchViewHolder>(TeamSearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamSearchViewHolder {
        val binding = ItemTeamSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TeamSearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TeamSearchViewHolder(private val binding: ItemTeamSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(searchResult: TeamSearchResult) = with(binding) {
            val team = searchResult.team

            // Team basic info
            teamName.text = team.name
            teamShortName.text = team.shortName ?: team.name.take(3).uppercase()
            leagueName.text = searchResult.leagueName
            seasonYear.text = searchResult.season.toString()

            // Load team logo
            Glide.with(root.context)
                .load(team.logo)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(teamLogo)

            // Set favorite status
            val isFavorite = isTeamFavorite(team.id)
            updateFavoriteButton(isFavorite)

            // Set click listeners
            root.setOnClickListener { onTeamClick(searchResult) }
            favoriteButton.setOnClickListener {
                onFavoriteClick(searchResult)
                // Update favorite button immediately
                updateFavoriteButton(!isFavorite)
            }
            standingsButton.setOnClickListener { onStandingsClick(searchResult) }
        }

        private fun updateFavoriteButton(isFavorite: Boolean) = with(binding) {
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
                favoriteButton.setColorFilter(ContextCompat.getColor(root.context, R.color.accent_color))
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline)
                favoriteButton.setColorFilter(ContextCompat.getColor(root.context, android.R.color.darker_gray))
            }
        }
    }

    class TeamSearchDiffCallback : DiffUtil.ItemCallback<TeamSearchResult>() {
        override fun areItemsTheSame(oldItem: TeamSearchResult, newItem: TeamSearchResult): Boolean {
            return oldItem.team.id == newItem.team.id && oldItem.leagueId == newItem.leagueId
        }

        override fun areContentsTheSame(oldItem: TeamSearchResult, newItem: TeamSearchResult): Boolean {
            return oldItem == newItem
        }
    }
}