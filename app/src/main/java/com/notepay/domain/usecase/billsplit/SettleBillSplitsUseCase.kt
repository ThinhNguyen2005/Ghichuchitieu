package com.notepay.domain.usecase.billsplit

import com.notepay.domain.repository.BillSplitRepository
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant
import javax.inject.Inject

/**
 * Đánh dấu toàn bộ khoản chia hoá đơn của một người là đã thanh toán.
 *
 * Trước đây ViewModel tự đọc từng bản ghi rồi gọi `markAsPaid` với `Clock.System.now()` riêng
 * cho mỗi khoản, nên các khoản cùng một lần thu tiền lại có mốc thời gian lệch nhau vài ms.
 * Use case gom về một mốc duy nhất, bỏ qua khoản đã trả và trả về số khoản thực sự được cập nhật.
 */
class SettleBillSplitsUseCase @Inject constructor(
    private val billSplitRepository: BillSplitRepository,
) {
    suspend operator fun invoke(
        splitIds: List<Long>,
        paidAt: Instant = Clock.System.now(),
    ): Result<Int> = try {
        val unpaid = splitIds.distinct().mapNotNull { splitId ->
            billSplitRepository.getById(splitId)?.takeUnless { it.isPaid }
        }
        unpaid.forEach { split ->
            billSplitRepository.markAsPaid(split.id, paidAt)
        }
        Result.success(unpaid.size)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
