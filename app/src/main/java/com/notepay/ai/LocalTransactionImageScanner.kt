package com.notepay.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Result is intentionally limited to a draft amount; the original image is never persisted. */
data class LocalImageScanResult(
    val amountInput: String? = null,
    val message: String,
    val source: Source? = null,
) {
    enum class Source { VIET_QR, OCR }
}

/**
 * Reads a user-selected screenshot entirely on-device.
 * VietQR's EMV amount (tag 54) is preferred; otherwise ML Kit OCR selects a conservative amount
 * candidate and never uses account numbers or balances as a draft amount.
 */
@Singleton
class LocalTransactionImageScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scan(uri: Uri): LocalImageScanResult {
        val bitmap = decodeBitmap(uri) ?: return LocalImageScanResult(message = "Không thể đọc ảnh này. Hãy thử ảnh screenshot rõ hơn.")
        val vietQrAmount = bitmap?.let(::extractVietQrAmount)
        if (vietQrAmount != null) {
            return LocalImageScanResult(
                amountInput = vietQrAmount.toString(),
                message = "Đã đọc số tiền từ mã VietQR. Hãy kiểm tra trước khi lưu.",
                source = LocalImageScanResult.Source.VIET_QR,
            )
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val text = Tasks.await(recognizer.process(image))
            val reconstructedLines = reconstructHorizontalLines(text)
            val fullRawText = reconstructedLines.joinToString("\n")
            val payload = com.notepay.domain.ingestion.RawTransactionPayload(
                rawText = fullRawText,
                source = com.notepay.domain.ingestion.TransactionInputSource.OCR_SCREENSHOT
            )
            when (val result = com.notepay.domain.ingestion.TransactionAnalyzer.analyze(payload)) {
                is com.notepay.domain.ingestion.ParsedTransactionResult.Success -> {
                    val majorUnits = result.amount.amountInCents / 100L
                    LocalImageScanResult(
                        amountInput = majorUnits.toString(),
                        message = "Đã điền số tiền từ ảnh. Hãy kiểm tra trước khi lưu.",
                        source = LocalImageScanResult.Source.OCR,
                    )
                }
                is com.notepay.domain.ingestion.ParsedTransactionResult.Unrecognized -> {
                    // Fallback to extraction from candidates if raw string wasn't structured
                    val fallbackAmount = extractAmount(reconstructedLines)
                    if (fallbackAmount != null) {
                        LocalImageScanResult(
                            amountInput = fallbackAmount.toString(),
                            message = "Đã điền số tiền từ ảnh. Hãy kiểm tra trước khi lưu.",
                            source = LocalImageScanResult.Source.OCR,
                        )
                    } else {
                        LocalImageScanResult(message = "Không tìm thấy số tiền rõ ràng trong ảnh. Hãy chọn ảnh nét hơn.")
                    }
                }
            }
        } catch (_: Throwable) {
            LocalImageScanResult(message = "Không thể đọc ảnh này. Hãy thử ảnh screenshot rõ hơn.")
        } finally {
            recognizer.close()
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? { return try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (width / sampleSize > MAX_IMAGE_SIDE || height / sampleSize > MAX_IMAGE_SIDE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (_: Throwable) {
        null
    } catch (_: Throwable) {
        null
    } }

    private fun extractVietQrAmount(bitmap: Bitmap): Long? = try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val payload = QRCodeReader()
            .decode(BinaryBitmap(HybridBinarizer(source)))
            .text
        parseEmvAmount(payload)
    } catch (_: Throwable) {
        null
    }

    private fun parseEmvAmount(payload: String): Long? {
        if (!payload.startsWith("000201") && !payload.startsWith("000202")) return null
        var index = 0
        while (index + 4 <= payload.length) {
            val tag = payload.substring(index, index + 2)
            val length = payload.substring(index + 2, index + 4).toIntOrNull() ?: return null
            val valueStart = index + 4
            val valueEnd = valueStart + length
            if (valueEnd > payload.length) return null
            if (tag == "54") {
                return payload.substring(valueStart, valueEnd)
                    .filter(Char::isDigit)
                    .toLongOrNull()
                    ?.takeIf { it > 0L }
            }
            index = valueEnd
        }
        return null
    }

    private fun extractAmount(lines: List<String>): Long? {
        val candidates = lines.flatMap { line ->
            amountPattern.findAll(line).mapNotNull { match ->
                val raw = match.value
                val value = raw.filter(Char::isDigit).toLongOrNull() ?: return@mapNotNull null
                val normalizedLine = normalize(line)
                val hasCurrency = currencyPattern.containsMatchIn(raw)
                val hasStructure = raw.any { it == '.' || it == ',' || it == ' ' }
                val hasAmountContext = amountMarkers.any { it in normalizedLine }
                if (value <= 0L || value > 9_999_999_999L || (!hasCurrency && !hasStructure && !hasAmountContext)) {
                    return@mapNotNull null
                }
                var score = 0
                if (hasAmountContext) score += 5
                if (hasCurrency) score += 3
                if (hasStructure) score += 1
                if (balanceMarkers.any { it in normalizedLine }) score -= 8
                if (accountMarkers.any { it in normalizedLine }) score -= 6
                AmountCandidate(value, score)
            }
        }
        return candidates
            .filter { it.score >= 2 }
            .maxWithOrNull(compareBy<AmountCandidate> { it.score }.thenBy { it.value })
            ?.value
    }

    private fun reconstructHorizontalLines(text: com.google.mlkit.vision.text.Text): List<String> {
        val linesWithRect = text.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val rect = line.boundingBox ?: return@mapNotNull null
                LineWithBoundingBox(line.text, rect)
            }
        }
        if (linesWithRect.isEmpty()) return emptyList()

        // Sắp xếp các dòng từ trên xuống dưới theo tọa độ y (top)
        val sortedByTop = linesWithRect.sortedBy { it.rect.top }
        
        val rows = mutableListOf<MutableList<LineWithBoundingBox>>()
        
        for (line in sortedByTop) {
            var placed = false
            for (row in rows) {
                val representative = row.first()
                val repHeight = representative.rect.bottom - representative.rect.top
                val lineHeight = line.rect.bottom - line.rect.top
                val minHeight = kotlin.math.min(repHeight, lineHeight)
                
                val repCenter = (representative.rect.top + representative.rect.bottom) / 2
                val lineCenter = (line.rect.top + line.rect.bottom) / 2
                val centerDiff = kotlin.math.abs(repCenter - lineCenter)
                val threshold = minHeight * 0.5f // Lệch tâm không quá 50% chiều cao dòng
                
                if (centerDiff < threshold) {
                    row.add(line)
                    placed = true
                    break
                }
            }
            if (!placed) {
                rows.add(mutableListOf(line))
            }
        }
        
        // Với mỗi dòng hàng ngang được ghép, ta sắp xếp các chữ từ trái qua phải (trục x)
        return rows.map { row ->
            row.sortedBy { it.rect.left }
                .joinToString(" ") { it.text }
        }
    }

    private data class LineWithBoundingBox(
        val text: String,
        val rect: Rect
    )

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)

    private data class AmountCandidate(val value: Long, val score: Int)

    private companion object {
        const val MAX_IMAGE_SIDE = 2048
        val currencyPattern = Regex("""(?i)(?:đ|vnđ|vnd)""")
        val amountPattern = Regex(
            """(?<!\d)(?:\d{1,3}(?:[.,\s]\d{3})+|\d{4,})(?:\s*(?:đ|vnđ|vnd))?(?!\d)""",
            RegexOption.IGNORE_CASE,
        )
        val amountMarkers = setOf("số tiền", "so tien", "thanh toán", "thanh toan", "giao dịch", "giao dich", "chuyển khoản", "chuyen khoan", "tổng tiền", "tong tien", "amount")
        val currencyMarkers = setOf("đ", "vnđ", "vnd")
        val balanceMarkers = setOf("số dư", "so du", "balance")
        val accountMarkers = setOf("tài khoản", "tai khoan", "stk", "account", "mã giao dịch", "ma giao dich")
    }
}
