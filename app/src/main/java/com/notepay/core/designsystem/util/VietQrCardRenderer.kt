package com.notepay.core.designsystem.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.notepay.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Immutable
data class VietQrCardData(
    val emvPayload: String,
    val bankShortName: String,
    val bankFullName: String,
    val bankLogoUrl: String?,
    val accountName: String,
    val accountNumber: String,
    val amountFormatted: String? = null,
    val memo: String? = null,
)

/**
 * Renders a self-contained branded VietQR card Bitmap (100% offline, with center VietQR emblem,
 * NAPAS 247 trust bar, offline bank logo asset, and account/amount metadata) and exports it as a
 * sharable PNG via FileProvider.
 */
object VietQrCardRenderer {
    internal val NAVY = "#173A6A".toColorInt()
    internal val VIETQR_RED = "#E31E24".toColorInt()
    internal val VIETQR_RED_FOLD = "#9E1B1F".toColorInt()
    internal val NAPAS_CYAN = "#009FE3".toColorInt()
    internal val NAPAS_GREEN = "#8DC63F".toColorInt()
    internal val INFO_BG = "#F3F6FA".toColorInt()
    internal val DIVIDER_GRAY = "#CBD5E1".toColorInt()

    private const val SHARED_IMAGES_DIR = "shared_images"
    private const val MAX_CACHED_QR_FILES = 10

    fun renderCardBitmap(
        context: Context,
        data: VietQrCardData,
        cardWidthPx: Int = 1080,
    ): Bitmap {
        val metrics = computeCardMetrics(context, data, cardWidthPx)
        val rawQr = VietQrGenerator.generateLocalQrBitmap(
            content = data.emvPayload,
            width = metrics.qrSize,
            height = metrics.qrSize,
            errorCorrectionLevel = ErrorCorrectionLevel.H,
        )
        val bankLogoBitmap = decodeOfflineBankLogo(context, data.bankLogoUrl)

        val output = createBitmap(cardWidthPx, metrics.totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)

        drawTopTriAccent(canvas, cardWidthPx.toFloat(), metrics.topAccentHeight)

        var y = metrics.topAccentHeight + metrics.pad * 0.65f
        drawVietQrWordmark(canvas, cardWidthPx / 2f, y, metrics.headerHeight, cardWidthPx)
        y += metrics.headerHeight + metrics.sectionGap * 0.7f

        drawQrWithEmblem(canvas, rawQr, cardWidthPx, y, metrics)
        rawQr.recycle()
        y += metrics.qrFrameSize + metrics.sectionGap

        drawTrustBar(
            canvas = canvas,
            centerX = cardWidthPx / 2f,
            topY = y,
            rowHeight = metrics.trustRowHeight,
            maxWidth = metrics.innerWidth,
            bankLogo = bankLogoBitmap,
            bankShortName = data.bankShortName,
        )
        bankLogoBitmap?.recycle()
        y += metrics.trustRowHeight + metrics.sectionGap * 0.85f

        drawCardFooterAndInfo(canvas, cardWidthPx, y, metrics)
        return output
    }

    fun saveCardPngToCache(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String = "vietqr",
    ): Uri {
        val dir = File(context.cacheDir, SHARED_IMAGES_DIR).apply {
            if (!exists()) mkdirs()
        }
        pruneOldSharedFiles(dir)
        val sanitized = filePrefix.replace(Regex("[^A-Za-z0-9_-]"), "_").take(24).ifBlank { "vietqr" }
        val file = File(dir, "${sanitized}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun buildShareImageIntent(
        imageUri: Uri,
        chooserTitle: String,
    ): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            clipData = ClipData.newRawUri("", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun shareVietQrCardImage(
        context: Context,
        data: VietQrCardData,
        chooserTitle: String,
        filePrefix: String = "vietqr",
    ): Result<Unit> = runCatching {
        val uri = withContext(Dispatchers.Default) {
            val bitmap = renderCardBitmap(context, data)
            try {
                withContext(Dispatchers.IO) {
                    saveCardPngToCache(context, bitmap, filePrefix)
                }
            } finally {
                bitmap.recycle()
            }
        }
        withContext(Dispatchers.Main) {
            context.startActivity(buildShareImageIntent(uri, chooserTitle))
        }
    }

    private fun decodeOfflineBankLogo(context: Context, bankLogoUrl: String?): Bitmap? {
        if (bankLogoUrl.isNullOrBlank()) return null
        val assetPath = bankLogoUrl.removePrefix("file:///android_asset/")
        if (assetPath == bankLogoUrl && bankLogoUrl.startsWith("http")) return null
        return runCatching {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    private fun pruneOldSharedFiles(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_CACHED_QR_FILES).forEach { runCatching { it.delete() } }
    }
}

private data class VietQrCardMetrics(
    val pad: Float,
    val innerWidth: Float,
    val qrSize: Int,
    val qrFramePad: Float,
    val qrFrameSize: Float,
    val topAccentHeight: Float,
    val headerHeight: Float,
    val trustRowHeight: Float,
    val sectionGap: Float,
    val lineGap: Float,
    val infoBoxPaddingV: Float,
    val infoBoxHeight: Float,
    val footerLayouts: List<StaticLayout>,
    val amountLayout: StaticLayout?,
    val memoLayout: StaticLayout?,
    val totalHeight: Int,
)

private fun computeCardMetrics(
    context: Context,
    data: VietQrCardData,
    cardWidthPx: Int,
): VietQrCardMetrics {
    val pad = cardWidthPx * 0.08f
    val innerWidth = cardWidthPx - pad * 2f
    val qrSize = (cardWidthPx * 0.64f).toInt()
    val qrFramePad = cardWidthPx * 0.022f
    val qrFrameSize = qrSize + qrFramePad * 2f

    val holderText = data.accountName.trim().takeIf { it.isNotBlank() }?.let {
        context.getString(R.string.vietqr_card_account_holder, it.uppercase())
    }
    val accountNumberText = data.accountNumber.trim().takeIf { it.isNotBlank() }?.let {
        context.getString(R.string.vietqr_card_account_number, it)
    }
    val bankFullNameText = data.bankFullName.trim().takeIf { it.isNotBlank() }
    val amountText = data.amountFormatted?.trim()?.takeIf { it.isNotBlank() }?.let {
        context.getString(R.string.vietqr_card_amount_label, it)
    }
    val memoText = data.memo?.trim()?.takeIf { it.isNotBlank() }?.let {
        context.getString(R.string.vietqr_card_memo_label, it)
    }

    val textSizeMedium = cardWidthPx * 0.036f
    val textSizeSmall = cardWidthPx * 0.033f
    val lineGap = cardWidthPx * 0.016f
    val regularPaint = createCenterTextPaint(textSizeMedium, VietQrCardRenderer.NAVY, bold = false)
    val boldPaint = createCenterTextPaint(textSizeMedium, VietQrCardRenderer.NAVY, bold = true)
    val infoBoldPaint = createCenterTextPaint(textSizeSmall, VietQrCardRenderer.NAVY, bold = true)
    val infoRegularPaint = createCenterTextPaint(textSizeSmall, VietQrCardRenderer.NAVY, bold = false)

    val footerLayouts = listOfNotNull(
        holderText?.let { buildCenteredLayout(it, regularPaint, innerWidth.toInt()) },
        accountNumberText?.let { buildCenteredLayout(it, boldPaint, innerWidth.toInt()) },
        bankFullNameText?.let { buildCenteredLayout(it, regularPaint, innerWidth.toInt()) },
    )
    val amountLayout = amountText?.let { buildCenteredLayout(it, infoBoldPaint, (innerWidth * 0.9f).toInt()) }
    val memoLayout = memoText?.let { buildCenteredLayout(it, infoRegularPaint, (innerWidth * 0.9f).toInt()) }

    val topAccentHeight = cardWidthPx * 0.014f
    val headerHeight = cardWidthPx * 0.145f
    val trustRowHeight = cardWidthPx * 0.088f
    val sectionGap = cardWidthPx * 0.038f
    val infoBoxPaddingV = cardWidthPx * 0.026f
    val hasInfoBox = amountLayout != null || memoLayout != null
    val infoBoxHeight = if (hasInfoBox) {
        infoBoxPaddingV * 2f +
            (amountLayout?.height ?: 0) +
            (if (amountLayout != null && memoLayout != null) lineGap else 0f) +
            (memoLayout?.height ?: 0)
    } else {
        0f
    }
    val footerHeight = footerLayouts.sumOf { it.height }.toFloat() +
        (footerLayouts.size - 1).coerceAtLeast(0) * lineGap

    val totalHeight = (
        topAccentHeight +
            pad * 0.65f +
            headerHeight +
            sectionGap * 0.7f +
            qrFrameSize +
            sectionGap +
            trustRowHeight +
            sectionGap * 0.85f +
            footerHeight +
            (if (hasInfoBox) sectionGap * 0.75f + infoBoxHeight else 0f) +
            pad * 0.85f
        ).toInt()

    return VietQrCardMetrics(
        pad = pad,
        innerWidth = innerWidth,
        qrSize = qrSize,
        qrFramePad = qrFramePad,
        qrFrameSize = qrFrameSize,
        topAccentHeight = topAccentHeight,
        headerHeight = headerHeight,
        trustRowHeight = trustRowHeight,
        sectionGap = sectionGap,
        lineGap = lineGap,
        infoBoxPaddingV = infoBoxPaddingV,
        infoBoxHeight = infoBoxHeight,
        footerLayouts = footerLayouts,
        amountLayout = amountLayout,
        memoLayout = memoLayout,
        totalHeight = totalHeight,
    )
}

private fun drawQrWithEmblem(
    canvas: Canvas,
    rawQr: Bitmap,
    cardWidthPx: Int,
    topY: Float,
    metrics: VietQrCardMetrics,
) {
    val qrLeft = (cardWidthPx - metrics.qrFrameSize) / 2f
    val frameRect = RectF(qrLeft, topY, qrLeft + metrics.qrFrameSize, topY + metrics.qrFrameSize)
    val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.NAVY
        style = Paint.Style.STROKE
        strokeWidth = (cardWidthPx * 0.0045f).coerceAtLeast(2f)
    }
    canvas.drawRect(frameRect, framePaint)

    val qrDest = Rect(
        (qrLeft + metrics.qrFramePad).toInt(),
        (topY + metrics.qrFramePad).toInt(),
        (qrLeft + metrics.qrFramePad + metrics.qrSize).toInt(),
        (topY + metrics.qrFramePad + metrics.qrSize).toInt(),
    )
    canvas.drawBitmap(rawQr, null, qrDest, Paint(Paint.FILTER_BITMAP_FLAG))
    drawCenterQrEmblem(canvas, qrDest.exactCenterX(), qrDest.exactCenterY(), metrics.qrSize * 0.185f)
}

private fun drawCardFooterAndInfo(
    canvas: Canvas,
    cardWidthPx: Int,
    startY: Float,
    metrics: VietQrCardMetrics,
) {
    var y = startY
    metrics.footerLayouts.forEach { layout ->
        canvas.save()
        canvas.translate(metrics.pad, y)
        layout.draw(canvas)
        canvas.restore()
        y += layout.height + metrics.lineGap
    }

    if (metrics.amountLayout != null || metrics.memoLayout != null) {
        y += metrics.sectionGap * 0.35f
        val boxRect = RectF(metrics.pad, y, cardWidthPx - metrics.pad, y + metrics.infoBoxHeight)
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VietQrCardRenderer.INFO_BG }
        val radius = cardWidthPx * 0.025f
        canvas.drawRoundRect(boxRect, radius, radius, boxPaint)

        var infoY = y + metrics.infoBoxPaddingV
        val infoX = metrics.pad + metrics.innerWidth * 0.05f
        metrics.amountLayout?.let {
            canvas.save()
            canvas.translate(infoX, infoY)
            it.draw(canvas)
            canvas.restore()
            infoY += it.height + metrics.lineGap
        }
        metrics.memoLayout?.let {
            canvas.save()
            canvas.translate(infoX, infoY)
            it.draw(canvas)
            canvas.restore()
        }
    }
}

private fun drawTopTriAccent(canvas: Canvas, width: Float, height: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = VietQrCardRenderer.NAVY
    canvas.drawRect(0f, 0f, width * 0.66f, height, paint)
    paint.color = VietQrCardRenderer.NAPAS_CYAN
    canvas.drawRect(width * 0.66f, 0f, width * 0.85f, height, paint)
    paint.color = VietQrCardRenderer.NAPAS_GREEN
    canvas.drawRect(width * 0.85f, 0f, width, height, paint)
}

private fun drawVietQrWordmark(
    canvas: Canvas,
    centerX: Float,
    topY: Float,
    height: Float,
    cardWidthPx: Int,
) {
    val textSize = height * 0.72f
    val ietPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.VIETQR_RED
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        this.textSize = textSize
        letterSpacing = 0.02f
    }
    val qrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.NAVY
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        this.textSize = textSize
        letterSpacing = 0.02f
    }

    val emblemSize = height * 0.92f
    val emblemGap = cardWidthPx * 0.012f
    val ietText = "IET"
    val qrText = "QR"
    val ietW = ietPaint.measureText(ietText)
    val qrW = qrPaint.measureText(qrText)
    val totalW = emblemSize + emblemGap + ietW + qrW
    val startX = centerX - totalW / 2f

    drawVRibbonEmblem(canvas, startX + emblemSize / 2f, topY + height / 2f, emblemSize)

    val fm = ietPaint.fontMetrics
    val baseline = topY + height / 2f - (fm.ascent + fm.descent) / 2f + height * 0.05f
    val textX = startX + emblemSize + emblemGap
    canvas.drawText(ietText, textX, baseline, ietPaint)
    canvas.drawText(qrText, textX + ietW, baseline, qrPaint)
}

private fun drawCenterQrEmblem(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    badgeSize: Float,
) {
    val half = badgeSize / 2f
    val badgeRect = RectF(centerX - half, centerY - half, centerX + half, centerY + half)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawRoundRect(badgeRect, badgeSize * 0.24f, badgeSize * 0.24f, bgPaint)
    drawVRibbonEmblem(canvas, centerX, centerY, badgeSize * 0.78f)
}

private fun drawVRibbonEmblem(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    size: Float,
) {
    val r = size * 0.44f
    val stroke = size * 0.24f
    val leftX = centerX - r * 0.72f
    val rightX = centerX + r * 0.72f
    val topY = centerY - r * 0.62f
    val bottomY = centerY + r * 0.70f

    val leftArmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.VIETQR_RED_FOLD
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val mainArmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.VIETQR_RED
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val leftPath = Path().apply {
        moveTo(leftX, topY)
        lineTo(centerX, bottomY)
    }
    canvas.drawPath(leftPath, leftArmPaint)

    val rightPath = Path().apply {
        moveTo(centerX, bottomY)
        lineTo(rightX, topY)
    }
    canvas.drawPath(rightPath, mainArmPaint)
    canvas.drawCircle(
        centerX,
        bottomY,
        stroke * 0.48f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VietQrCardRenderer.VIETQR_RED },
    )
}

private fun drawTrustBar(
    canvas: Canvas,
    centerX: Float,
    topY: Float,
    rowHeight: Float,
    maxWidth: Float,
    bankLogo: Bitmap?,
    bankShortName: String,
) {
    val dividerH = rowHeight * 0.72f
    val dividerTop = topY + (rowHeight - dividerH) / 2f
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.DIVIDER_GRAY
        strokeWidth = 3f
    }
    canvas.drawLine(centerX, dividerTop, centerX, dividerTop + dividerH, dividerPaint)

    val sideGap = maxWidth * 0.055f
    drawNapas247(canvas, centerX - sideGap, topY + rowHeight / 2f, rowHeight * 0.48f)

    val rightAnchorLeft = centerX + sideGap
    val rightSlotWidth = (maxWidth / 2f) - sideGap
    val targetH = rowHeight * 0.74f
    if (bankLogo != null && bankLogo.height > 0 && bankLogo.width > 0) {
        val scale = minOf(rightSlotWidth / bankLogo.width.toFloat(), targetH / bankLogo.height.toFloat())
        val drawW = bankLogo.width * scale
        val drawH = bankLogo.height * scale
        val top = topY + (rowHeight - drawH) / 2f
        val dest = RectF(rightAnchorLeft, top, rightAnchorLeft + drawW, top + drawH)
        canvas.drawBitmap(bankLogo, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    } else if (bankShortName.isNotBlank()) {
        val bankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = VietQrCardRenderer.NAVY
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = rowHeight * 0.46f
        }
        val fm = bankPaint.fontMetrics
        val baseline = topY + rowHeight / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(bankShortName, rightAnchorLeft, baseline, bankPaint)
    }
}

private fun drawNapas247(
    canvas: Canvas,
    anchorRightX: Float,
    centerY: Float,
    textSize: Float,
) {
    val napasPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.NAVY
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        this.textSize = textSize
    }
    val num24Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.NAPAS_CYAN
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        this.textSize = textSize
    }
    val num7Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VietQrCardRenderer.NAPAS_GREEN
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        this.textSize = textSize
    }
    val p1 = "napas "
    val p2 = "24"
    val p3 = "7"
    val w1 = napasPaint.measureText(p1)
    val w2 = num24Paint.measureText(p2)
    val w3 = num7Paint.measureText(p3)
    val startX = anchorRightX - (w1 + w2 + w3)
    val fm = napasPaint.fontMetrics
    val baseline = centerY - (fm.ascent + fm.descent) / 2f
    canvas.drawText(p1, startX, baseline, napasPaint)
    canvas.drawText(p2, startX + w1, baseline, num24Paint)
    canvas.drawText(p3, startX + w1 + w2, baseline, num7Paint)
}

private fun createCenterTextPaint(sizePx: Float, color: Int, bold: Boolean): TextPaint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = sizePx
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

private fun buildCenteredLayout(text: String, paint: TextPaint, widthPx: Int): StaticLayout =
    StaticLayout.Builder
        .obtain(text, 0, text.length, paint, widthPx.coerceAtLeast(1))
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, 1.05f)
        .setIncludePad(false)
        .build()
