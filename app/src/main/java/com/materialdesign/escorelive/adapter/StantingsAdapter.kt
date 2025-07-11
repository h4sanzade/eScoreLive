package com.materialdesign.escorelive.ui.matchdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.remote.TeamStanding
import com.materialdesign.escorelive.databinding.ItemStandingBinding

class StandingsAdapter : ListAdapter<TeamStanding, StandingsAdapter.StandingViewHolder>(StandingDiffCallback()) {

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
            // Position
            positionText.text = standing.rank.toString()

            // Position background color based on rank
            when (standing.rank) {
                in 1..4 -> positionText.setBackgroundResource(R.drawable.position_champions_bg)
                in 5..6 -> positionText.setBackgroundResource(R.drawable.position_europa_bg)
                in 18..20 -> positionText.setBackgroundResource(R.drawable.position_relegation_bg)
                else -> positionText.setBackgroundResource(R.drawable.position_normal_bg)
            }

            // Team logo and name
            Glide.with(root.context)
                .load(standing.team.logo)
                .placeholder(R.drawable.ic_placeholder)
                .into(teamLogo)

            teamName.text = standing.team.name

            // Stats
            playedText.text = standing.all.played.toString()
            wonText.text = standing.all.win.toString()
            drawnText.text = standing.all.draw.toString()
            lostText.text = standing.all.lose.toString()
            gfText.text = standing.all.goals.`for`.toString()
            gaText.text = standing.all.goals.against.toString()
            gdText.text = standing.goalsDiff.toString()
            pointsText.text = standing.points.toString()

            // Goal difference color
            when {
                standing.goalsDiff > 0 -> gdText.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                standing.goalsDiff < 0 -> gdText.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_red_light))
                else -> gdText.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
            }

            // Form indicators
            if (standing.form.isNotEmpty()) {
                formText.text = standing.form.takeLast(5)
                formText.visibility = android.view.View.VISIBLE
            } else {
                formText.visibility = android.view.View.GONE
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