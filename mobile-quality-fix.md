# Mobile Quality Fix — NotePay

> Khắc phục các vấn đề chất lượng mobile được phát hiện qua audit dựa trên bộ skill `mobile-design` từ `.agents`.

---

## Overview

Ứng dụng NotePay (Android, Kotlin + Jetpack Compose, Hilt DI) đang mắc phải nhiều vấn đề vi phạm các nguyên tắc mobile design từ `platform-android.md`, `mobile-performance.md`, `touch-psychology.md` và `mobile-design-thinking.md`. Bản kế hoạch này chia thành **5 nhóm vấn đề** được sắp xếp theo ưu tiên từ cao đến thấp.

**Project Type:** MOBILE (Android)  
**Primary Agent:** `mobile-developer`  
**Framework:** Kotlin + Jetpack Compose  
**OS:** Windows

---

## Success Criteria

| Tiêu chí | Metric |
|----------|--------|
| Không còn `forEach` render giao dịch bên trong `item { Column { } }` | 0 vi phạm |
| Tổng thu/chi trong `stickyHeader` được `remember()` | 100% |
| Tất cả vùng chạm interactive ≥ 48dp | 100% |
| Số `contentDescription = null` trên Icon interactive giảm về 0 | 0 còn lại |
| Chuỗi tiếng Việt hardcoded trong UI layer chuyển sang `strings.xml` | 100% |
| Build `./gradlew assembleDebug` pass không lỗi | ✅ |

---

## Tech Stack

| Thành phần | Công nghệ | Ghi chú |
|-----------|-----------|---------|
| Language | Kotlin | Existing |
| UI | Jetpack Compose + Material 3 | Existing |
| DI | Hilt | Existing |
| Min SDK | 26 (Android 8.0) | Existing |
| Build | Gradle KTS | Existing |

---

## Phân tích chi tiết các vấn đề

### Nhóm 1: 🔴 Hiệu năng Danh sách (CRITICAL)

**Nguồn skill:** `mobile-performance.md` — *"Tránh rendering toàn bộ danh sách trong một lần pass."*

#### Vấn đề 1.1: `HomeScreen.kt` — `forEach` trong `item {}` block

**File:** `app/src/main/java/com/notepay/ui/feature/home/HomeScreen.kt` (dòng 390–401)

```kotlin
// ❌ HIỆN TẠI: Toàn bộ giao dịch render cùng lúc — không recycle
item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.recentTransactions.forEach { tx ->
            TransactionItem(...)
        }
    }
}
```

**Hậu quả:** Nếu danh sách có 50+ giao dịch gần đây, tất cả `TransactionItem` được khởi tạo và layout trong cùng 1 frame, gây lag/drop frame.

**Sửa:**
```kotlin
// ✅ Lazy rendering riêng từng item
items(
    items = state.recentTransactions,
    key = { it.id }
) { tx ->
    val walletName = state.wallets.find { it.id == tx.walletId }?.name ?: ""
    TransactionItem(
        transaction = tx,
        walletName = walletName,
        onClick = { onTransactionClick(tx.id) },
    )
}
```

#### Vấn đề 1.2: `TransactionListScreen.kt` — Calendar view `forEach` (dòng 317)

**File:** `app/src/main/java/com/notepay/ui/feature/list/TransactionListScreen.kt` (dòng 312–328)

```kotlin
// ❌ HIỆN TẠI:
Column(...) {
    dayTxList.forEach { transaction ->
        TransactionItem(...)
    }
}
```

**Sửa:** Chuyển thành `items()` trong LazyColumn, hoặc nếu đây là phần nhỏ trong scrollable view thì giữ nhưng giới hạn kích thước.

#### Vấn đề 1.3: `TransactionListScreen.kt` — `stickyHeader` tính toán nặng (dòng 451–452)

```kotlin
// ❌ HIỆN TẠI: filter + sumOf mỗi lần recompose khi cuộn
stickyHeader(key = "${page}_${date}") {
    val totalIncome = dayTxList.filter { ... }.sumOf { ... }
    val totalExpense = dayTxList.filter { ... }.sumOf { ... }
    ...
}
```

**Sửa:**
```kotlin
stickyHeader(key = "${page}_${date}") {
    val (totalIncome, totalExpense) = remember(dayTxList) {
        val income = dayTxList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.amountInCents }
        val expense = dayTxList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.amountInCents }
        income to expense
    }
    ...
}
```

---

### Nhóm 2: 🟡 Touch Target & Fitts' Law (MEDIUM)

**Nguồn skill:** `touch-psychology.md` — *"Vùng chạm tối thiểu 48dp × 48dp trên Android."*  
**Nguồn skill:** `platform-android.md` — *"Min touch target: 48dp (even if visual is smaller)"*

#### Vấn đề 2.1: Icon kích thước nhỏ thiếu padding chạm

**Các vị trí phát hiện:**

| File | Dòng | Kích thước visual | Vấn đề |
|------|------|--------------------|--------|
| `StatsDashboard.kt` | 241 | `18.dp` | Icon trong Row, thiếu padding chạm |
| `StatsDashboard.kt` | 287 | `18.dp` | Calendar icon nhỏ |
| `StatsDashboard.kt` | 409 | `17.dp` | KPI icon |
| `StatsDashboard.kt` | 469 | `12.dp` | Legend icon |
| `TransactionDetailScreen.kt` | 290 | `19.dp` | Detail icon |
| `TransactionItem.kt` | 100 | `40.dp` | CategoryAvatar — OK, nhưng cần kiểm tra Card toàn bộ |

**Sửa:** Cho các Icon interactive (bên trong `IconButton` hoặc `clickable`), đảm bảo vùng chạm tối thiểu 48dp bằng cách:
- Sử dụng `Modifier.minimumInteractiveComponentSize()` (Compose M3 built-in)
- Hoặc wrap trong `IconButton` (mặc định đã có 48dp touch target)

> **Lưu ý:** Icon thuần decorative (không clickable) không cần vùng chạm 48dp.

---

### Nhóm 3: 🟡 Accessibility — TalkBack (MEDIUM)

**Nguồn skill:** `platform-android.md` — *"Mọi phần tử interactive cần contentDescription."*

#### Vấn đề 3.1: ~94 lần `contentDescription = null` trên Icon

Hiện tại trong project có **~94 vị trí** Icon sử dụng `contentDescription = null`. Trong đó:
- **Decorative icons** (icon bên cạnh text đã mô tả chức năng): `null` là hợp lệ
- **Interactive standalone icons** (IconButton, clickable icon không có text label kèm): **BẮT BUỘC** cần `contentDescription`

**Các file cần sửa ưu tiên:**
- `HomeScreen.kt` — Notification icon, Battery icon, Warning icon
- `StatsScreen.kt` — Add icon, settings icon, feedback icons  
- `NotificationSettingsScreen.kt` — Hầu hết icon trong settings rows
- `AddWalletScreen.kt` — Back button, icon picker
- `BillSplit*.kt` — Config/action icons

**Cách tiếp cận:**
1. Quét toàn bộ `contentDescription = null`
2. Phân loại: decorative vs interactive
3. Chỉ sửa các interactive icon → thêm `contentDescription = stringResource(R.string.cd_...)`
4. Thêm các string resource mới vào `strings.xml`

---

### Nhóm 4: 🟡 Hardcoded Strings — i18n (MEDIUM)

**Nguồn skill:** `mobile-design-thinking.md` — *"Strings phải được externalize cho i18n."*

#### Vấn đề 4.1: Chuỗi tiếng Việt hardcoded trong code

| File | Dòng | Chuỗi | Cần chuyển |
|------|------|-------|------------|
| `TransactionItem.kt` | 63 | `"Chuyển khoản"` | `R.string.transfer_label` |
| `TransactionItem.kt` | 114 | `"Chuyển khoản nội bộ"` | `R.string.internal_transfer_cd` |
| `TransactionListScreen.kt` | 501 | `"Ví"` | `R.string.wallet_default` |
| `PaymentReconciliationSheet.kt` | 194 | `"Nhận bằng Chuyển khoản"` | `R.string.receive_by_transfer` |

> **Lưu ý:** Phần lớn app đã sử dụng `stringResource(R.string....)` rất tốt. Chỉ còn vài nơi bị sót.

---

### Nhóm 5: 🟢 Platform Compliance & Hardcoded Padding (LOW)

**Nguồn skill:** `platform-android.md` — *"Sử dụng WindowInsets thay vì hardcode padding."*

#### Vấn đề 5.1: Bottom padding hardcode 96dp/108dp

| File | Dòng | Giá trị |
|------|------|---------|
| `HomeScreen.kt` | 230 | `bottom = padding.calculateBottomPadding() + 96.dp` |
| `TransactionListScreen.kt` | 314 | `bottom = bottomSystemPadding + 96.dp` |
| `TransactionListScreen.kt` | 427 | `bottom = bottomSystemPadding + 108.dp` |

**Phân tích:** Các giá trị này bù cho FAB + BottomNavigationBar. Đây là pattern phổ biến trong Compose khi FAB overlay lên content. Tuy nhiên, giá trị cố định 96dp/108dp có thể không chính xác trên mọi thiết bị.

**Sửa tiềm năng:**
- Tính toán động dựa trên chiều cao thực tế của BottomBar + FAB
- Hoặc sử dụng `Modifier.navigationBarsPadding()` kết hợp với padding cố định nhỏ hơn cho FAB

#### Vấn đề 5.2: Theme không dùng Dynamic Color (Android 12+)

**File:** `Theme.kt`

Hiện tại `NotePayTheme` sử dụng bảng màu tĩnh, không tận dụng `dynamicDarkColorScheme()` / `dynamicLightColorScheme()` (mặc dù import sẵn ở dòng 9-10). App có `ThemeManager` cho phép chọn màu manual.

**Đánh giá:** Đây là **thiết kế có chủ đích** (ThemeManager cho phép user chọn 8 màu chủ đề + custom). Không nhất thiết phải sửa, nhưng có thể thêm option "System Dynamic Color" vào ThemeManager cho API 31+.

---

## Task Breakdown

### Phase 1: Performance (P0)

| Task | File | Agent | Skill | Ước lượng |
|------|------|-------|-------|-----------|
| **T1.1** Chuyển `forEach` → `items()` trong HomeScreen | `HomeScreen.kt` | `mobile-developer` | `mobile-design` | 5 phút |
| **T1.2** Chuyển `forEach` → `items()` trong TransactionListScreen Calendar view | `TransactionListScreen.kt` | `mobile-developer` | `mobile-design` | 5 phút |
| **T1.3** Wrap tính toán `stickyHeader` bằng `remember()` | `TransactionListScreen.kt` | `mobile-developer` | `mobile-design` | 3 phút |

**INPUT → OUTPUT → VERIFY:**
- INPUT: Các file `.kt` với forEach/tính toán inline
- OUTPUT: Lazy items với key + remember()
- VERIFY: `./gradlew assembleDebug` pass + scroll mượt trên device

---

### Phase 2: Touch Targets (P1)

| Task | File(s) | Agent | Skill | Ước lượng |
|------|---------|-------|-------|-----------|
| **T2.1** Audit tất cả interactive Icon < 48dp | Toàn bộ `ui/feature/` + `ui/component/` | `mobile-developer` | `mobile-design` | 10 phút |
| **T2.2** Thêm `minimumInteractiveComponentSize()` cho các vi phạm | Files từ T2.1 | `mobile-developer` | `mobile-design` | 15 phút |

**INPUT → OUTPUT → VERIFY:**
- INPUT: Danh sách Icon interactive < 48dp
- OUTPUT: Tất cả touch target ≥ 48dp
- VERIFY: Visual inspection trên emulator + layout bounds overlay

---

### Phase 3: Accessibility (P1)

| Task | File(s) | Agent | Skill | Ước lượng |
|------|---------|-------|-------|-----------|
| **T3.1** Phân loại 94 vị trí `contentDescription = null` | Toàn bộ UI | `mobile-developer` | `mobile-design` | 15 phút |
| **T3.2** Thêm `contentDescription` cho interactive icons + strings.xml | Nhiều file | `mobile-developer` | `mobile-design` | 20 phút |

**INPUT → OUTPUT → VERIFY:**
- INPUT: Danh sách phân loại decorative vs interactive
- OUTPUT: Interactive icon đều có contentDescription
- VERIFY: Bật TalkBack, kiểm tra mọi nút bấm được đọc đúng

---

### Phase 4: Hardcoded Strings (P2)

| Task | File(s) | Agent | Skill | Ước lượng |
|------|---------|-------|-------|-----------|
| **T4.1** Chuyển 4 chuỗi hardcoded → `strings.xml` | `TransactionItem.kt`, `TransactionListScreen.kt`, `PaymentReconciliationSheet.kt` | `mobile-developer` | `i18n-localization` | 10 phút |

**INPUT → OUTPUT → VERIFY:**
- INPUT: Chuỗi hardcoded tiếng Việt
- OUTPUT: `stringResource(R.string.xxx)` + entry trong `strings.xml`
- VERIFY: Grep không còn chuỗi tiếng Việt hardcoded trong UI layer

---

### Phase 5: Platform Padding (P3 — Optional)

| Task | File(s) | Agent | Skill | Ước lượng |
|------|---------|-------|-------|-----------|
| **T5.1** Đánh giá và refactor bottom padding hardcode | `HomeScreen.kt`, `TransactionListScreen.kt` | `mobile-developer` | `mobile-design` | 15 phút |

**INPUT → OUTPUT → VERIFY:**
- INPUT: Padding hardcode 96dp/108dp
- OUTPUT: Padding động hoặc tính toán chuẩn hơn
- VERIFY: Test trên nhiều kích thước màn hình (emulator)

---

## Phase X: Verification Checklist

- [ ] `./gradlew assembleDebug` pass không lỗi
- [ ] Không còn `forEach` render danh sách giao dịch trong `item { Column { } }`
- [ ] `stickyHeader` tính toán tổng được wrap trong `remember()`
- [ ] Tất cả interactive Icon có touch target ≥ 48dp
- [ ] Tất cả interactive standalone Icon có `contentDescription` ≠ null
- [ ] Không còn chuỗi tiếng Việt hardcoded trong Composable UI code
- [ ] Scroll danh sách giao dịch mượt mà (visual inspection)
- [ ] TalkBack đọc đúng mọi nút bấm trên HomeScreen

---

## Risk Assessment

| Rủi ro | Xác suất | Impact | Giảm thiểu |
|--------|----------|--------|------------|
| Chuyển `forEach` → `items()` thay đổi layout spacing | Trung bình | Thấp | Dùng `Arrangement.spacedBy(8.dp)` giống hiện tại |
| Thêm `minimumInteractiveComponentSize()` làm thay đổi UI layout | Thấp | Trung bình | Kiểm tra visual trước/sau trên emulator |
| String resource mới thiếu key đúng | Thấp | Thấp | Đặt tên key theo convention hiện tại |

---

> **Tổng thời gian ước tính:** ~1.5 giờ cho tất cả 5 phase  
> **Phase bắt buộc:** 1, 2, 3, 4 (~1 giờ)  
> **Phase tùy chọn:** 5 (~15 phút)
