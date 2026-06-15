package com.notepay.domain.model

/**
 * Danh mục giao dịch. Mỗi category có:
 *  - id: định danh duy nhất (tương thích với enum name trong database)
 *  - displayName: tên hiển thị
 *  - colorArgb: màu nhận diện
 *  - isIncome: thuộc loại thu nhập (true) hay chi tiêu (false)
 *  - isCustom: true nếu do người dùng tạo thêm
 */
data class Category(
    val id: String,
    val displayName: String,
    val colorArgb: Long,
    val isIncome: Boolean = false,
    val isCustom: Boolean = false,
) {
    /**
     * Thuộc tính name trả về id để giữ tương thích 100% với code cũ
     * (ví dụ mapper cũ sử dụng category.name để lưu vào SQLite).
     */
    val name: String get() = id

    companion object {
        val FOOD = Category("FOOD", "Ăn uống", 0xFFE57373L, isIncome = false)
        val TRANSPORT = Category("TRANSPORT", "Di chuyển", 0xFF64B5F6L, isIncome = false)
        val SHOPPING = Category("SHOPPING", "Mua sắm", 0xFFFFB74DL, isIncome = false)
        val BILL = Category("BILL", "Hóa đơn", 0xFF81C784L, isIncome = false)
        val ENTERTAINMENT = Category("ENTERTAINMENT", "Giải trí", 0xFFBA68C8L, isIncome = false)
        val HEALTH = Category("HEALTH", "Sức khỏe", 0xFFF06292L, isIncome = false)
        val EDUCATION = Category("EDUCATION", "Học tập", 0xFF4DB6ACL, isIncome = false)
        val SALARY = Category("SALARY", "Lương", 0xFF66BB6AL, isIncome = true)
        val GIFT = Category("GIFT", "Quà/Cho", 0xFFFFD54FL, isIncome = true)
        
        val COFFEE = Category("COFFEE", "Cà phê/Trà", 0xFF8D6E63L, isIncome = false)
        val BEAUTY = Category("BEAUTY", "Làm đẹp", 0xFFF48FB1L, isIncome = false)
        val PETS = Category("PETS", "Thú cưng", 0xFFFFAB91L, isIncome = false)
        val SPORTS = Category("SPORTS", "Thể thao", 0xFFA5D6A7L, isIncome = false)
        val INVESTMENT = Category("INVESTMENT", "Đầu tư", 0xFF80CBC4L, isIncome = true)
        val FAMILY = Category("FAMILY", "Gia đình", 0xFFFFE082L, isIncome = false)
        val TRAVEL = Category("TRAVEL", "Du lịch", 0xFF9FA8DAL, isIncome = false)
        
        val OTHER = Category("OTHER", "Khác", 0xFF90A4AEL, isIncome = false)

        val DEFAULT_EXPENSE: Category = OTHER
        val DEFAULT_INCOME: Category = SALARY

        /** Category hợp lệ cho giao dịch thu nhập. */
        val INCOME_CATEGORIES: Set<Category> = setOf(SALARY, GIFT, INVESTMENT, OTHER)

        private val defaultEntries = listOf(
            FOOD, TRANSPORT, SHOPPING, BILL, ENTERTAINMENT, HEALTH, EDUCATION,
            COFFEE, BEAUTY, PETS, SPORTS, FAMILY, TRAVEL,
            SALARY, GIFT, INVESTMENT, OTHER
        )

        // Bộ đệm in-memory cho danh mục do user tạo thêm
        private val customCategories = java.util.concurrent.CopyOnWriteArrayList<Category>()

        /** Đăng ký danh sách danh mục tự tạo từ database/shared preferences */
        fun registerCustomCategories(categories: List<Category>) {
            customCategories.clear()
            customCategories.addAll(categories)
        }

        /** Trả về toàn bộ danh mục gồm mặc định + tự tạo */
        fun getAll(): List<Category> {
            return defaultEntries + customCategories
        }

        /** Tương thích với Category.entries cũ */
        val entries: List<Category> get() = getAll()

        fun safeValueOf(name: String?): Category {
            if (name == null) return OTHER
            return getAll().firstOrNull { it.id.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}
