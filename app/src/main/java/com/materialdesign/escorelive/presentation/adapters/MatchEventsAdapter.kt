package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemMatchEventBinding
import com.materialdesign.escorelive.ui.matchdetail.MatchEvent

class MatchEventsAdapter : ListAdapter<MatchEvent, MatchEventsAdapter.EventViewHolder>(
    EventDiffCallback()
) {

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

            if (event.assistPlayer != null) {
                assistText.text = "Assist: ${event.assistPlayer}"
                assistText.visibility = android.view.View.VISIBLE
            } else {
                assistText.visibility = android.view.View.GONE
            }

            setupEventIcon(event)

            setupTeamIndicator(event)
        }

        private fun setupEventIcon(event: MatchEvent) = with(binding) {
            val (iconRes, colorRes) = when (event.type.lowercase()) {
                "goal" -> {
                    eventIcon.animate()
                        .rotationBy(360f)
                        .setDuration(1000)
                        .start()

                    Pair(R.drawable.ic_goal, android.R.color.holo_green_dark)
                }
                "card" -> {
                    if (event.detail.contains("Yellow", ignoreCase = true)) {
                        Pair(R.drawable.ic_yellow_card, android.R.color.holo_orange_dark)
                    } else {
                        eventIcon.animate()
                            .translationX(-10f)
                            .setDuration(100)
                            .withEndAction {
                                eventIcon.animate()
                                    .translationX(10f)
                                    .setDuration(100)
                                    .withEndAction {
                                        eventIcon.animate()
                                            .translationX(0f)
                                            .setDuration(100)
                                            .start()
                                    }
                                    .start()
                            }
                            .start()

                        Pair(R.drawable.ic_red_card, android.R.color.holo_red_dark)
                    }
                }
                "substitution" -> {
                    Pair(R.drawable.ic_substitution, android.R.color.holo_blue_dark)
                }
                "var" -> {
                    Pair(R.drawable.ic_event, android.R.color.holo_purple)
                }
                else -> {
                    Pair(R.drawable.ic_event, android.R.color.darker_gray)
                }
            }

            eventIcon.setImageResource(iconRes)
            eventIcon.setColorFilter(ContextCompat.getColor(root.context, colorRes))
        }

        private fun setupTeamIndicator(event: MatchEvent) = with(binding) {
            val teamColor = if (event.isHomeTeam) {
                R.color.home_team_color
            } else {
                R.color.away_team_color
            }

            teamIndicator.setBackgroundColor(ContextCompat.getColor(root.context, teamColor))

            if (event.isHomeTeam) {
                root.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
            } else {
                root.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            }

            val slideDirection = if (event.isHomeTeam) -50f else 50f
            root.translationX = slideDirection
            root.animate()
                .translationX(0f)
                .setDuration(300)
                .start()
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