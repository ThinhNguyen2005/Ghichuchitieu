package com.notepay.data.preferences

import android.content.Context
import com.notepay.domain.repository.CategoryLearningStore
import com.notepay.domain.repository.CategoryLearningStoreEditor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryLearningStoreImpl @Inject constructor(
    @ApplicationContext context: Context,
) : CategoryLearningStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun getInt(key: String, defaultValue: Int): Int = preferences.getInt(key, defaultValue)

    override fun getStringSet(key: String): Set<String> =
        preferences.getStringSet(key, emptySet()).orEmpty()

    override fun edit(block: CategoryLearningStoreEditor.() -> Unit) {
        val editor = preferences.edit()
        SharedPreferencesEditor(editor).apply(block)
        editor.apply()
    }

    private class SharedPreferencesEditor(
        private val editor: android.content.SharedPreferences.Editor,
    ) : CategoryLearningStoreEditor {
        override fun putInt(key: String, value: Int) {
            editor.putInt(key, value)
        }

        override fun putString(key: String, value: String) {
            editor.putString(key, value)
        }

        override fun putStringSet(key: String, value: Set<String>) {
            editor.putStringSet(key, value)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "notepay_category_habits"
    }
}
