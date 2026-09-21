package com.notepay.ui.feature.backup

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.notepay.R
import com.notepay.data.backup.DataExporter
import com.notepay.data.backup.DataImporter
import com.notepay.MainDispatcherRule
import com.notepay.ui.feedback.FeedbackType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.every
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class BackupRestoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val exporter = mockk<DataExporter>()
    private val importer = mockk<DataImporter>()

    @Test
    fun `import success is emitted as one shot feedback`() = runTest {
        every { context.getString(R.string.backup_restore_success) } returns "Khôi phục thành công"
        coEvery { importer.readFromFile(any()) } returns "{}"
        coEvery { importer.importFromJson("{}") } returns Unit

        val viewModel = BackupRestoreViewModel(exporter, importer, context)
        val feedback = async(start = CoroutineStart.UNDISPATCHED) { viewModel.feedback.first() }

        viewModel.importFromFile(mockk<Uri>())

        assertThat(feedback.await()).isEqualTo(
            com.notepay.ui.feedback.UiFeedback(
                message = "Khôi phục thành công",
                type = FeedbackType.Success,
            ),
        )
        assertThat(viewModel.state.value.isImporting).isFalse()
    }

    @Test
    fun `import failure is emitted as one shot error feedback`() = runTest {
        every { context.getString(R.string.backup_import_error_format, "bad file") } returns "Không thể khôi phục: bad file"
        coEvery { importer.readFromFile(any()) } throws IllegalStateException("bad file")

        val viewModel = BackupRestoreViewModel(exporter, importer, context)
        val feedback = async(start = CoroutineStart.UNDISPATCHED) { viewModel.feedback.first() }

        viewModel.importFromFile(mockk<Uri>())

        assertThat(feedback.await()).isEqualTo(
            com.notepay.ui.feedback.UiFeedback(
                message = "Không thể khôi phục: bad file",
                type = FeedbackType.Error,
            ),
        )
        assertThat(viewModel.state.value.isImporting).isFalse()
    }
}
