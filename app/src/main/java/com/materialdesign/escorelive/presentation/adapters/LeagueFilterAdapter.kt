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
import com.materialdesign.escorelive.databinding.ItemLeagueFilterBinding
import com.materialdesign.escorelive.presentation.filter.League

class LeagueFilterAdapter(
    private val onLeagueClick: (League) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<League, LeagueFilterAdapter.LeagueFilterViewHolder>(LeagueDiffCallback()) {

    private var selectedLeagues: Set<String> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueFilterViewHolder {
        val binding = ItemLeagueFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeagueFilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeagueFilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateSelectedLeagues(selected: Set<String>) {
        selectedLeagues = selected
        notifyDataSetChanged()
        onSelectionChanged(selected.size)
    }

    inner class LeagueFilterViewHolder(
        private val binding: ItemLeagueFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(league: League) = with(binding) {
            // Set league data
            leagueName.text = league.name
            leagueCountry.text = league.country

            // Load league logo
            loadLeagueImage(leagueFlagImage, league.logoUrl)

            // Set selection state
            val isSelected = selectedLeagues.contains(league.name)
            leagueCheckbox.isChecked = isSelected

            // Update UI based on selection
            updateSelectionUI(isSelected)

            // Handle clicks
            root.setOnClickListener {
                onLeagueClick(league)
                addClickAnimation()
            }

            leagueCheckbox.setOnClickListener {
                onLeagueClick(league)
            }
        }

        private fun loadLeagueImage(imageView: ImageView, url: String?) {
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

        private fun updateSelectionUI(isSelected: Boolean) = with(binding) {
            if (isSelected) {
                // Selected state
                root.setBackgroundColor(
                    ContextCompat.getColor(root.context, R.color.filter_selected_bg)
                )
                leagueName.setTextColor(
                    ContextCompat.getColor(root.context, R.color.accent_color)
                )
                leagueCountry.setTextColor(
                    ContextCompat.getColor(root.context, R.color.accent_color)
                )
            } else {
                // Normal state
                root.setBackgroundColor(
                    ContextCompat.getColor(root.context, android.R.color.transparent)
                )
                leagueName.setTextColor(
                    ContextCompat.getColor(root.context, R.color.white)
                )
                leagueCountry.setTextColor(
                    ContextCompat.getColor(root.context, android.R.color.darker_gray)
                )
            }
        }

        private fun addClickAnimation() = with(binding) {
            root.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    root.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    class LeagueDiffCallback : DiffUtil.ItemCallback<League>() {
        override fun areItemsTheSame(oldItem: League, newItem: League): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: League, newItem: League): Boolean {
            return oldItem == newItem
        }
    }
}