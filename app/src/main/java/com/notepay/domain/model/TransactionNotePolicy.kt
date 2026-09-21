package com.notepay.domain.model

object TransactionNotePolicy {
    private const val ELLIPSIS = "..."

    fun truncate(note: String): String = if (note.length <= Transaction.MAX_NOTE_LENGTH) {
        note
    } else {
        note.take(Transaction.MAX_NOTE_LENGTH - ELLIPSIS.length) + ELLIPSIS
    }
}
