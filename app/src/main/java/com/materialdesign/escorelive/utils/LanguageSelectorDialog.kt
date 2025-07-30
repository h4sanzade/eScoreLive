package com.materialdesign.escorelive.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.materialdesign.escorelive.R

class LanguageSelectorDialog(
    private val context: Context,
    private val localeManager: LocaleManager,
    private val onLanguageSelected: (String) -> Unit
) {

    fun show() {
        val languages = localeManager.getAvailableLanguages()
        val currentLanguage = localeManager.getLanguage(context)

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_language_selector, null)
        val listView = dialogView.findViewById<ListView>(R.id.languageListView)

        val adapter = LanguageAdapter(context, languages, currentLanguage)
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.language))
            .setView(dialogView)
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedLanguage = languages[position]
            onLanguageSelected(selectedLanguage.code)
            dialog.dismiss()
        }

        dialog.show()
    }

    private class LanguageAdapter(
        private val context: Context,
        private val languages: List<LanguageItem>,
        private val currentLanguage: String
    ) : BaseAdapter() {

        override fun getCount(): Int = languages.size

        override fun getItem(position: Int): LanguageItem = languages[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_language_selector, parent, false)

            val language = languages[position]

            val flagIcon = view.findViewById<TextView>(R.id.flagIcon)
            val languageName = view.findViewById<TextView>(R.id.languageName)
            val selectedIcon = view.findViewById<ImageView>(R.id.selectedIcon)

            flagIcon.text = language.flag
            languageName.text = language.name

            if (language.code == currentLanguage) {
                selectedIcon.visibility = View.VISIBLE
                selectedIcon.setImageResource(R.drawable.ic_check)
            } else {
                selectedIcon.visibility = View.GONE
            }

            return view
        }
    }
}