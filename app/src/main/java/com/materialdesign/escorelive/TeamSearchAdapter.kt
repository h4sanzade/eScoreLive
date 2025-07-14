package com.materialdesign.escorelive.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.model.TeamSearchResult
import com.materialdesign.escorelive.databinding.ItemTeamSearchBinding

class TeamSearchAdapter(
    private val onFavoriteClick: (TeamSearchResult) -> Unit
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

        fun bind(team: TeamSearchResult) = with(binding) {
            teamName.text = team.name
            teamCountry.text = team.country

            // Show founded year if available
            if (team.founded != null && team.founded > 0) {
                teamFounded.text = "Founded: ${team.founded}"
                teamFounded.visibility = android.view.View.VISIBLE
            } else {
                teamFounded.visibility = android.view.View.GONE
            }

            // Load team logo
            Glide.with(root.context)
                .load(team.logo)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(teamLogo)

            // Set favorite button state
            if (team.isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_heart_filled)
                favoriteButton.setColorFilter(null)
            } else {
                favoriteButton.setImageResource(R.drawable.ic_heart_outline)
                favoriteButton.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(root.context, android.R.color.darker_gray)
                )
            }

            favoriteButton.setOnClickListener {
                onFavoriteClick(team)
            }
        }
    }

    class TeamSearchDiffCallback : DiffUtil.ItemCallback<TeamSearchResult>() {
        override fun areItemsTheSame(oldItem: TeamSearchResult, newItem: TeamSearchResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TeamSearchResult, newItem: TeamSearchResult): Boolean {
            return oldItem == newItem
        }
    }
}