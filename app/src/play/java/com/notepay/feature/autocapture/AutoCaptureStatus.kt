package com.notepay.feature.autocapture

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Bản play không tự ghi chi tiêu từ thông báo ngân hàng nên luôn trả về 0,
 * khiến dải trạng thái Auto-Capture trên màn Home không hiển thị.
 */
fun autoCaptureSourceCount(context: Context): Flow<Int> = flowOf(0)
