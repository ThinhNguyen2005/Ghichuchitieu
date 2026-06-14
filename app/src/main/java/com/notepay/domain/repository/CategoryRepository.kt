package com.notepay.domain.repository

import com.notepay.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Interface quản lý các danh mục chi tiêu/thu nhập.
 * Hỗ trợ các danh mục mặc định và danh mục tùy biến (custom) do người dùng tạo.
 */
interface CategoryRepository {
    /** Lắng nghe danh sách toàn bộ danh mục dưới dạng Flow */
    fun observeCategories(): Flow<List<Category>>

    /** Lấy danh sách toàn bộ danh mục đồng bộ */
    suspend fun getCategories(): List<Category>

    /** Thêm danh mục tùy biến mới */
    suspend fun addCustomCategory(category: Category)
}
