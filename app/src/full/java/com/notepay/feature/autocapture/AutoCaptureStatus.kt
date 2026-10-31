package com.notepay.feature.autocapture

import android.content.Context
import com.notepay.data.datastore.AutoCaptureSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Bản full có Auto-Capture: đếm số nguồn thông báo ngân hàng đang được tự ghi.
 * Trả về 0 khi tính năng tắt để màn Home ẩn dải trạng thái.
 *
 * Store được tạo trực tiếp vì DataStore của nó là delegate mức file (một instance duy
 * nhất cho cả tiến trình), nên thể hiện này dùng chung dữ liệu với bản Hilt trong Cài đặt.
 */
fun autoCaptureSourceCount(context: Context): Flow<Int> =
    AutoCaptureSettingsStore(context).settings.map { settings ->
        if (settings.autoCaptureEnabled) settings.enabledPackages.size else 0
    }
