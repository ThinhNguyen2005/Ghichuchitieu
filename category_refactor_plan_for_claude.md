# Yêu cầu Refactor: Chuẩn hóa Đa ngôn ngữ (i18n) và Làm phong phú Icon cho Danh mục (Categories)

Xin chào Claude, hiện tại trong dự án Android Jetpack Compose của tôi, các danh mục thu chi (`Category`) đang bị hardcode text tiếng Việt trực tiếp trong class Domain (`Category.kt`) và bộ Icon đang sử dụng còn khá cơ bản. 

Dựa trên nguyên tắc "Zero Hardcoded Strings" và yêu cầu về UI/UX phong phú, tôi cần bạn giúp đưa ra một giải pháp toàn diện cho 2 vấn đề sau:

## 1. Vấn đề Đa ngôn ngữ (i18n)
Hiện tại `Category.kt` đang định nghĩa như sau:
```kotlin
val FOOD = Category("FOOD", "Ăn uống", 0xFFE57373L, isIncome = false)
val TRANSPORT = Category("TRANSPORT", "Di chuyển", 0xFF64B5F6L, isIncome = false)
// ... và khoảng 25+ danh mục khác
```
**Yêu cầu:**
- Loại bỏ hoàn toàn text cứng tiếng Việt khỏi file `Category.kt`.
- Tạo một cơ chế ánh xạ (ví dụ: `CategoryNameMapper.kt` ở tầng UI) để chuyển từ `Category.id` sang `StringRes` (ví dụ: `R.string.category_food`).
- Viết sẵn cho tôi file `strings.xml` cho cả 2 bản: Tiếng Anh (English) và Tiếng Việt. Tiếng Anh sẽ là ngôn ngữ gốc (default).

## 2. Vấn đề Icon Danh mục
Hiện tại `CategoryIconMapper.kt` đang ánh xạ các icon cơ bản từ `Icons.Rounded`. Một số danh mục đang dùng chung hoặc dùng icon chưa thực sự trực quan. Ví dụ:
- `BEAUTY` đang dùng `Icons.Rounded.Spa`
- `SALARY` dùng `Icons.Rounded.AttachMoney`
- Các mục như `TAX`, `INSURANCE`, `DEBT_LOAN` icon còn khá thô.

**Yêu cầu:**
- Tôi đang sử dụng thư viện `androidx.compose.material:material-icons-extended`.
- Hãy đề xuất một bộ mapping Icon mới, phong phú hơn, tinh tế hơn cho toàn bộ ~30 danh mục mặc định của ứng dụng. Bạn có thể kết hợp `Icons.Rounded`, `Icons.Outlined` hoặc `Icons.Filled` sao cho bộ icon nhìn đồng nhất, cao cấp và sát nghĩa nhất với từng danh mục.
- Nếu có danh mục nào bạn thấy cần thiết phải bổ sung cho một app Quản lý chi tiêu chuẩn quốc tế, hãy cứ đề xuất thêm!

---

### Dưới đây là danh sách danh mục hiện tại để bạn tham khảo và làm lại:
**Chi tiêu (Expense):**
FOOD, TRANSPORT, SHOPPING, BILL, ENTERTAINMENT, HEALTH, EDUCATION, COFFEE, BEAUTY, PETS, SPORTS, FAMILY, TRAVEL, CLOTHES, HOME, GAS, REPAIR, ELECTRICITY, WATER, INTERNET, CHILDREN, CHARITY, SAVINGS, DEBT_LOAN, INSURANCE, TAX, OTHER.

**Thu nhập (Income):**
SALARY, GIFT, INVESTMENT, BONUS, INCOME_OTHER.

Hãy cho tôi:
1. File `CategoryNameMapper.kt` (và cách áp dụng).
2. File `CategoryIconMapper.kt` với bộ icon đã được nâng cấp.
3. Nội dung file `strings.xml` (Tiếng Việt & Tiếng Anh).
