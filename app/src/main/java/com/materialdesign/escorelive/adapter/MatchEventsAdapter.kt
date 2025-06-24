package com.materialdesign.escorelive.ui.matchdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemMatchEventBinding

class MatchEventsAdapter : ListAdapter<MatchEvent, MatchEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemMatchEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemMatchEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: MatchEvent) = with(binding) {
            eventMinute.text = "${event.minute}'"
            playerName.text = event.player
            eventDetail.text = event.detail

            // Set assist player if available
            if (event.assistPlayer != null) {
                assistText.text = "Assist: ${event.assistPlayer}"
                assistText.visibility = android.view.View.VISIBLE
            } else {
                assistText.visibility = android.view.View.GONE
            }

            // Set event icon and color based on type
            when (event.type.lowercase()) {
                "goal" -> {
                    eventIcon.setImageResource(R.drawable.ic_goal)
                    eventIcon.setColorFilter(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                }
                "card" -> {
                    if (event.detail.contains("Yellow", ignoreCase = true)) {
                        eventIcon.setImageResource(R.drawable.ic_yellow_card)
                        eventIcon.setColorFilter(ContextCompat.getColor(root.context, android.R.color.holo_orange_dark))
                    } else {
                        eventIcon.setImageResource(R.drawable.ic_red_card)
                        eventIcon.setColorFilter(ContextCompat.getColor(root.context, android.R.color.holo_red_dark))
                    }
                }
                "substitution" -> {
                    eventIcon.setImageResource(R.drawable.ic_substitution)
                    eventIcon.setColorFilter(ContextCompat.getColor(root.context, android.R.color.holo_blue_dark))
                }
                else -> {
                    eventIcon.setImageResource(R.drawable.ic_event)
                    eventIcon.setColorFilter(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
            }

            // Align event based on team (home team on left, away team on right)
            if (event.isHomeTeam) {
                // Home team events on the left
                root.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
                teamIndicator.setBackgroundColor(ContextCompat.getColor(root.context, R.color.home_team_color))
            } else {
                // Away team events on the right
                root.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
                teamIndicator.setBackgroundColor(ContextCompat.getColor(root.context, R.color.away_team_color))
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<MatchEvent>() {
        override fun areItemsTheSame(oldItem: MatchEvent, newItem: MatchEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MatchEvent, newItem: MatchEvent): Boolean {
            return oldItem == newItem
        }
    }
}