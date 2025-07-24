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
import com.materialdesign.escorelive.domain.model.Match
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.databinding.ItemAllMatchesBinding
import java.text.SimpleDateFormat
import java.util.*

class MatchListAdapter(
    private val onMatchClick: (Match) -> Unit
) : ListAdapter<Match, MatchListAdapter.MatchListViewHolder>(MatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchListViewHolder {
        val binding = ItemAllMatchesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MatchListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MatchListViewHolder(private val binding: ItemAllMatchesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(match: Match) = with(binding) {
            leagueName.text = match.league.name
            loadImage(leagueLogo, match.league.logo)

            homeTeamName.text = match.homeTeam.name
            awayTeamName.text = match.awayTeam.name
            loadImage(homeTeamLogo, match.homeTeam.logo)
            loadImage(awayTeamLogo, match.awayTeam.logo)

            matchTime.text = match.matchMinute

            setupMatchStatus(match)

            displayMatchDate(match)

            root.setOnClickListener { onMatchClick(match) }
        }

        private fun setupMatchStatus(match: Match) = with(binding) {
            when {
                match.isLive -> {
                    setupLiveStatus(match)
                }
                match.isFinished -> {
                    setupFinishedStatus(match)
                }
                match.isUpcoming -> {
                    setupUpcomingStatus(match)
                }
                else -> {
                    setupDefaultStatus(match)
                }
            }
        }

        private fun setupLiveStatus(match: Match) = with(binding) {
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

            liveIndicator.animate()
                .alpha(0.5f)
                .setDuration(1000)
                .withEndAction {
                    liveIndicator.animate()
                        .alpha(1.0f)
                        .setDuration(1000)
                        .start()
                }
                .start()
        }

        private fun setupFinishedStatus(match: Match) = with(binding) {
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

            highlightWinner(match)
        }

        private fun setupUpcomingStatus(match: Match) = with(binding) {
            liveIndicator.visibility = android.view.View.VISIBLE
            liveIndicator.setBackgroundResource(R.drawable.upcoming_indicator_bg)
            liveText.text = "VS"
            liveText.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_blue_light))

            homeScore.visibility = android.view.View.GONE
            awayScore.visibility = android.view.View.GONE

            matchTime.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_blue_light))
            statusCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.upcoming_match_bg))
        }

        private fun setupDefaultStatus(match: Match) = with(binding) {
            liveIndicator.visibility = android.view.View.GONE
            homeScore.visibility = android.view.View.VISIBLE
            awayScore.visibility = android.view.View.VISIBLE

            if (match.homeScore > 0 || match.awayScore > 0) {
                homeScore.text = match.homeScore.toString()
                awayScore.text = match.awayScore.toString()
                highlightWinner(match)
            } else {
                homeScore.visibility = android.view.View.GONE
                awayScore.visibility = android.view.View.GONE
            }

            matchTime.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
            statusCard.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.card_background))
        }

        private fun highlightWinner(match: Match) = with(binding) {
            when {
                match.homeScore > match.awayScore -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

                    homeScore.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(300)
                        .withEndAction {
                            homeScore.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                }
                match.awayScore > match.homeScore -> {
                    awayScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    homeScore.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))

                    awayScore.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(300)
                        .withEndAction {
                            awayScore.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                }
                else -> {
                    homeScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                    awayScore.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
            }
        }

        private fun displayMatchDate(match: Match) = with(binding) {
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
                    binding.matchDate.text = match.kickoffTimeFormatted ?: "TBD"
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

    class MatchDiffCallback : DiffUtil.ItemCallback<Match>() {
        override fun areItemsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Match, newItem: Match): Boolean {
            return oldItem == newItem
        }
    }
}