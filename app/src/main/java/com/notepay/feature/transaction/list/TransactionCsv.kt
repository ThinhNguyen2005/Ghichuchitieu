package com.notepay.feature.transaction.list

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.IOException
import java.util.Locale
import kotlin.time.Clock

/** MIME type của tệp xuất — dùng chung cho launcher SAF và phần ghi tệp. */
internal const val CsvMimeType = "text/csv"

private const val Tag = "TransactionCsv"

/**
 * BOM UTF-8. Excel trên Windows chỉ nhận ra tệp là UTF-8 khi có BOM; thiếu nó thì mọi dấu
 * tiếng Việt trong tên danh mục và ghi chú sẽ thành ký tự rác.
 */
private const val Utf8Bom = "\uFEFF"

/**
 * Dựng nội dung CSV cho đúng danh sách giao dịch người dùng đang nhìn thấy.
 *
 * Hàm thuần: không đọc cơ sở dữ liệu, không phụ thuộc Android, nên kiểm thử được bằng JUnit.
 * Thứ tự dòng luôn mới nhất trước, khớp với thứ tự sổ trên màn hình.
 *
 * @param header dòng tiêu đề đã dịch, ví dụ "Ngày,Giờ,Danh mục,Ví,Loại,Số tiền,Ghi chú".
 * @param typeLabels nhãn Thu/Chi đã dịch theo từng loại giao dịch.
 * @param transferLabel nhãn cho giao dịch chuyển khoản nội bộ, dùng thay cho Thu/Chi.
 */
internal fun buildTransactionsCsv(
    transactions: List<Transaction>,
    walletsMap: Map<Long, String>,
    header: String,
    typeLabels: Map<TransactionType, String>,
    transferLabel: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val builder = StringBuilder(Utf8Bom)
    builder.append(header).append('\n')

    transactions
        .sortedByDescending { it.occurredAt }
        .forEach { tx ->
            val local = tx.occurredAt.toLocalDateTime(zone)
            val amount = tx.amount.amountInCents / 100L
            val walletName = walletsMap[tx.walletId].orEmpty()
            val typeLabel = if (tx.isInternalTransfer) transferLabel else typeLabels.getValue(tx.type)

            builder.append(csvField(isoDate(local.year, local.month.ordinal + 1, local.day)))
            builder.append(',')
            builder.append(csvField(String.format(Locale.ROOT, "%02d:%02d", local.hour, local.minute)))
            builder.append(',')
            builder.append(csvField(tx.category.displayName))
            builder.append(',')
            builder.append(csvField(walletName))
            builder.append(',')
            builder.append(csvField(typeLabel))
            builder.append(',')
            builder.append(amount.toString())
            builder.append(',')
            builder.append(csvField(tx.note.trim()))
            builder.append('\n')
        }

    return builder.toString()
}

/** Ngày dạng `yyyy-MM-dd` để Excel sắp xếp được mà không phụ thuộc locale máy. */
private fun isoDate(year: Int, month: Int, day: Int): String =
    String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)

/**
 * Bọc một ô trong dấu nháy kép khi nội dung có ký tự phân tách, xuống dòng hoặc chính dấu nháy,
 * và nhân đôi dấu nháy bên trong — đúng RFC 4180. Không có bước này thì một ghi chú chứa dấu
 * phẩy sẽ làm lệch toàn bộ các cột phía sau.
 */
private fun csvField(value: String): String {
    val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }
    if (!needsQuoting) return value
    return '"' + value.replace("\"", "\"\"") + '"'
}

/** Phần tên tệp gợi ý, dạng `20261029`. */
internal fun csvFileNameStem(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val today = Clock.System.now().toLocalDateTime(zone).date
    return isoDate(today.year, today.month.ordinal + 1, today.day).replace("-", "")
}

/**
 * Ghi nội dung CSV vào URI mà người dùng chọn qua SAF. Chạy trên [Dispatchers.IO] vì đây là
 * I/O đĩa; trả về false thay vì ném ra ngoài để tầng UI hiện được thông báo thân thiện.
 */
internal suspend fun writeCsvToUri(
    resolver: ContentResolver,
    uri: Uri,
    content: String,
): Boolean = withContext(Dispatchers.IO) {
    try {
        val stream = resolver.openOutputStream(uri) ?: return@withContext false
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: IOException) {
        Log.w(Tag, "Không ghi được tệp CSV", e)
        false
    } catch (e: SecurityException) {
        Log.w(Tag, "Không có quyền ghi URI đã chọn", e)
        false
    }
}
