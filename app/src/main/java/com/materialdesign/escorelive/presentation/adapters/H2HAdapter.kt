package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.materialdesign.escorelive.domain.model.LiveMatch
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemH2hMatchBinding
import java.text.SimpleDateFormat
import java.util.*

class H2HAdapter : ListAdapter<LiveMatch, H2HAdapter.H2HViewHolder>(H2HDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H2HViewHolder {
        val binding = ItemH2hMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return H2HViewHolder(binding)
    }

    override fun onBindViewHolder(holder: H2HViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class H2HViewHolder(private val binding: ItemH2hMatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: LiveMatch) = with(binding) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            try {
                match.kickoffTime?.let { kickoffTime ->
                    val date = inputFormat.parse(kickoffTime)
                    date?.let {
                        matchDate.text = outputFormat.format(it)
                    }
                }
            } catch (e: Exception) {
                matchDate.text = "Date N/A"
            }

            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name

            homeScore.text = match.homeScore.toString()
            awayScore.text = match.awayScore.toString()

            leagueName.text = match.league.name

            // Load logos
            Glide.with(root.context)
                .load(match.homeTeam.logo)
                .placeholder(R.drawable.ic_placeholder)
                .into(homeTeamLogo)

            Glide.with(root.context)
                .load(match.awayTeam.logo)
                .placeholder(R.drawable.ic_placeholder)
                .into(awayTeamLogo)

            // Highlight winner
            when {
                match.homeScore > match.awayScore -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
                match.awayScore > match.homeScore -> {
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                }
                else -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
            }
        }
    }

    class H2HDiffCallback : DiffUtil.ItemCallback<LiveMatch>() {
        override fun areItemsTheSame(oldItem: LiveMatch, newItem: LiveMatch): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LiveMatch, newItem: LiveMatch): Boolean {
            return oldItem == newItem
        }
    }
}