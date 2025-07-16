package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemLineupPlayerBinding
import com.materialdesign.escorelive.ui.matchdetail.LineupPlayer

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
            // Player basic info
            playerNumber.text = player.number.toString()
            playerName.text = player.name
            playerPosition.text = player.position

            // Setup player rating
            setupPlayerRating(player)

            // Setup starting/substitute status
            setupPlayerStatus(player)

            // Setup team indicator
            setupTeamIndicator(player)

            // Add entrance animation
            addEntranceAnimation()
        }

        private fun setupPlayerRating(player: LineupPlayer) = with(binding) {
            if (player.rating != null && player.rating > 0) {
                playerRating.text = String.format("%.1f", player.rating)
                playerRating.visibility = android.view.View.VISIBLE

                val (ratingColor, ratingBackground) = when {
                    player.rating >= 8.5f -> {
                        // Excellent performance
                        Pair(android.R.color.white, android.R.color.holo_green_dark)
                    }
                    player.rating >= 8.0f -> {
                        Pair(android.R.color.white, android.R.color.holo_green_light)
                    }
                    player.rating >= 7.0f -> {
                        Pair(android.R.color.white, android.R.color.holo_orange_light)
                    }
                    player.rating >= 6.0f -> {
                        Pair(android.R.color.white, android.R.color.holo_orange_dark)
                    }
                    else -> {
                        // Poor performance
                        Pair(android.R.color.white, android.R.color.holo_red_light)
                    }
                }

                playerRating.setTextColor(ContextCompat.getColor(root.context, ratingColor))
                playerRating.setBackgroundResource(ratingBackground)

                // Add pulse animation for high ratings
                if (player.rating >= 8.0f) {
                    playerRating.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(500)
                        .withEndAction {
                            playerRating.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(500)
                                .start()
                        }
                        .start()
                }
            } else {
                playerRating.visibility = android.view.View.GONE
            }
        }

        private fun setupPlayerStatus(player: LineupPlayer) = with(binding) {
            if (player.isStarting) {
                playerCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.starting_player_bg))
                startingIndicator.visibility = android.view.View.VISIBLE
                startingIndicator.text = "Starting XI"
                startingIndicator.setBackgroundResource(R.drawable.indicator_background)
                startingIndicator.setTextColor(ContextCompat.getColor(root.context, R.color.accent_color))
            } else {
                playerCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.substitute_player_bg))
                startingIndicator.visibility = android.view.View.VISIBLE
                startingIndicator.text = "Substitute"
                startingIndicator.setBackgroundResource(R.drawable.filter_unselected_bg)
                startingIndicator.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
            }

            // Jersey number styling
            playerNumber.setBackgroundResource(R.drawable.jersey_number_bg)

            // Position-based styling
            val positionColor = when (player.position.uppercase()) {
                "GK" -> android.R.color.holo_orange_light
                in listOf("CB", "LB", "RB", "LWB", "RWB") -> android.R.color.holo_blue_light
                in listOf("CDM", "CM", "CAM", "LM", "RM") -> android.R.color.holo_green_light
                in listOf("LW", "RW", "CF", "ST") -> android.R.color.holo_red_light
                else -> android.R.color.darker_gray
            }

            playerPosition.setTextColor(ContextCompat.getColor(root.context, positionColor))
        }

        private fun setupTeamIndicator(player: LineupPlayer) = with(binding) {
            val teamColor = if (player.isHomeTeam) {
                R.color.home_team_color
            } else {
                R.color.away_team_color
            }

            teamIndicator.setBackgroundColor(ContextCompat.getColor(root.context, teamColor))
        }

        private fun addEntranceAnimation() = with(binding) {
            // Slide in animation based on team
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val slideDirection = if (position % 2 == 0) -100f else 100f
                root.translationX = slideDirection
                root.alpha = 0f

                root.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay((position % 5) * 50L) // Staggered animation
                    .start()
            }
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