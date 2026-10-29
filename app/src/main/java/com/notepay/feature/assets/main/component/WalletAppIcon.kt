package com.notepay.ui.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notepay.domain.model.Wallet
import com.notepay.ui.util.BankIconHelper
import com.notepay.ui.util.WalletUiHelper

/**
 * Hiển thị Icon ví tài chính. 
 * Ưu tiên 1: Icon ứng dụng ngân hàng thật từ thiết bị.
 * Ưu tiên 2: Icon Vector mặc định nếu không có app ngân hàng hoặc là ví tiền mặt.
 */
@Composable
fun WalletAppIcon(
    wallet: Wallet,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color? = null
) {
    val context = LocalContext.current
    var bankDrawable by remember(wallet.linkedPackageName, wallet.bankBin) {
        mutableStateOf<Drawable?>(null)
    }

    LaunchedEffect(wallet.linkedPackageName, wallet.bankBin) {
        if (!wallet.linkedPackageName.isNullOrBlank()) {
            bankDrawable = BankIconHelper.getInstalledAppIcon(context, wallet.linkedPackageName)
        }
        if (bankDrawable == null && !wallet.bankBin.isNullOrBlank()) {
            bankDrawable = BankIconHelper.getInstalledAppIconByBankBin(context, wallet.bankBin)
        }
    }

    if (bankDrawable != null) {
        AsyncImage(
            model = bankDrawable,
            contentDescription = wallet.name,
            modifier = modifier
                .size(iconSize)
                .clip(CircleShape)
        )
    } else {
        val iconVector = WalletUiHelper.getIcon(wallet.iconKey)
        val tintColor = tint ?: WalletUiHelper.getColor(wallet.colorKey)
        
        Icon(
            imageVector = iconVector,
            contentDescription = wallet.name,
            tint = tintColor,
            modifier = modifier.size(iconSize)
        )
    }
}
