package com.notepay.ui.feature.transaction

import android.content.Context
import com.notepay.R
import com.notepay.domain.usecase.CategorySuggestionReason

object CategorySuggestionUiMapper {
    fun toText(context: Context, reason: CategorySuggestionReason): String = when (reason) {
        CategorySuggestionReason.History ->
            context.getString(R.string.category_suggestion_reason_history)
        is CategorySuggestionReason.Phrase ->
            context.getString(R.string.category_suggestion_reason_phrase, reason.phrase)
        CategorySuggestionReason.Learned ->
            context.getString(R.string.category_suggestion_reason_learned)
    }
}
