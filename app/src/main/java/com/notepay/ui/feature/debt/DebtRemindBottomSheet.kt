package com.notepay.ui.feature.debt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.notepay.BuildConfig
import com.notepay.R
import com.notepay.domain.model.VietQrBank
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietQrGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtRemindBottomSheet(
    debtWithHistory: DebtWithHistory,
    wallets: List<Wallet>,
    banks: List<VietQrBank>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val debt = debtWithHistory.debt

    // Find receiving wallet: prefer debt.walletId, then active wallet, then any wallet with bank info
    val receivingWallet = remember(debt.walletId, wallets) {
        wallets.firstOrNull { it.id == debt.walletId && !it.bankBin.isNullOrBlank() && !it.accountNumber.isNullOrBlank() }
            ?: wallets.firstOrNull { it.isActive && !it.bankBin.isNullOrBlank() && !it.accountNumber.isNullOrBlank() }
            ?: wallets.firstOrNull { !it.bankBin.isNullOrBlank() && !it.accountNumber.isNullOrBlank() }
    }

    val bank = remember(receivingWallet?.bankBin, banks) {
        banks.firstOrNull { it.bin == receivingWallet?.bankBin }
    }

    val bankName = bank?.shortName ?: receivingWallet?.bankBin.orEmpty()
    val accountNumber = receivingWallet?.accountNumber.orEmpty()
    val accountName = receivingWallet?.accountName.orEmpty()
    val amountFormatted = MoneyFormatter.format(debtWithHistory.remainingAmount)

    val memo = remember(debt.personName) {
        val sanitized = debt.personName.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
        "NP $sanitized"
    }

    val hasBankConfig = receivingWallet != null && !receivingWallet.bankBin.isNullOrBlank() && !accountNumber.isBlank()

    val reminderMessage = remember(hasBankConfig, debt.personName, amountFormatted, accountNumber, bankName, accountName) {
        if (hasBankConfig) {
            context.getString(
                R.string.debt_share_remind_text_format,
                debt.personName,
                amountFormatted,
                accountNumber,
                bankName,
                accountName
            )
        } else {
            context.getString(
                R.string.debt_share_remind_simple_format,
                debt.personName,
                amountFormatted
            )
        }
    }

    val qrImageUrl = remember(hasBankConfig, receivingWallet, debtWithHistory.remainingAmount, memo) {
        if (hasBankConfig && receivingWallet != null) {
            VietQrGenerator.generateImageUrl(
                bankBin = receivingWallet.bankBin.orEmpty(),
                accountNumber = receivingWallet.accountNumber.orEmpty(),
                amountCents = debtWithHistory.remainingAmount.amountInCents,
                memo = memo,
                accountName = receivingWallet.accountName,
            )
        } else null
    }

    val emvPayload = remember(hasBankConfig, receivingWallet, debtWithHistory.remainingAmount, memo) {
        if (hasBankConfig && receivingWallet != null) {
            VietQrGenerator.generateEmvCoPayload(
                bankBin = receivingWallet.bankBin.orEmpty(),
                accountNumber = receivingWallet.accountNumber.orEmpty(),
                amountCents = debtWithHistory.remainingAmount.amountInCents,
                memo = memo,
                accountName = receivingWallet.accountName,
            )
        } else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.debt_qr_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = stringResource(R.string.debt_qr_dialog_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // QR Code Card
            if (hasBankConfig) {
                var remoteFailed by remember(qrImageUrl) { mutableStateOf(false) }
                val useRemoteQr = BuildConfig.FLAVOR == "play" && !qrImageUrl.isNullOrBlank() && !remoteFailed
                val localQrBitmap = remember(emvPayload, useRemoteQr) {
                    if (!useRemoteQr && !emvPayload.isNullOrBlank()) {
                        try {
                            VietQrGenerator.generateLocalQrBitmap(emvPayload, 512, 512)
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f),
                    shape = AppTheme.shapes.corner24,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (useRemoteQr) {
                            AsyncImage(
                                model = qrImageUrl,
                                contentDescription = stringResource(R.string.cd_vietqr_logo),
                                contentScale = ContentScale.Fit,
                                onError = { remoteFailed = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(AppTheme.shapes.corner16),
                            )
                        } else if (localQrBitmap != null) {
                            Image(
                                bitmap = localQrBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.cd_vietqr_logo),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(AppTheme.shapes.corner16),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Bank details display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bankName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = accountNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (accountName.isNotBlank()) {
                            Text(
                                text = accountName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.debt_qr_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            HorizontalDivider()

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Copy reminder message
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("debt_reminder", reminderMessage)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            context.getString(R.string.debt_copied_reminder),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = AppTheme.shapes.corner16,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.debt_action_copy_message),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Share message via Android Share Sheet
                FilledTonalButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reminderMessage)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    shape = AppTheme.shapes.corner16,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.debt_action_share),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // SMS shortcut if phone number exists
                if (!debt.phoneNumber.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${debt.phoneNumber}")
                                putExtra("sms_body", reminderMessage)
                            }
                            try {
                                context.startActivity(smsIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, reminderMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = AppTheme.shapes.corner16,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Message,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.debt_action_sms),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
