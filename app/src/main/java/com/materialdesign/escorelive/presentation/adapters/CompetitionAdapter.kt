// CompetitionAdapter.kt - Updated with Standings Support
package com.materialdesign.escorelive.presentation.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionType
import com.materialdesign.escorelive.databinding.ItemCompetitionBinding

class CompetitionAdapter(
    private val onCompetitionClick: (Competition) -> Unit,
    private val onFavoriteClick: (Competition) -> Unit,
    private val onStandingsClick: ((Competition) -> Unit)? = null
) : ListAdapter<Competition, CompetitionAdapter.CompetitionViewHolder>(CompetitionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompetitionViewHolder {
        val binding = ItemCompetitionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompetitionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompetitionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompetitionViewHolder(
        private val binding: ItemCompetitionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(competition: Competition) = with(binding) {
            competitionName.text = competition.name
            regionName.text = competition.country

            // Load competition logo (prioritize league logo over country flag)
            loadCompetitionImage(flagImage, competition.logoUrl, competition.flagUrl)

            setupCompetitionType(competition)
            updateFavoriteButton(competition.isFavorite)
            setupCompetitionStats(competition)
            setupStandingsButton(competition)

            // Add current season indicator
            if (competition.currentSeason) {
                addCurrentSeasonIndicator()
            }

            // Click listeners
            root.setOnClickListener {
                onCompetitionClick(competition)
                addClickAnimation()
            }

            // Long click for standings (if supported)
            root.setOnLongClickListener {
                if (onStandingsClick != null && competition.type == CompetitionType.LEAGUE) {
                    onStandingsClick.invoke(competition)
                    true
                } else {
                    false
                }
            }

            favoriteButton.setOnClickListener {
                onFavoriteClick(competition)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun setupStandingsButton(competition: Competition) = with(binding) {
            // Add a standings button for leagues (not for cups or tournaments)
            if (competition.type == CompetitionType.LEAGUE && onStandingsClick != null) {
                // We can add a standings button or use long press
                // For now, we'll use long press and show a hint
                root.tooltipText = "Long press to view standings"
            }
        }

        private fun loadCompetitionImage(imageView: ImageView, logoUrl: String?, flagUrl: String?) {
            // Prioritize league logo over country flag
            val imageUrl = logoUrl ?: flagUrl

            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_competition)
                .centerInside() // Use centerInside for better logo display
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(imageView.context)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_competition)
            }
        }

        private fun setupCompetitionType(competition: Competition) = with(binding) {
            when (competition.type) {
                CompetitionType.LEAGUE -> {
                    competitionType.visibility = View.GONE
                }
                CompetitionType.CUP -> {
                    competitionType.text = "CUP"
                    competitionType.visibility = View.VISIBLE
                    competitionType.setBackgroundResource(R.drawable.competition_type_bg)
                    competitionType.setTextColor(ContextCompat.getColor(root.context, R.color.accent_color))
                }
                CompetitionType.TOURNAMENT -> {
                    competitionType.text = "TOURNAMENT"
                    competitionType.visibility = View.VISIBLE
                    competitionType.setBackgroundResource(R.drawable.competition_type_bg)
                    competitionType.setTextColor(ContextCompat.getColor(root.context, R.color.accent_color))
                }
                CompetitionType.INTERNATIONAL -> {
                    competitionType.text = "INTERNATIONAL"
                    competitionType.visibility = View.VISIBLE
                    competitionType.setBackgroundResource(R.drawable.indicator_background)
                    competitionType.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
            }
        }

        private fun updateFavoriteButton(isFavorite: Boolean) = with(binding) {
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
                favoriteButton.setColorFilter(
                    ContextCompat.getColor(root.context, R.color.accent_color)
                )

                // Add favorite animation
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
                favoriteButton.setColorFilter(
                    ContextCompat.getColor(root.context, android.R.color.darker_gray)
                )
            }
        }

        private fun setupCompetitionStats(competition: Competition) = with(binding) {
            // Season info
            if (!competition.season.isNullOrEmpty()) {
                seasonInfo.text = "Season ${competition.season}"
                seasonInfo.visibility = View.VISIBLE

                // Add league indicator for standings
                if (competition.type == CompetitionType.LEAGUE) {
                    seasonInfo.text = "${seasonInfo.text} • Long press for table"
                    seasonInfo.setTextColor(ContextCompat.getColor(root.context, R.color.accent_color))
                }
            } else {
                seasonInfo.visibility = View.GONE
            }

            // Teams count based on known competitions
            if (competition.isTopCompetition) {
                teamsCount.text = getTeamsCount(competition.name, competition.id)
                teamsCount.visibility = if (teamsCount.text.isNotEmpty()) View.VISIBLE else View.GONE
            } else {
                teamsCount.visibility = View.GONE
            }

            // Show stats container if any stat is visible
            statsContainer.visibility = if (
                seasonInfo.visibility == View.VISIBLE ||
                teamsCount.visibility == View.VISIBLE
            ) View.VISIBLE else View.GONE
        }

        private fun getTeamsCount(competitionName: String, competitionId: String): String {
            return when (competitionId) {
                "39", "140", "135", "61" -> "20 Teams" // Premier League, La Liga, Serie A, Ligue 1
                "78" -> "18 Teams" // Bundesliga
                "2" -> "32 Teams" // Champions League
                "3" -> "48 Teams" // Europa League
                "203" -> "20 Teams" // Turkish Super League
                "342" -> "10 Teams" // Azerbaijan Premier League
                "88" -> "18 Teams" // Eredivisie
                "94" -> "18 Teams" // Primeira Liga
                else -> when {
                    competitionName.contains("Premier League", ignoreCase = true) -> "20 Teams"
                    competitionName.contains("La Liga", ignoreCase = true) -> "20 Teams"
                    competitionName.contains("Serie A", ignoreCase = true) -> "20 Teams"
                    competitionName.contains("Ligue 1", ignoreCase = true) -> "20 Teams"
                    competitionName.contains("Bundesliga", ignoreCase = true) -> "18 Teams"
                    competitionName.contains("Champions League", ignoreCase = true) -> "32 Teams"
                    competitionName.contains("Europa League", ignoreCase = true) -> "48 Teams"
                    competitionName.contains("Cup", ignoreCase = true) -> "Cup Format"
                    else -> ""
                }
            }
        }

        private fun addCurrentSeasonIndicator() = with(binding) {
            // Add a subtle indicator for current season
            seasonInfo.setTextColor(ContextCompat.getColor(root.context, R.color.accent_color))
            seasonInfo.animate()
                .alpha(0.8f)
                .setDuration(1000)
                .withEndAction {
                    seasonInfo.animate()
                        .alpha(1.0f)
                        .setDuration(1000)
                        .start()
                }
                .start()
        }

        private fun addClickAnimation() = with(binding) {
            root.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    root.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    class CompetitionDiffCallback : DiffUtil.ItemCallback<Competition>() {
        override fun areItemsTheSame(oldItem: Competition, newItem: Competition): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Competition, newItem: Competition): Boolean {
            return oldItem == newItem
        }
    }
}