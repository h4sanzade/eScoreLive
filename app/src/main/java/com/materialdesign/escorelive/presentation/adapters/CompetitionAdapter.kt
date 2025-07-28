// CompetitionAdapter.kt
package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.View
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
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionType
import com.materialdesign.escorelive.databinding.ItemCompetitionBinding

class CompetitionAdapter(
    private val onCompetitionClick: (Competition) -> Unit,
    private val onFavoriteClick: (Competition) -> Unit
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

            loadCompetitionImage(flagImage, competition.flagUrl, competition.logoUrl)

            setupCompetitionType(competition)

            updateFavoriteButton(competition.isFavorite)

            setupCompetitionStats(competition)

            root.setOnClickListener {
                onCompetitionClick(competition)
                addClickAnimation()
            }

            favoriteButton.setOnClickListener {
                onFavoriteClick(competition)
            }
        }

        private fun loadCompetitionImage(imageView: ImageView, flagUrl: String?, logoUrl: String?) {
            val imageUrl = flagUrl ?: logoUrl

            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_competition)
                .centerCrop()
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
                    competitionType.text = "LEAGUE"
                    competitionType.visibility = View.GONE
                }
                CompetitionType.CUP -> {
                    competitionType.text = "CUP"
                    competitionType.visibility = View.VISIBLE
                    competitionType.setBackgroundResource(R.drawable.competition_type_bg)
                }
                CompetitionType.TOURNAMENT -> {
                    competitionType.text = "TOURNAMENT"
                    competitionType.visibility = View.VISIBLE
                    competitionType.setBackgroundResource(R.drawable.competition_type_bg)
                }
                CompetitionType.INTERNATIONAL -> {
                    competitionType.text = "INTERNATIONAL"
                    competitionType.visibility = View.VISIBLE
                    competitionType.setBackgroundResource(R.drawable.competition_type_bg)
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
            if (!competition.season.isNullOrEmpty()) {
                seasonInfo.text = competition.season
                seasonInfo.visibility = View.VISIBLE
            } else {
                seasonInfo.visibility = View.GONE
            }

            if (competition.isTopCompetition) {
                teamsCount.text = when (competition.name.lowercase()) {
                    "premier league", "la liga", "serie a", "ligue 1" -> "20 Teams"
                    "bundesliga" -> "18 Teams"
                    "champions league" -> "32 Teams"
                    "europa league" -> "48 Teams"
                    else -> ""
                }
                teamsCount.visibility = if (teamsCount.text.isNotEmpty()) View.VISIBLE else View.GONE
            } else {
                teamsCount.visibility = View.GONE
            }

            statsContainer.visibility = if (
                seasonInfo.visibility == View.VISIBLE ||
                teamsCount.visibility == View.VISIBLE
            ) View.VISIBLE else View.GONE
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