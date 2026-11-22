package com.notepay.feature.assets.shared

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.domain.model.SharedWalletInvite
import com.notepay.domain.model.SharedWalletMember
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.money.Money
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedWalletMembersScreen(
    onBack: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    viewModel: SharedWalletMembersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var editingMember by remember { mutableStateOf<SharedWalletMember?>(null) }
    var removingMember by remember { mutableStateOf<SharedWalletMember?>(null) }
    var showCreateInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { onFeedback(it) }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.wallet?.name ?: stringResource(R.string.shared_wallet_members_title),
                        style = AppTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (state.isCloudConfigured && state.wallet != null) {
                        IconButton(
                            onClick = viewModel::syncNow,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = stringResource(R.string.shared_wallet_sync_now),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.background,
                    titleContentColor = AppTheme.colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        SharedMembersBody(
            state = state,
            contentPadding = padding,
            onRetry = viewModel::refresh,
            onEditMember = { editingMember = it },
            onRemoveMember = { removingMember = it },
            onOpenCreateInvite = { showCreateInviteDialog = true },
            onCopyInvite = { code ->
                clipboard.setText(AnnotatedString(code))
                scope.launch {
                    onFeedback(
                        UiFeedback(
                            message = resources.getString(R.string.shared_wallet_invite_copied, code),
                            type = FeedbackType.Success,
                        ),
                    )
                }
            },
            onShareInvite = { code ->
                val walletName = state.wallet?.name.orEmpty()
                val text = resources.getString(R.string.shared_wallet_invite_share_text, walletName, code)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            },
            onRevokeInvite = viewModel::revokeInvite,
        )
    }

    editingMember?.let { member ->
        EditMemberDialog(
            member = member,
            onDismiss = { editingMember = null },
            onSave = { role, limitMinor ->
                viewModel.updateMember(member.userId, role, limitMinor)
                editingMember = null
            },
        )
    }

    removingMember?.let { member ->
        AlertDialog(
            onDismissRequest = { removingMember = null },
            title = { Text(stringResource(R.string.shared_wallet_member_remove)) },
            text = {
                Text(stringResource(R.string.shared_wallet_member_remove_confirm, member.displayName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(member.userId)
                        removingMember = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = AppTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { removingMember = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showCreateInviteDialog) {
        CreateInviteDialog(
            onDismiss = { showCreateInviteDialog = false },
            onConfirm = { role, hours, maxUses ->
                viewModel.createInvite(role, hours, maxUses)
                showCreateInviteDialog = false
            },
        )
    }
}

@Composable
private fun SharedMembersBody(
    state: SharedWalletMembersUiState,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onEditMember: (SharedWalletMember) -> Unit,
    onRemoveMember: (SharedWalletMember) -> Unit,
    onOpenCreateInvite: () -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
    onRevokeInvite: (String) -> Unit,
) {
    when {
        state.isLoading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = AppTheme.colors.primary)
        }

        !state.isCloudConfigured -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateWithAction(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(R.string.shared_wallet_members_title),
                description = stringResource(R.string.shared_wallet_offline_hint),
            )
        }

        state.errorMessage != null && state.members.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateWithAction(
                icon = Icons.Rounded.CloudOff,
                title = state.errorMessage,
                actionLabel = stringResource(R.string.action_retry_assets),
                onClick = onRetry,
            )
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "members_header") {
                Text(
                    text = stringResource(R.string.shared_wallet_members_section, state.members.size),
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                )
            }

            if (state.members.isEmpty()) {
                item(key = "members_empty") {
                    Text(
                        text = stringResource(R.string.shared_wallet_members_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            } else {
                items(state.members, key = { "member_${it.userId}" }) { member ->
                    MemberRowCard(
                        member = member,
                        canManage = state.isOwner &&
                            member.userId != state.currentUserId &&
                            member.role != SharedWalletRole.OWNER,
                        onEdit = { onEditMember(member) },
                        onRemove = { onRemoveMember(member) },
                    )
                }
            }

            if (state.isOwner) {
                item(key = "invites_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.shared_wallet_invites_section),
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Button(
                            onClick = onOpenCreateInvite,
                            enabled = !state.isSubmitting,
                            shape = AppTheme.shapes.capsule,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.ink,
                                contentColor = AppTheme.colors.onInk,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.shared_wallet_invite_create_action))
                        }
                    }
                }

                if (state.invites.isEmpty()) {
                    item(key = "invites_empty") {
                        Text(
                            text = stringResource(R.string.shared_wallet_invite_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                } else {
                    items(state.invites, key = { "invite_${it.code}" }) { invite ->
                        InviteRowCard(
                            invite = invite,
                            onCopy = { onCopyInvite(invite.code) },
                            onShare = { onShareInvite(invite.code) },
                            onRevoke = { onRevokeInvite(invite.code) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRowCard(
    member: SharedWalletMember,
    canManage: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val limitText = member.spendingLimitMinor?.let {
        stringResource(R.string.shared_wallet_member_limit_format, MoneyFormatter.format(Money(it)))
    } ?: stringResource(R.string.shared_wallet_member_no_limit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(1.dp, AppTheme.colors.separator),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.surfaceSunken),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = member.displayName.take(1).uppercase(Locale.ROOT).ifBlank { "?" },
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = member.displayName,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    RoleBadge(role = member.role)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = limitText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (canManage) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.shared_wallet_member_edit_title),
                        tint = AppTheme.colors.textSecondary,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.shared_wallet_member_remove),
                        tint = AppTheme.colors.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteRowCard(
    invite: SharedWalletInvite,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRevoke: () -> Unit,
) {
    val expiryDate = Instant.fromEpochMilliseconds(invite.expiresAtEpochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val expiryLabel = String.format(
        Locale.ROOT,
        "%02d/%02d %02d:%02d",
        expiryDate.day,
        expiryDate.month.number,
        expiryDate.hour,
        expiryDate.minute,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(1.dp, AppTheme.colors.separator),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = invite.code,
                    style = AppTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                )
                RoleBadge(role = invite.role)
            }
            Text(
                text = "${stringResource(R.string.shared_wallet_invite_uses_format, invite.usedCount, invite.maxUses)} · $expiryLabel",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopy,
                    shape = AppTheme.shapes.capsule,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.shared_wallet_invite_copy))
                }
                OutlinedButton(
                    onClick = onShare,
                    shape = AppTheme.shapes.capsule,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.shared_wallet_invite_share))
                }
                TextButton(
                    onClick = onRevoke,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.shared_wallet_invite_revoke),
                        color = AppTheme.colors.error,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RoleBadge(role: SharedWalletRole) {
    val labelRes = when (role) {
        SharedWalletRole.OWNER -> R.string.shared_wallet_role_owner
        SharedWalletRole.EDITOR -> R.string.shared_wallet_role_editor
        SharedWalletRole.VIEWER -> R.string.shared_wallet_role_viewer
    }
    val containerColor = when (role) {
        SharedWalletRole.OWNER -> AppTheme.colors.sand
        SharedWalletRole.EDITOR -> AppTheme.colors.surfaceSunken
        SharedWalletRole.VIEWER -> AppTheme.colors.surfaceSunken
    }
    val textColor = if (role == SharedWalletRole.OWNER) AppTheme.colors.onSand else AppTheme.colors.textSecondary

    Surface(
        shape = AppTheme.shapes.capsule,
        color = containerColor,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EditMemberDialog(
    member: SharedWalletMember,
    onDismiss: () -> Unit,
    onSave: (SharedWalletRole, Long?) -> Unit,
) {
    var selectedRole by remember {
        mutableStateOf(
            if (member.role == SharedWalletRole.VIEWER) SharedWalletRole.VIEWER else SharedWalletRole.EDITOR,
        )
    }
    var limitInput by remember {
        mutableStateOf(member.spendingLimitMinor?.div(100L)?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_wallet_member_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = member.displayName,
                    style = AppTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SharedWalletRole.EDITOR, SharedWalletRole.VIEWER).forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = {
                                Text(
                                    stringResource(
                                        if (role == SharedWalletRole.EDITOR) {
                                            R.string.shared_wallet_role_editor
                                        } else {
                                            R.string.shared_wallet_role_viewer
                                        },
                                    ),
                                )
                            },
                            shape = AppTheme.shapes.capsule,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppTheme.colors.sand,
                                selectedLabelColor = AppTheme.colors.onSand,
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { raw -> limitInput = raw.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.shared_wallet_member_limit_input)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limitMinor = limitInput.toLongOrNull()?.takeIf { it > 0L }?.times(100L)
                    onSave(selectedRole, limitMinor)
                },
            ) {
                Text(stringResource(R.string.action_save_changes), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun CreateInviteDialog(
    onDismiss: () -> Unit,
    onConfirm: (role: SharedWalletRole, expiresInHours: Int, maxUses: Int) -> Unit,
) {
    var role by remember { mutableStateOf(SharedWalletRole.EDITOR) }
    var expiryHours by remember { mutableIntStateOf(24) }
    var maxUses by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_wallet_invite_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.shared_wallet_invite_role_label), style = AppTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SharedWalletRole.EDITOR, SharedWalletRole.VIEWER).forEach { candidate ->
                        FilterChip(
                            selected = role == candidate,
                            onClick = { role = candidate },
                            label = {
                                Text(
                                    stringResource(
                                        if (candidate == SharedWalletRole.EDITOR) {
                                            R.string.shared_wallet_role_editor
                                        } else {
                                            R.string.shared_wallet_role_viewer
                                        },
                                    ),
                                )
                            },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        )
                    }
                }
                Text(stringResource(R.string.shared_wallet_invite_expiry_label), style = AppTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = expiryHours == 24,
                        onClick = { expiryHours = 24 },
                        label = { Text(stringResource(R.string.shared_wallet_invite_expiry_24h)) },
                        shape = AppTheme.shapes.capsule,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    )
                    FilterChip(
                        selected = expiryHours == 168,
                        onClick = { expiryHours = 168 },
                        label = { Text(stringResource(R.string.shared_wallet_invite_expiry_7d)) },
                        shape = AppTheme.shapes.capsule,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    )
                }
                Text(stringResource(R.string.shared_wallet_invite_uses_label), style = AppTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5).forEach { uses ->
                        FilterChip(
                            selected = maxUses == uses,
                            onClick = { maxUses = uses },
                            label = { Text(uses.toString()) },
                            shape = AppTheme.shapes.capsule,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(role, expiryHours, maxUses) }) {
                Text(stringResource(R.string.shared_wallet_invite_create_action), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
