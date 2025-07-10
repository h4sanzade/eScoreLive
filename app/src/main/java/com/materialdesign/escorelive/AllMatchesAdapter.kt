package com.materialdesign.escorelive.ui.allmatchs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.materialdesign.escorelive.LiveMatch
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemAllMatchesBinding
import java.text.SimpleDateFormat
import java.util.*

class AllMatchesAdapter(
    private val onMatchClick: (LiveMatch) -> Unit
) : ListAdapter<LiveMatch, AllMatchesAdapter.AllMatchesViewHolder>(AllMatchesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllMatchesViewHolder {
        val binding = ItemAllMatchesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllMatchesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AllMatchesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AllMatchesViewHolder(private val binding: ItemAllMatchesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: LiveMatch) = with(binding) {
            // League info
            leagueName.text = match.league.name
            loadImage(leagueLogo, match.league.logo)

            // Teams
            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name
            loadImage(homeTeamLogo, match.homeTeam.logo)
            loadImage(awayTeamLogo, match.awayTeam.logo)

            // Match time and status
            matchTime.text = match.matchMinute

            when {
                match.isLive -> {
                    liveIndicator.visibility = android.view.View.VISIBLE
                    liveIndicator.setBackgroundResource(R.drawable.live_indicator_bg)
                    liveText.text = "LIVE"
                    liveText.setTextColor(ContextCompat.getColor(root.context, android.R.color.white))

                    homeScore.visibility = android.view.View.VISIBLE
                    awayScore.visibility = android.view.View.VISIBLE
                    homeScore.text = match.homeScore.toString()
                    awayScore.text = match.awayScore.toString()

                    matchTime.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_red_light))
                    statusCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.live_match_bg))
                }
                match.isFinished -> {
                    liveIndicator.visibility = android.view.View.VISIBLE
                    liveIndicator.setBackgroundResource(R.drawable.finished_indicator_bg)
                    liveText.text = "FT"
                    liveText.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

                    homeScore.visibility = android.view.View.VISIBLE
                    awayScore.visibility = android.view.View.VISIBLE
                    homeScore.text = match.homeScore.toString()
                    awayScore.text = match.awayScore.toString()

                    matchTime.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                    statusCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.finished_match_bg))
                }
                match.isUpcoming -> {
                    liveIndicator.visibility = android.view.View.VISIBLE
                    liveIndicator.setBackgroundResource(R.drawable.upcoming_indicator_bg)
                    liveText.text = "VS"
                    liveText.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_blue_light))

                    homeScore.visibility = android.view.View.GONE
                    awayScore.visibility = android.view.View.GONE

                    matchTime.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_blue_light))
                    statusCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.upcoming_match_bg))
                }
                else -> {
                    liveIndicator.visibility = android.view.View.GONE
                    homeScore.visibility = android.view.View.VISIBLE
                    awayScore.visibility = android.view.View.VISIBLE

                    if (match.homeScore > 0 || match.awayScore > 0) {
                        homeScore.text = match.homeScore.toString()
                        awayScore.text = match.awayScore.toString()
                    } else {
                        homeScore.visibility = android.view.View.GONE
                        awayScore.visibility = android.view.View.GONE
                    }

                    matchTime.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                    statusCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.card_background))
                }
            }

            // Format and display match date
            displayMatchDate(match)

            root.setOnClickListener { onMatchClick(match) }
        }

        private fun displayMatchDate(match: LiveMatch) = with(binding) {
            match.kickoffTime?.let { kickoffTime ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                    val date = inputFormat.parse(kickoffTime)

                    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    date?.let {
                        val today = Calendar.getInstance()
                        val matchDate = Calendar.getInstance()
                        matchDate.time = it

                        val dateText = when {
                            isSameDay(today, matchDate) -> "Today"
                            isYesterday(today, matchDate) -> "Yesterday"
                            isTomorrow(today, matchDate) -> "Tomorrow"
                            else -> dateFormat.format(it)
                        }

                        binding.matchDate.text = "$dateText, ${timeFormat.format(it)}"
                    }
                } catch (e: Exception) {
                    matchDate.text = match.kickoffTimeFormatted ?: "TBD"
                }
            } ?: run {
                binding.matchDate.text = "TBD"
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        private fun isYesterday(today: Calendar, date: Calendar): Boolean {
            val yesterday = today.clone() as Calendar
            yesterday.add(Calendar.DAY_OF_MONTH, -1)
            return isSameDay(yesterday, date)
        }

        private fun isTomorrow(today: Calendar, date: Calendar): Boolean {
            val tomorrow = today.clone() as Calendar
            tomorrow.add(Calendar.DAY_OF_MONTH, 1)
            return isSameDay(tomorrow, date)
        }

        private fun loadImage(imageView: android.widget.ImageView, url: String) {
            Glide.with(imageView.context)
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }
    }

    class AllMatchesDiffCallback : DiffUtil.ItemCallback<LiveMatch>() {
        override fun areItemsTheSame(oldItem: LiveMatch, newItem: LiveMatch): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LiveMatch, newItem: LiveMatch): Boolean {
            return oldItem == newItem
        }
    }
}