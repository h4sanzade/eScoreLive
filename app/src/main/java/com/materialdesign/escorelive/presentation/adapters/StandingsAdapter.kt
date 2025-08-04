package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.remote.TeamStanding

class StandingsAdapter : ListAdapter<TeamStanding, StandingsAdapter.StandingViewHolder>(
    StandingDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_standing_row, parent, false)
        return StandingViewHolder(view)
    }

    override fun onBindViewHolder(holder: StandingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StandingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val positionText: TextView = itemView.findViewById(R.id.position_text)
        private val teamLogo: ImageView = itemView.findViewById(R.id.team_logo)
        private val teamName: TextView = itemView.findViewById(R.id.team_name)
        private val playedText: TextView = itemView.findViewById(R.id.played_text)
        private val wonText: TextView = itemView.findViewById(R.id.won_text)
        private val drawnText: TextView = itemView.findViewById(R.id.drawn_text)
        private val lostText: TextView = itemView.findViewById(R.id.lost_text)
        private val gfText: TextView = itemView.findViewById(R.id.gf_text)
        private val gaText: TextView = itemView.findViewById(R.id.ga_text)
        private val gdText: TextView = itemView.findViewById(R.id.gd_text)
        private val pointsText: TextView = itemView.findViewById(R.id.points_text)
        private val formText: TextView = itemView.findViewById(R.id.form_text)

        fun bind(standing: TeamStanding) {
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

            addEntranceAnimation()
        }

        private fun setupPositionBackground(rank: Int) {
            val (backgroundRes, textColorRes) = when (rank) {
                in 1..4 -> {
                    Pair(R.drawable.position_champions_bg, android.R.color.white)
                }
                in 5..6 -> {
                    Pair(R.drawable.position_europa_bg, android.R.color.white)
                }
                in 18..20 -> {
                    Pair(R.drawable.position_relegation_bg, android.R.color.white)
                }
                else -> {
                    Pair(R.drawable.position_normal_bg, android.R.color.black)
                }
            }

            positionText.setBackgroundResource(backgroundRes)
            positionText.setTextColor(ContextCompat.getColor(itemView.context, textColorRes))

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

        private fun setupGoalDifference(goalsDiff: Int) {
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

            gdText.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
        }

        private fun setupTeamForm(form: String) {
            if (form.isNotEmpty()) {
                val recentForm = form.takeLast(5)
                formText.text = recentForm
                formText.visibility = View.VISIBLE

                val formColor = calculateFormColor(recentForm)
                formText.setTextColor(ContextCompat.getColor(itemView.context, formColor))

                val formBgRes = when {
                    recentForm.count { it == 'W' } >= 4 -> R.drawable.form_excellent_bg
                    recentForm.count { it == 'W' } >= 2 -> R.drawable.form_good_bg
                    recentForm.count { it == 'L' } >= 3 -> R.drawable.form_poor_bg
                    else -> R.drawable.form_average_bg
                }
                formText.setBackgroundResource(formBgRes)
            } else {
                formText.visibility = View.GONE
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

        private fun addEntranceAnimation() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val slideDirection = if (position % 2 == 0) -50f else 50f
                itemView.translationX = slideDirection
                itemView.alpha = 0f

                itemView.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay((position % 10) * 30L)
                    .start()
            }
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