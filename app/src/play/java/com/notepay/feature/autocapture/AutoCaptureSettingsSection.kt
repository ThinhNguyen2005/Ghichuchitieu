package com.notepay.feature.autocapture

import androidx.compose.foundation.lazy.LazyListScope

/**
 * Bản play không có tính năng tự động ghi chi tiêu từ thông báo nên không thêm item nào.
 *
 * Khai báo dưới dạng extension của [LazyListScope] thay vì một @Composable rỗng: với
 * composable rỗng thì `item { }` vẫn chiếm một slot, và LazyColumn dùng spacedBy vẫn cộng
 * khoảng cách cho slot đó, để lại khoảng trống thừa trên bản play.
 */
fun LazyListScope.autoCaptureSettingsItem() = Unit
