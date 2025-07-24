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
import com.materialdesign.escorelive.data.remote.TeamStanding
import com.materialdesign.escorelive.databinding.ItemStandingBinding

class StandingsAdapter : ListAdapter<TeamStanding, StandingsAdapter.StandingViewHolder>(
    StandingDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingViewHolder {
        val binding = ItemStandingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StandingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StandingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StandingViewHolder(private val binding: ItemStandingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(standing: TeamStanding) = with(binding) {
            positionText.text = standing.rank.toString()
            setupPositionBackground(standing.rank)

            loadImage(teamLogo, standing.team.logo)
            teamName.text = standing.team.name

            playedText.text = standing.all.played.toString()
            wonText.text = standing.all.win.toString()
            drawnText.text = standing.all.draw.toString()
            lostText.text = standing.all.lose.toString()
            gfText.text = standing.all.goals.`for`.toString()
            gaText.text = standing.all.goals.against.toString()
            pointsText.text = standing.points.toString()

            setupGoalDifference(standing.goalsDiff)

            setupTeamForm(standing.form)
        }

        private fun setupPositionBackground(rank: Int) = with(binding) {
            val backgroundRes = when (rank) {
                in 1..4 -> {
                    R.drawable.position_champions_bg
                }
                in 5..6 -> {
                    R.drawable.position_europa_bg
                }
                in 18..20 -> {
                    R.drawable.position_relegation_bg
                }
                else -> {

                    R.drawable.position_normal_bg
                }
            }

            positionText.setBackgroundResource(backgroundRes)

            if (rank <= 3) {
                positionText.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(300)
                    .withEndAction {
                        positionText.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
            }
        }

        private fun setupGoalDifference(goalsDiff: Int) = with(binding) {
            gdText.text = when {
                goalsDiff > 0 -> "+$goalsDiff"
                goalsDiff < 0 -> goalsDiff.toString()
                else -> "0"
            }

            val colorRes = when {
                goalsDiff > 0 -> android.R.color.holo_green_dark
                goalsDiff < 0 -> android.R.color.holo_red_light
                else -> android.R.color.darker_gray
            }

            gdText.setTextColor(ContextCompat.getColor(root.context, colorRes))
        }

        private fun setupTeamForm(form: String) = with(binding) {
            if (form.isNotEmpty()) {
                val recentForm = form.takeLast(5)
                formText.text = recentForm
                formText.visibility = android.view.View.VISIBLE

                val formColor = calculateFormColor(recentForm)
                formText.setTextColor(ContextCompat.getColor(root.context, formColor))
            } else {
                formText.visibility = android.view.View.GONE
            }
        }

        private fun calculateFormColor(form: String): Int {
            val wins = form.count { it == 'W' }
            val draws = form.count { it == 'D' }
            val losses = form.count { it == 'L' }

            return when {
                wins >= 4 -> android.R.color.holo_green_dark
                wins >= 2 && losses <= 1 -> android.R.color.holo_green_light
                draws >= 3 -> android.R.color.holo_orange_light
                losses >= 3 -> android.R.color.holo_red_light
                else -> android.R.color.darker_gray
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

    class StandingDiffCallback : DiffUtil.ItemCallback<TeamStanding>() {
        override fun areItemsTheSame(oldItem: TeamStanding, newItem: TeamStanding): Boolean {
            return oldItem.team.id == newItem.team.id
        }

        override fun areContentsTheSame(oldItem: TeamStanding, newItem: TeamStanding): Boolean {
            return oldItem == newItem
        }
    }
}