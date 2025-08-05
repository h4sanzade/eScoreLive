package com.materialdesign.escorelive.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.materialdesign.escorelive.R
import com.materialdesign.escorelive.presentation.filter.League

class LeagueFilterAdapter(
    private val onLeagueClick: (League) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<League, LeagueFilterAdapter.LeagueFilterViewHolder>(LeagueDiffCallback()) {

    private var selectedLeagues: Set<String> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueFilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_league_filter, parent, false)
        return LeagueFilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeagueFilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateSelectedLeagues(selected: Set<String>) {
        selectedLeagues = selected
        notifyDataSetChanged()
        val uniqueLeagueCount = selected.map { it.substringBeforeLast("_") }.distinct().size
        onSelectionChanged(uniqueLeagueCount)
    }

    inner class LeagueFilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardView: CardView = itemView as CardView
        private val leagueFlagImage: ImageView = itemView.findViewById(R.id.leagueFlagImage)
        private val leagueName: TextView = itemView.findViewById(R.id.leagueName)
        private val leagueCountry: TextView = itemView.findViewById(R.id.leagueCountry)
        private val leagueCheckbox: CheckBox = itemView.findViewById(R.id.leagueCheckbox)
        private val selectedBorder: View = itemView.findViewById(R.id.selectedBorder)

        fun bind(league: League) {
            leagueName.text = league.name
            leagueCountry.text = league.country

            val flagUrl = getCountryFlagUrl(league.country, league)
            loadCountryFlag(leagueFlagImage, flagUrl)

            val leagueKey = "${league.name}_${league.country}"
            val isSelected = selectedLeagues.any { it.contains(league.name) && it.contains(league.country) }
            leagueCheckbox.isChecked = isSelected

            updateSelectionUI(isSelected)

            cardView.setOnClickListener {
                onLeagueClick(league)
                addClickAnimation()
            }

            leagueCheckbox.setOnClickListener {
                onLeagueClick(league)
            }
        }

        private fun getCountryFlagUrl(country: String, league: League): String? {
            return when (country.lowercase()) {
                "england" -> "https://flagcdn.com/w320/gb-eng.png"
                "spain" -> "https://flagcdn.com/w320/es.png"
                "germany" -> "https://flagcdn.com/w320/de.png"
                "italy" -> "https://flagcdn.com/w320/it.png"
                "france" -> "https://flagcdn.com/w320/fr.png"
                "turkey" -> "https://flagcdn.com/w320/tr.png"
                "netherlands" -> "https://flagcdn.com/w320/nl.png"
                "portugal" -> "https://flagcdn.com/w320/pt.png"
                "azerbaijan" -> "https://flagcdn.com/w320/az.png"
                "uefa" -> "https://img.uefa.com/imgml/uefacom/uefa/genericimages/logo_generic_90x90.png"
                "fifa" -> "https://img.fifa.com/assets/img/about-fifa/fifa-logo.svg"
                "brazil" -> "https://flagcdn.com/w320/br.png"
                "mexico" -> "https://flagcdn.com/w320/mx.png"
                "usa" -> "https://flagcdn.com/w320/us.png"
                "greece" -> "https://flagcdn.com/w320/gr.png"
                "belgium" -> "https://flagcdn.com/w320/be.png"
                "denmark" -> "https://flagcdn.com/w320/dk.png"
                "norway" -> "https://flagcdn.com/w320/no.png"
                "sweden" -> "https://flagcdn.com/w320/se.png"
                else -> league.logoUrl.ifEmpty { "https://flagcdn.com/w320/xx.png" }
            }
        }

        private fun loadCountryFlag(imageView: ImageView, url: String?) {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_competition)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000)

            if (!url.isNullOrEmpty()) {
                Glide.with(imageView.context)
                    .load(url)
                    .apply(requestOptions)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_competition)
            }
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            if (isSelected) {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.filter_selected_bg)
                )

                selectedBorder.visibility = View.VISIBLE

                leagueName.setTextColor(
                    ContextCompat.getColor(itemView.context, android.R.color.white)
                )
                leagueCountry.setTextColor(
                    ContextCompat.getColor(itemView.context, android.R.color.white)
                )

                leagueCheckbox.buttonTintList = ContextCompat.getColorStateList(
                    itemView.context, android.R.color.holo_red_light
                )

                cardView.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .setDuration(150)
                    .withEndAction {
                        cardView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start()
                    }
                    .start()

            } else {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.card_background)
                )

                selectedBorder.visibility = View.GONE

                leagueName.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
                leagueCountry.setTextColor(
                    ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                )

                leagueCheckbox.buttonTintList = ContextCompat.getColorStateList(
                    itemView.context, android.R.color.darker_gray
                )
            }
        }

        private fun addClickAnimation() {
            cardView.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    cardView.animate()
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