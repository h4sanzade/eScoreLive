package com.materialdesign.escorelive.ui.matchdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemLineupPlayerBinding

class LineupAdapter : ListAdapter<LineupPlayer, LineupAdapter.LineupViewHolder>(LineupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineupViewHolder {
        val binding = ItemLineupPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LineupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LineupViewHolder(private val binding: ItemLineupPlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(player: LineupPlayer) = with(binding) {
            playerNumber.text = player.number.toString()
            playerName.text = player.name
            playerPosition.text = player.position

            // Show rating if available
            if (player.rating != null && player.rating > 0) {
                playerRating.text = String.format("%.1f", player.rating)
                playerRating.visibility = android.view.View.VISIBLE

                // Color rating based on value
                val ratingColor = when {
                    player.rating >= 8.0f -> ContextCompat.getColor(root.context, android.R.color.holo_green_dark)
                    player.rating >= 7.0f -> ContextCompat.getColor(root.context, android.R.color.holo_orange_light)
                    player.rating >= 6.0f -> ContextCompat.getColor(root.context, android.R.color.holo_orange_dark)
                    else -> ContextCompat.getColor(root.context, android.R.color.holo_red_light)
                }
                playerRating.setTextColor(ratingColor)
            } else {
                playerRating.visibility = android.view.View.GONE
            }

            // Style based on starting/substitute
            if (player.isStarting) {
                playerCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.starting_player_bg))
                startingIndicator.visibility = android.view.View.VISIBLE
                startingIndicator.text = "Starting XI"
            } else {
                playerCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.substitute_player_bg))
                startingIndicator.visibility = android.view.View.VISIBLE
                startingIndicator.text = "Substitute"
            }

            // Team color indicator
            if (player.isHomeTeam) {
                teamIndicator.setBackgroundColor(ContextCompat.getColor(root.context, R.color.home_team_color))
            } else {
                teamIndicator.setBackgroundColor(ContextCompat.getColor(root.context, R.color.away_team_color))
            }

            // Jersey number styling
            playerNumber.setBackgroundResource(R.drawable.jersey_number_bg)
        }
    }

    class LineupDiffCallback : DiffUtil.ItemCallback<LineupPlayer>() {
        override fun areItemsTheSame(oldItem: LineupPlayer, newItem: LineupPlayer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LineupPlayer, newItem: LineupPlayer): Boolean {
            return oldItem == newItem
        }
    }
}