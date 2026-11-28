package com.notepay.core.designsystem.feedback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.SnackbarDuration
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UiFeedbackTest {

    @Test
    fun `resolveSnackbarDuration keeps short when no action is present`() {
        assertThat(FeedbackDuration.Short.resolveSnackbarDuration(hasAction = false))
            .isEqualTo(SnackbarDuration.Short)
    }

    @Test
    fun `resolveSnackbarDuration promotes short to long when action is present`() {
        assertThat(FeedbackDuration.Short.resolveSnackbarDuration(hasAction = true))
            .isEqualTo(SnackbarDuration.Long)
    }

    @Test
    fun `resolveSnackbarDuration preserves long and indefinite regardless of action`() {
        assertThat(FeedbackDuration.Long.resolveSnackbarDuration(hasAction = false))
            .isEqualTo(SnackbarDuration.Long)
        assertThat(FeedbackDuration.Long.resolveSnackbarDuration(hasAction = true))
            .isEqualTo(SnackbarDuration.Long)
        assertThat(FeedbackDuration.Indefinite.resolveSnackbarDuration(hasAction = false))
            .isEqualTo(SnackbarDuration.Indefinite)
        assertThat(FeedbackDuration.Indefinite.resolveSnackbarDuration(hasAction = true))
            .isEqualTo(SnackbarDuration.Indefinite)
    }

    @Test
    fun `all feedback types map to distinct semantic icons`() {
        val iconsByType = FeedbackType.entries.associateWith { it.feedbackIcon() }

        assertThat(iconsByType[FeedbackType.Success]).isEqualTo(Icons.Rounded.CheckCircle)
        assertThat(iconsByType[FeedbackType.Error]).isEqualTo(Icons.Rounded.ErrorOutline)
        assertThat(iconsByType[FeedbackType.Warning]).isEqualTo(Icons.Rounded.WarningAmber)
        assertThat(iconsByType[FeedbackType.Info]).isEqualTo(Icons.Rounded.Info)
        assertThat(iconsByType[FeedbackType.Pending]).isEqualTo(Icons.Rounded.Sync)
        assertThat(iconsByType[FeedbackType.Offline]).isEqualTo(Icons.Rounded.CloudOff)
        assertThat(iconsByType.values.toSet()).hasSize(FeedbackType.entries.size)
    }
}
