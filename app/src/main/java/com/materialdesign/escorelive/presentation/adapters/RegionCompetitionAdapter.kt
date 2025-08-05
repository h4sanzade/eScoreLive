package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.data.remote.dto.Competition
import com.materialdesign.escorelive.data.remote.dto.CompetitionType

class RegionCompetitionAdapter(
    private val onCompetitionClick: (Competition) -> Unit,
    private val onFavoriteClick: (Competition) -> Unit,
    private val onCountryHeaderClick: (String) -> Unit,
    private val onStandingsClick: ((Competition) -> Unit)? = null
) : RecyclerView.Adapter<RegionCompetitionAdapter.RegionViewHolder>() {

    private val regionalData = mutableListOf<RegionItem>()

    sealed class RegionItem {
        data class CountryHeader(
            val countryName: String,
            val countryFlag: String?,
            val competitionCount: Int,
            val isExpanded: Boolean = true
        ) : RegionItem()

        data class CompetitionItem(
            val competition: Competition
        ) : RegionItem()
    }

    fun submitRegionalData(data: Map<String, List<Competition>>) {
        val newItems = mutableListOf<RegionItem>()

        val popularCountries = listOf("England", "Spain", "Germany", "Italy", "France", "Turkey", "Netherlands", "Portugal")
        val sortedCountries = data.keys.sortedWith { country1, country2 ->
            val priority1 = if (popularCountries.contains(country1)) popularCountries.indexOf(country1) else Int.MAX_VALUE
            val priority2 = if (popularCountries.contains(country2)) popularCountries.indexOf(country2) else Int.MAX_VALUE

            when {
                priority1 != Int.MAX_VALUE && priority2 != Int.MAX_VALUE -> priority1.compareTo(priority2)
                priority1 != Int.MAX_VALUE -> -1
                priority2 != Int.MAX_VALUE -> 1
                else -> country1.compareTo(country2)
            }
        }

        sortedCountries.forEach { countryName ->
            val competitions = data[countryName] ?: emptyList()
            if (competitions.isNotEmpty()) {
                val countryFlag = competitions.firstOrNull()?.flagUrl
                newItems.add(
                    RegionItem.CountryHeader(
                        countryName = countryName,
                        countryFlag = countryFlag,
                        competitionCount = competitions.size,
                        isExpanded = true // Always expanded for now
                    )
                )

                competitions.sortedWith(compareBy<Competition> { !it.isTopCompetition }.thenBy { it.name })
                    .forEach { competition ->
                        newItems.add(RegionItem.CompetitionItem(competition))
                    }
            }
        }

        val diffCallback = RegionDiffCallback(regionalData, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        regionalData.clear()
        regionalData.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = regionalData.size

    override fun getItemViewType(position: Int): Int {
        return when (regionalData[position]) {
            is RegionItem.CountryHeader -> VIEW_TYPE_COUNTRY_HEADER
            is RegionItem.CompetitionItem -> VIEW_TYPE_COMPETITION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        return when (viewType) {
            VIEW_TYPE_COUNTRY_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_country_header, parent, false)
                CountryHeaderViewHolder(view)
            }
            VIEW_TYPE_COMPETITION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_region_competition, parent, false)
                RegionCompetitionViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
        when (val item = regionalData[position]) {
            is RegionItem.CountryHeader -> {
                (holder as CountryHeaderViewHolder).bind(item)
            }
            is RegionItem.CompetitionItem -> {
                (holder as RegionCompetitionViewHolder).bind(item.competition)
            }
        }
    }

    abstract class RegionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class CountryHeaderViewHolder(itemView: View) : RegionViewHolder(itemView) {
        private val countryName: TextView = itemView.findViewById(R.id.country_name)
        private val countryFlag: ImageView = itemView.findViewById(R.id.country_flag)
        private val competitionCount: TextView = itemView.findViewById(R.id.competition_count)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expand_icon)

        fun bind(item: RegionItem.CountryHeader) {
            countryName.text = item.countryName
            competitionCount.text = "${item.competitionCount} competitions"

            // Load country flag
            if (!item.countryFlag.isNullOrEmpty()) {
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_competition)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)

                Glide.with(itemView.context)
                    .load(item.countryFlag)
                    .apply(requestOptions)
                    .into(countryFlag)
            } else {
                countryFlag.setImageResource(R.drawable.ic_competition)
            }

            expandIcon.setImageResource(
                if (item.isExpanded) R.drawable.ic_expand_less
                else R.drawable.ic_expand_more
            )

            val popularCountries = listOf("England", "Spain", "Germany", "Italy", "France", "Turkey")
            if (popularCountries.contains(item.countryName)) {
                countryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_color))
                itemView.setBackgroundResource(R.drawable.popular_country_bg)
            } else {
                countryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.card_background))
            }

            itemView.setOnClickListener {
                onCountryHeaderClick(item.countryName)
            }
        }
    }

    inner class RegionCompetitionViewHolder(itemView: View) : RegionViewHolder(itemView) {
        private val competitionName: TextView = itemView.findViewById(R.id.competition_name)
        private val competitionLogo: ImageView = itemView.findViewById(R.id.competition_logo)
        private val competitionType: TextView = itemView.findViewById(R.id.competition_type)
        private val seasonInfo: TextView = itemView.findViewById(R.id.season_info)
        private val favoriteButton: ImageView = itemView.findViewById(R.id.favorite_button)
        private val topBadge: TextView = itemView.findViewById(R.id.top_badge)

        fun bind(competition: Competition) {
            competitionName.text = competition.name

            val imageUrl = competition.logoUrl ?: competition.flagUrl
            if (!imageUrl.isNullOrEmpty()) {
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_competition)
                    .centerInside()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .into(competitionLogo)
            } else {
                competitionLogo.setImageResource(R.drawable.ic_competition)
            }

            // Competition type
            if (competition.type.displayName != "League") {
                competitionType.text = competition.type.displayName.uppercase()
                competitionType.visibility = View.VISIBLE
                competitionType.setBackgroundResource(R.drawable.competition_type_bg)
            } else {
                competitionType.visibility = View.GONE
            }

            if (!competition.season.isNullOrEmpty()) {
                val seasonText = "Season ${competition.season}"
                seasonInfo.text = if (competition.type == CompetitionType.LEAGUE && onStandingsClick != null) {
                    "$seasonText • Long press for table"
                } else {
                    seasonText
                }
                seasonInfo.visibility = View.VISIBLE
            } else {
                seasonInfo.visibility = View.GONE
            }

            if (competition.isTopCompetition) {
                topBadge.visibility = View.VISIBLE
                topBadge.text = "TOP"
                topBadge.setBackgroundResource(R.drawable.indicator_background)

                topBadge.animate()
                    .alpha(0.8f)
                    .setDuration(1000)
                    .withEndAction {
                        topBadge.animate()
                            .alpha(1.0f)
                            .setDuration(1000)
                            .start()
                    }
                    .start()
            } else {
                topBadge.visibility = View.GONE
            }

            updateFavoriteButton(competition.isFavorite)

            itemView.setOnClickListener {
                onCompetitionClick(competition)
                addClickAnimation()
            }

            itemView.setOnLongClickListener {
                if (onStandingsClick != null && competition.type == CompetitionType.LEAGUE) {
                    onStandingsClick.invoke(competition)
                    true
                } else {
                    false
                }
            }

            favoriteButton.setOnClickListener {
                onFavoriteClick(competition)
            }
        }

        private fun updateFavoriteButton(isFavorite: Boolean) {
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
                favoriteButton.setColorFilter(ContextCompat.getColor(itemView.context, R.color.accent_color))
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline)
                favoriteButton.setColorFilter(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
            }
        }

        private fun addClickAnimation() {
            itemView.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    class RegionDiffCallback(
        private val oldList: List<RegionItem>,
        private val newList: List<RegionItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return when {
                oldItem is RegionItem.CountryHeader && newItem is RegionItem.CountryHeader -> {
                    oldItem.countryName == newItem.countryName
                }
                oldItem is RegionItem.CompetitionItem && newItem is RegionItem.CompetitionItem -> {
                    oldItem.competition.id == newItem.competition.id
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    companion object {
        private const val VIEW_TYPE_COUNTRY_HEADER = 1
        private const val VIEW_TYPE_COMPETITION = 2
    }
}