package com.notepay.core.platform.speech

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceTranscriptPostProcessorTest {

    @Test
    fun `normalizeSttArtifacts fixes misheard ca, dong currency, and million shorthand`() {
        assertThat(VoiceTranscriptPostProcessor.normalizeSttArtifacts("ăn trưa 45 ca"))
            .isEqualTo("ăn trưa 45k")
        assertThat(VoiceTranscriptPostProcessor.normalizeSttArtifacts("trà sữa 45.000₫"))
            .isEqualTo("trà sữa 45.000đ")
        assertThat(VoiceTranscriptPostProcessor.normalizeSttArtifacts("cơm trưa 45 nghìn đồng"))
            .isEqualTo("cơm trưa 45 nghìn")
        assertThat(VoiceTranscriptPostProcessor.normalizeSttArtifacts("mua tủ lạnh 2 triệu 500"))
            .isEqualTo("mua tủ lạnh 2 triệu 500 nghìn")
    }

    @Test
    fun `normalizeSttArtifacts does not corrupt single digit shift phrases`() {
        assertThat(VoiceTranscriptPostProcessor.normalizeSttArtifacts("làm 1 ca trực"))
            .isEqualTo("làm 1 ca trực")
        assertThat(VoiceTranscriptPostProcessor.normalizeSttArtifacts("chạy 2 ca làm"))
            .isEqualTo("chạy 2 ca làm")
    }

    @Test
    fun `selectBestCandidate prefers candidate with parsed amount over unparsed rank zero`() {
        val selected = VoiceTranscriptPostProcessor.selectBestCandidate(
            candidates = listOf(
                "ăn trưa bốn năm",
                "ăn trưa 45 ca",
                "an trua 45k",
            ),
            confidences = floatArrayOf(0.8f, 0.78f, 0.7f),
        )
        assertThat(selected).isEqualTo("ăn trưa 45k")
    }

    @Test
    fun `selectBestCandidate prefers Vietnamese diacritics when amount counts tie`() {
        val selected = VoiceTranscriptPostProcessor.selectBestCandidate(
            candidates = listOf(
                "an pho 50k",
                "ăn phở 50k",
            ),
            confidences = floatArrayOf(0.85f, 0.85f),
        )
        assertThat(selected).isEqualTo("ăn phở 50k")
    }
}
