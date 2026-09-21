package com.notepay.domain.repository

/** Persistence boundary for the on-device category-learning statistics. */
interface CategoryLearningStore {
    fun getString(key: String): String?

    fun getInt(key: String, defaultValue: Int = 0): Int

    fun getStringSet(key: String): Set<String>

    fun edit(block: CategoryLearningStoreEditor.() -> Unit)
}

interface CategoryLearningStoreEditor {
    fun putInt(key: String, value: Int)

    fun putString(key: String, value: String)

    fun putStringSet(key: String, value: Set<String>)
}
