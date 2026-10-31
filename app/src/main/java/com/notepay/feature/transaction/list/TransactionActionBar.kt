package com.notepay.feature.transaction.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme

/**
 * Thanh hành động hợp nhất của màn Giao dịch: tìm kiếm, khoảng thời gian và bộ lọc nằm trong
 * đúng một khung bo 16dp thay vì ba hàng riêng, nhờ đó phần danh sách có thêm chiều cao.
 *
 * Khi [collapsed] bật (người dùng đã cuộn xuống), ô nhập chữ thu về icon tìm kiếm để thanh gọn
 * lại; nếu đang có từ khóa thì vẫn giữ ô nhập để từ khóa không biến mất khỏi tầm mắt.
 */
@Composable
internal fun TransactionActionBar(
    query: String,
    onQueryChange: (String) -> Unit,
    dateLabel: String,
    activeFilterCount: Int,
    collapsed: Boolean,
    onExpandSearch: () -> Unit,
    onOpenDateRange: () -> Unit,
    onOpenFilter: () -> Unit,
    searchFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppTheme.colors.hairline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Không cố định chiều cao: ở font scale 1.3x-2.0x thanh tự giãn để chữ
                // tiếng Việt không bị cụt chân.
                .defaultMinSize(minHeight = 48.dp)
                .padding(start = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (collapsed && query.isBlank()) {
                // Vẫn chiếm toàn bộ vùng trống bên trái để cụm thời gian + bộ lọc luôn dính
                // mép phải, không bị nhảy vị trí khi thanh gập.
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    IconButton(onClick = onExpandSearch, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.action_search),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else {
                SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    searchFocusRequester = searchFocusRequester,
                    modifier = Modifier.weight(1f),
                )
            }

            ZoneDivider()

            DateRangeZone(label = dateLabel, onClick = onOpenDateRange)

            ZoneDivider()

            FilterZone(activeFilterCount = activeFilterCount, onClick = onOpenFilter)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.tx_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DateRangeZone(label: String, onClick: () -> Unit) {
    // Trần bề ngang của nhãn phải lớn lên cùng font scale, nếu không nhãn dài nhất
    // ("Toàn bộ thời gian") bị cắt ngay khi người dùng tăng cỡ chữ lên 1.3x. Trần 150dp giữ
    // cho ô tìm kiếm vẫn gõ được ở 2.0x thay vì bị đẩy về 0dp.
    val maxLabelWidth = (112.dp * LocalDensity.current.fontScale).coerceAtMost(150.dp)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = maxLabelWidth),
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun FilterZone(activeFilterCount: Int, onClick: () -> Unit) {
    val hasActiveFilters = activeFilterCount > 0
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                if (hasActiveFilters) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = activeFilterCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = if (hasActiveFilters) {
                    stringResource(R.string.cd_filter_active_count, activeFilterCount)
                } else {
                    stringResource(R.string.filter_advanced)
                },
                tint = if (hasActiveFilters) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ZoneDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(AppTheme.colors.hairline)
    )
}
