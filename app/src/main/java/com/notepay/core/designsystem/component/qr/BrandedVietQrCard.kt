package com.notepay.core.designsystem.component.qr

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietQrCardData
import com.notepay.core.designsystem.util.VietQrCardRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Displays the offline branded VietQR card (matching the exact exported PNG layout:
 * VietQR logo + center V badge + NAPAS 247 + bank logo + account holder & number).
 */
@Composable
fun BrandedVietQrCard(
    data: VietQrCardData,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cardBitmap by produceState<Bitmap?>(initialValue = null, data) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                VietQrCardRenderer.renderCardBitmap(context = context, data = data, cardWidthPx = 900)
            }.getOrNull()
        }
    }

    val shape = AppTheme.shapes.corner24
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = shape, elevated = false),
        shape = shape,
        color = Color.White,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        tonalElevation = 0.dp,
    ) {
        val bmp = cardBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_vietqr_logo),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape),
            )
        } else if (data.emvPayload.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.bill_split_vietqr_missing),
                    color = AppTheme.colors.textSecondary,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        }
    }
}
