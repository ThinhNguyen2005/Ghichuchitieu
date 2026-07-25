package com.notepay.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop

/**
 * Backdrop chung do NotePayNavHost cung cấp cho các bề mặt Liquid Glass.
 * Fallback rỗng vẫn cho phép component hiển thị surface có tương phản khi dùng độc lập.
 */
val LocalNotePayBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }