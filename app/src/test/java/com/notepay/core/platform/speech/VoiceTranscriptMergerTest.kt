package com.notepay.core.platform.speech

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceTranscriptMergerTest {

    @Test
    fun `merge returns spoken text when base is empty`() {
        val merged = VoiceTranscriptMerger.merge(
            baseText = "",
            spokenSegment = "  an trua 45k  ",
        )
        assertThat(merged).isEqualTo("an trua 45k")
    }

    @Test
    fun `merge appends spoken segment with single space`() {
        val merged = VoiceTranscriptMerger.merge(
            baseText = "ca phe 30k   ",
            spokenSegment = "tra sua 55k",
        )
        assertThat(merged).isEqualTo("ca phe 30k tra sua 55k")
    }

    @Test
    fun `merge preserves base text when spoken segment is blank`() {
        val merged = VoiceTranscriptMerger.merge(
            baseText = "an sang 25k",
            spokenSegment = "   ",
        )
        assertThat(merged).isEqualTo("an sang 25k")
    }

    @Test
    fun `merge enforces max length cap`() {
        val merged = VoiceTranscriptMerger.merge(
            baseText = "a".repeat(15),
            spokenSegment = "b".repeat(15),
            maxLength = 20,
        )
        assertThat(merged.length).isEqualTo(20)
        assertThat(merged).isEqualTo("aaaaaaaaaaaaaaa bbbb")
    }
}
