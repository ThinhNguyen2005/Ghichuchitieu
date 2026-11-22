---
version: 5.0.0
name: NotePay — Luminous iOS & Atmospheric Canvas
description: Bộ design token v5 cho NotePay. Chuyển từ thẻ mực đen phẳng sang hệ giao diện iOS nổi khối sáng (Luminous Hero Cards), đổ bóng mềm hai lớp (`Modifier.iosCardShadow`), nền atmospheric toàn màn hình (`AppAtmosphericBackground`) hỗ trợ 6 preset sáng/tối và ảnh nền cá nhân hóa có lớp phủ đảm bảo tương phản. Giữ chữ hiển thị serif Fraunces + số liệu Inter tabular (`tnum`), màu cát (`sand`) làm điểm nhấn chọn lọc, và thanh điều hướng pill nổi ở đáy.
---

# NotePay Design Specification v5.0.0: Luminous iOS & Atmospheric Canvas

> **Checklist áp dụng & kiểm tra toàn app:** Xem đầy đủ tại [ui-ux-audit-guide.md](ui-ux-audit-guide.md) (Hạng mục 6 — bảng theo dõi từng màn hình Tier 1 / Tier 2 / Tier 3 và 8 tiêu chí nghiệm thu v5.0).

## 1. Hướng đi & Nguyên tắc cốt lõi (v5.0)

NotePay v5 chuẩn hóa trải nghiệm thị giác theo **phong cách iOS hiện đại, nhẹ, nổi khối tự nhiên trên nền atmospheric**, giải quyết 3 nhược điểm của v4:
1. **Không dùng thẻ đen (`ink`) cho Hero Card:** Các thẻ tâm điểm (Số dư Trang chủ, Tổng thu/chi Báo cáo, Tổng tài sản ròng, Khối nhập số tiền Thêm giao dịch, Tổng quan Sổ nợ) chuyển sang **Luminous iOS Elevated Hero Card** sáng ở Light mode và xám slate sâu dịu ở Dark mode. Token `ink` (`#0B0B0B`) chỉ dành cho **nút CTA chính** và **thanh điều hướng nổi** ở đáy.
2. **Đổ bóng mềm hai lớp chuẩn iOS (`Modifier.iosCardShadow`):** Thay giao diện phẳng hoàn toàn bằng đổ bóng 2 lớp (`Ambient` rộng + `Contact` mảnh) giúp phân tầng rõ ràng giữa nền ảnh/gradient, nút nổi, thẻ nội dung và thẻ tâm điểm.
3. **Nền Atmospheric toàn màn hình (`AppAtmosphericBackground`):** Toàn bộ màn hình đặt trên `AppAtmosphericBackground` (cung cấp qua `LocalAppAtmosphericBackground`) hỗ trợ 6 preset Sáng/Tối (`warm_silk`, `ocean_mist`, `sage_morning`, `sunset_bloom`, `aurora_lavender`, `minimal_canvas`) và ảnh nền thư viện do người dùng chọn kèm scrim chuyển sắc tự động bảo vệ độ tương phản `>= 4.5:1`.

## 2. Hệ màu & Token bề mặt v5

### Bảng màu cốt lõi (`Color.kt` & `AppColors.kt`)

| Vai trò / Token | Light | Dark | Dùng cho |
|---|---|---|---|
| `HomeHeroCard*Top` -> `Bottom` | `#FFFFFF` -> `#FAF8F4` | `#282B32` -> `#1E2127` | Nền chuyển sắc dọc của Luminous Hero Card |
| `HomeHeroCard*Rim` | `#E6FFFFFF` (1dp) | `#24FFFFFF` (1dp) | Viền sáng 1dp tạo cạnh kính iOS trên Hero Card |
| `surface` | `#FFFFFF` | `#1D1D1D` | Thẻ nhóm, thẻ biểu đồ, thẻ ví, bàn phím |
| `surfaceSunken` | `#F4F1EC` | `#191919` | Ô chọn phụ, track segmented pill, phím số phụ |
| `background` | `#EFEFEF` | `#141414` | Màu nền fallback & nền scrim Light/Dark |
| `sand` / `onSand` | `#F2D3A8` / `#111111` | `#F2D3A8` / `#111111` | Điểm sáng pill/tab đang chọn, thanh mục tiêu, badge |
| `ink` / `onInk` | `#0B0B0B` / `#FFFFFF` | `#262626` / `#F2F2F2` | Nút Primary CTA, thanh điều hướng nổi (không dùng cho Hero Card) |
| `textPrimary` / `textSecondary` | `#111111` / `#5C5C5C` | `#F2F2F2` / `#A8A8A8` | Chữ chính (số dư hero) & nhãn phụ |
| `success` (Income) | `#2F6B3A` | `#7FCB8C` | Chỉ dùng cho dòng tiền thu / chỉ báo tích cực |
| `error` (Expense) | `#A63B2E` | `#FF8F7E` | Chỉ dùng cho dòng tiền chi / cảnh báo vượt ngưỡng |
| `warning` | `#8A5A00` | `#F2C26B` | Cảnh báo thận trọng (kèm icon + text) |

### 6 Preset nền Atmospheric Sáng / Tối (`HomePreset*` trong `Color.kt`)

| Preset ID | Light Stops (`Top -> Mid -> Bottom`) | Dark Stops (`Top -> Mid -> Bottom`) |
|---|---|---|
| `warm_silk` (mặc định) | `#FBF5EC -> #F5E6D3 -> #F3F1EE` | `#181615 -> #271F19 -> #141414` |
| `ocean_mist` | `#EAF3FF -> #D9ECF8 -> #F2F5F8` | `#0E1824 -> #15293D -> #11151B` |
| `sage_morning` | `#EDF6F0 -> #DCECE2 -> #F2F5F3` | `#101C17 -> #182D24 -> #121614` |
| `sunset_bloom` | `#FFF1EC -> #FCE0D4 -> #F1ECF8` | `#1E1420 -> #2D1C25 -> #141318` |
| `aurora_lavender` | `#F2EFFF -> #E4E5FC -> #F4F3F8` | `#141427 -> #211E3D -> #13131A` |
| `minimal_canvas` | `#F2F2F7 -> #EFEFF4 -> #F2F2F7` | `#121316 -> #16181D -> #121316` |

**Quy tắc màu:**
- Không hardcode mã Hex trực tiếp trong composable; mọi màu đều lấy từ `AppTheme.colors` hoặc hằng token trong `Color.kt`.
- `success` / `error` là hai màu ngữ nghĩa dòng tiền, không dùng trang trí ngẫu nhiên. Ô chỉ số thu/chi trong Hero Card dùng nền pha mềm (`color.copy(alpha = if (isLight) 0.10f else 0.16f)`) và chữ/icon cùng màu ngữ nghĩa.
- Trạng thái (an toàn / cảnh báo / vượt ngưỡng) luôn phải có đủ **icon + chữ + màu**.
- Chữ đặt trên `AppAtmosphericBackground` (đặc biệt khi dùng ảnh thư viện `custom`) phải có scrim chuyển sắc bảo vệ tương phản tối thiểu `4.5:1`.

## 3. Typography (`AppTypography.kt`)

- **Inter** (`InterFontFamily`, 400 / 500 / 600) cho toàn bộ giao diện, điều khiển và số liệu; bật `tnum` (`fontFeatureSettings = "tnum"`) cho mọi số tiền và tỷ lệ.
- **Fraunces** (`FrauncesFontFamily`, `displaySerif` 26sp / 30sp lineHeight) chỉ dùng cho **tiêu đề màn hình / lời chào / tiêu đề mục ngắn** (không dùng cho số tiền hoặc đoạn văn dài).
- **Kích thước chữ tối thiểu >= 12sp** trên toàn app (tuyệt đối không dùng `10.sp` / `11.sp`).
- `amountHero`: 44sp / Medium / `-0.03em` / `tnum` (`maxLines = if (fontScale >= 1.45f) 2 else 1` + `TextOverflow.Ellipsis`).

## 4. Bo góc và khoảng cách (`AppShapes.kt` & `AppDimensions.kt`)

Thang bo góc đồng nhất (bội số của `4dp`): `28 / 24 / 20 / 16 / 12` và `capsule` (`circle`).
- Luminous Hero Card: `AppTheme.shapes.corner28` (`28.dp`).
- Card phân tích lớn (Biểu đồ, Ngân sách, Xu hướng dài hạn, Bàn phím số): `AppTheme.shapes.corner24` (`24.dp`).
- Thẻ nhóm giao dịch theo ngày, Thẻ Mẹo tài chính, Thẻ ví, Trường chọn ví/ngày: `AppTheme.shapes.corner20` (`20.dp`).
- Ô chỉ số thu/chi lồng bên trong Hero Card (`CashflowMetricTile`), ô chọn danh mục nhanh, pill ghi chú: `AppTheme.shapes.corner16` (`16.dp`).
- Nút chip bộ lọc, toggle, CTA chính: `AppTheme.shapes.capsule` hoặc `AppTheme.shapes.circle` (`48.dp`).
- Grid khoảng cách `4dp`: `4 / 8 / 12 / 16 / 24 / 32`. Lề ngang màn hình `16.dp`. Chừa đáy tối thiểu `120.dp` (hoặc `104.dp + navBarInset`) ở các màn dùng thanh điều hướng nổi.

## 5. Chiều sâu & Đổ bóng chuẩn iOS (`IosShadowModifier.kt`)

Dùng `androidx.compose.ui.draw.dropShadow` hai lớp theo chế độ Sáng/Tối:

| Cấp độ | Tham số Light (`HomeShadowLight` `#1A1F2C`) | Tham số Dark (`HomeShadowDark` `#000000`) | Dùng cho |
|---|---|---|---|
| **Elevated (`elevated = true`)** | Ambient: blur `24.dp`, offsetY `12.dp`, alpha `0.10f`<br>Contact: blur `6.dp`, offsetY `2.dp`, alpha `0.05f` | Ambient: blur `24.dp`, offsetY `12.dp`, alpha `0.42f`<br>Contact: blur `6.dp`, offsetY `2.dp`, alpha `0.24f` | Luminous Hero Card trên các màn |
| **Standard (`elevated = false`)** | Ambient: blur `16.dp`, offsetY `6.dp`, alpha `0.07f`<br>Contact: blur `3.dp`, offsetY `1.dp`, alpha `0.04f` | Ambient: blur `16.dp`, offsetY `6.dp`, alpha `0.34f`<br>Contact: blur `3.dp`, offsetY `1.dp`, alpha `0.20f` | Thẻ nhóm, thẻ ví, thẻ mẹo, nút/pill nổi, bàn phím |

- **Quy tắc thứ tự Modifier bắt buộc:** `Modifier.iosCardShadow(shape, elevated)` **luôn đặt trước** `.clip(shape)`, `.background(...)`, hoặc `Surface(shape = ...)`. Cha chứa thẻ có bóng phải chừa tối thiểu `12.dp` padding tránh cắt bóng.
- `BottomSheet`: Dùng `ModalBottomSheet` (`sheetTop`, `surface`, `scrimColor = Color.Black.copy(alpha = 0.45f)`). Không dùng `Dialog` cho form nhập liệu hay danh sách dài.

## 6. Thành phần chuẩn v5

- **Luminous iOS Hero Card** (không dùng thẻ đen): Gradient dọc 2 điểm + viền sáng `1.dp` + `iosCardShadow(corner28, elevated = true)`. Bên trong gồm: tiêu đề phụ (`textSecondary`), nút tác vụ phụ/ẩn số dư (`48.dp`), số tiền `amountHero` (`textPrimary`), `HorizontalDivider(0.5.dp, hairline)`, và 2 ô `corner16` lồng bên trong (`Thu` nền xanh dịu / `Chi` nền đỏ dịu) tự xếp dọc khi `fontScale >= 1.45f`.
- **Header & Control Pills nổi:** Tiêu đề `displaySerif` + `heading()`, nút icon tròn `48.dp` (`Surface` nền `surface @ 90%/85%`, viền `0.5.dp hairline`, `iosCardShadow(CircleShape, false)`), chip chọn ví / bộ chọn kỳ `capsule` cao tối thiểu `48.dp`.
- **Smart Financial Insights Card:** Hiển thị đầy đủ toàn văn tiêu đề và nội dung (không cắt `maxLines = 1`), huy hiệu lĩnh vực (`Chi tiêu`, `Tiết kiệm`, `Ngân sách`, `Thói quen`, `Định kỳ`, `Quản lý ví`), bộ đếm vị trí, nút xem mẹo tiếp theo `48.dp` và nút đóng `48.dp` lưu bền vững vào `AppSettingsDataStore` (tự sinh mẹo đa dạng + xoay vòng qua `HomeSmartTipGenerator`, loại trừ chuyển khoản nội bộ).
- **Grouped iOS List Cards (`HomeDaySection`, `UtilitiesScreen`, `TransactionList`):** Thẻ trắng `corner20` có `iosCardShadow` + `0.5.dp hairline`, dòng tối thiểu `48–64.dp`, avatar `40.dp`, vạch chia thụt lề `HorizontalDivider(start = 68.dp, end = 16.dp, 0.5.dp, hairline)`.
- **Áp dụng cho màn hình dữ liệu dày:** Các màn Báo cáo, Tài sản, Thêm giao dịch dùng `AppAtmosphericBackground(..., forceSoftBlurOnPhoto = true)` để khi người dùng đặt ảnh nền tùy chỉnh từ thư viện, nền tự làm mờ nhẹ tạo chiều sâu mà bảng số liệu/biểu đồ/bàn phím trên thẻ trắng vẫn đọc rõ tuyệt đối.

## 7. Nên và Không nên

### Nên
- Đặt 1 **Luminous iOS Hero Card** làm tâm điểm trên nền `AppAtmosphericBackground`.
- Gắn `iosCardShadow` trước `clip`/`Surface` để tạo độ nổi tự nhiên kiểu iOS ở cả Light và Dark mode.
- Đảm bảo mọi nút, chip, toggle, hàng cài đặt đạt tối thiểu `48.dp` vùng chạm và giữ chữ `>= 12sp` co giãn được tới `fontScale 2.0x`.
- Ghi đầy đủ chữ tiếng Việt + tiếng Anh vào cả `values/strings.xml` và `values-en/strings.xml`.

### Không nên
- Không dùng thẻ nền đen (`AppTheme.colors.ink`) làm Hero Card nữa (chỉ dùng `primary` / `ink` cho nút CTA chính và thanh navigation pill).
- Không cắt bớt thông tin quan trọng bằng `maxLines = 1` ở thẻ lời khuyên/insight, không lưu trạng thái đóng thẻ chỉ trong RAM.
- Không hardcode `.sp < 12`, mã Hex màu, hoặc giữ thẻ tĩnh phẳng không tương thích hệ bóng v5 trên các màn đã migrate.

## 8. Ánh xạ từ v4 -> v5 & Tình trạng áp dụng

| Thành phần | v4 (Sand & Ink) | v5 (Luminous iOS & Atmospheric Canvas) |
|---|---|---|
| Nền màn hình | Nền xám phẳng `#EFEFEF` / `#141414` + backdrop nhỏ ở Home | `AppAtmosphericBackground` toàn app (6 preset + ảnh tùy chỉnh có scrim) |
| Thẻ Hero tâm điểm | Thẻ mực đen `ink` nguyên khối (`#0B0B0B`) | **Luminous iOS Hero Card** (`HomeHeroCard*Top -> Bottom` + viền `Rim`) |
| Chiều sâu thẻ | Phẳng `0.dp` không bóng | `Modifier.iosCardShadow` 2 lớp (`elevated = true` cho Hero, `false` cho thẻ/pill) |
| Phân nhóm thu/chi | Text trơn trên nền đen / dải xám | 2 ô lồng `corner16` pha màu ngữ nghĩa dịu, tự xếp dọc ở `fontScale >= 1.45f` |
| Phân tách dòng danh sách | Khoảng trắng không vạch chia | Avatar `40.dp` + `HorizontalDivider` mảnh `0.5.dp` thụt lề theo kiểu iOS |
| Mẹo tài chính thông minh | Cố định 1 câu cắt `maxLines = 1`, reset khi tắt app | Đa dạng theo dữ liệu + xoay vòng 16 mẫu (`HomeSmartTipGenerator`), full text, lưu DataStore |

> **Trạng thái hiện tại (22/11/2026):** Đã hoàn thành nâng cấp chuẩn v5 cho toàn bộ **Tier 1** (`HomeScreen`, `StatsScreen`, `AssetsScreen`, `TransactionFormScreen`, `UtilitiesScreen`) và `DebtSummaryCard`. Danh sách đầy đủ các màn Tier 2 & Tier 3 cần áp dụng dần nằm ở **Hạng mục 6 của `ui-ux-audit-guide.md`**.
