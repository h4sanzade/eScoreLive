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

            teamName.text = team.name
            teamShortName.text = team.shortName ?: team.name.take(3).uppercase()
            leagueName.text = searchResult.leagueName
            seasonYear.text = searchResult.season.toString()

            loadTeamLogo(teamLogo, team.logo)

            val isFavorite = isTeamFavorite(team.id)
            updateFavoriteButton(isFavorite)

            root.setOnClickListener { onTeamClick(searchResult) }

            favoriteButton.setOnClickListener {
                onFavoriteClick(searchResult)
                val newFavoriteState = !isFavorite
                updateFavoriteButton(newFavoriteState)
            }

            standingsButton.setOnClickListener { onStandingsClick(searchResult) }
        }

        private fun loadTeamLogo(imageView: ImageView, logoUrl: String?) {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)

            Glide.with(imageView.context)
                .load(logoUrl)
                .apply(requestOptions)
                .into(imageView)
        }

        private fun updateFavoriteButton(isFavorite: Boolean) = with(binding) {
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
                favoriteButton.setColorFilter(ContextCompat.getColor(root.context, R.color.accent_color))

                favoriteButton.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction {
                        favoriteButton.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
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