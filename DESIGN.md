---
version: 3.0.0
name: NotePay — Ledger Ink & Warm Paper
description: Bộ design token mới cho NotePay — tách hẳn khỏi mặc định Material 3 (tím #6750A4, elevation dp, corner scale đồng nhất) và khỏi việc chỉ sao chép iOS monochrome. Xây dựng bản sắc riêng: giấy sổ tay ấm (ledger paper), mực đậm, accent đất nung, bóng đổ mềm dạng "paper lift", bo góc bất đối xứng.

colors:
  # ==== LIGHT MODE — "Warm Paper Ledger" (~75%) ====
  background: "#F7F3EC"            # Giấy ngà ấm, không phải xám lạnh Material/Apple
  surface: "#FFFDF8"                # Bề mặt card — trắng ngà, ấm hơn #FFFFFF thuần
  surface-sunken: "#EFE8DC"         # Vùng lõm (input, ô nhập liệu chưa focus)
  surface-raised: "#FFFFFF"         # Bề mặt nổi cao nhất (modal, sheet)

  text-primary: "#1F1B16"           # Mực đen ấm (không phải #000000 lạnh)
  text-secondary: "#6B6255"         # Nâu xám ấm, ~4.8:1
  text-muted: "#A69C8C"             # Placeholder / disabled

  border: "#DCD3C2"                 # Viền giấy ấm
  border-strong: "#C7BBA4"          # Viền nhấn (dashed divider ledger)
  hairline: "rgba(31, 27, 22, 0.10)"

  # ==== LIGHT MODE — Accent thương hiệu (~15%) — KHÔNG phải tím M3, KHÔNG phải đen/trắng Apple ====
  primary: "#B5502E"                 # Đất nung (terracotta ink) — màu chữ ký của NotePay
  on-primary: "#FFF8F0"
  primary-container: "#F3DFCF"       # Nền nhạt của accent, dùng cho badge/trạng thái nhẹ
  on-primary-container: "#5C2413"

  secondary: "#3E4A3E"               # Xanh rêu đậm — dùng cho icon phụ, tab phụ
  on-secondary: "#F7F3EC"
  secondary-container: "#DCE3D5"
  on-secondary-container: "#1F2A1F"

  cta: "#B5502E"                     # Nút hành động chính: pill đất nung, không phải đen tuyền
  on-cta: "#FFF8F0"

  # ==== DARK MODE — "Ink Night" (~75%) ====
  dark-background: "#15130F"         # Đen ấm ngả nâu, không phải OLED #000000 thuần
  dark-surface: "#211E18"
  dark-surface-sunken: "#1A1712"
  dark-surface-raised: "#2A261E"

  dark-text-primary: "#F3ECDF"       # Trắng ngà, không trắng lạnh
  dark-text-secondary: "#B7AC97"
  dark-text-muted: "#7C7263"

  dark-border: "#3A3327"
  dark-border-strong: "#4E4433"
  dark-hairline: "rgba(243, 236, 223, 0.09)"

  # ==== DARK MODE — Accent (~15%) ====
  dark-primary: "#E08A5C"            # Đất nung sáng hơn cho nền tối
  dark-on-primary: "#2E1206"
  dark-primary-container: "#4A2415"
  dark-on-primary-container: "#F3DFCF"

  dark-secondary: "#8FA485"
  dark-on-secondary: "#12190F"
  dark-secondary-container: "#2B3527"
  dark-on-secondary-container: "#DCE3D5"

  dark-cta: "#E08A5C"
  dark-on-cta: "#2E1206"

  # ==== Semantic tài chính (~10%) — desaturated, ấm, KHÁC hệ Apple #34C759/#FF3B30 ====
  income: "#4C7A4A"                  # Xanh lá rêu đục, không phải xanh lá tươi hệ thống
  income-container: "#E1EBDC"
  dark-income: "#7FAE78"
  dark-income-container: "#233021"

  expense: "#A63B2E"                 # Đỏ gạch đất, cùng họ với primary — đồng bộ tông ấm
  expense-container: "#F3DAD2"
  dark-expense: "#D9705A"
  dark-expense-container: "#3D1B14"

  warning: "#B8862E"                 # Vàng đất (mù tạt), không phải vàng cam Apple
  warning-container: "#F3E6C8"
  dark-warning: "#D9AC5C"
  dark-warning-container: "#3B2C0F"

typography:
  # Ghép chữ: Serif hiển thị (bản sắc "sổ ghi chép") + Sans nội dung + Mono cho số
  display-large:
    fontFamily: "Fraunces"          # Serif ấm, thay cho Inter mặc định — tạo cảm giác "sổ tay", khác hẳn font hệ thống
    fontSize: 32px
    fontWeight: 600
    lineHeight: 1.18
    letterSpacing: -0.01em
  headline-large:
    fontFamily: "Fraunces"
    fontSize: 22px
    fontWeight: 600
    lineHeight: 1.25
  title-large:
    fontFamily: "Manrope"
    fontSize: 19px
    fontWeight: 700
    lineHeight: 1.26
  title-medium:
    fontFamily: "Manrope"
    fontSize: 17px
    fontWeight: 700
    lineHeight: 1.3
  body-large:
    fontFamily: "Manrope"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.4
  body-medium:
    fontFamily: "Manrope"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.4
  caption:
    fontFamily: "Manrope"
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.35
  eyebrow:
    fontFamily: "Manrope"
    fontSize: 10.5px
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: 0.08em
    textTransform: uppercase

  amount-hero:
    fontFamily: "JetBrains Mono"
    fontSize: 36px
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: -0.015em
  amount-card:
    fontFamily: "JetBrains Mono"
    fontSize: 24px
    fontWeight: 700
    lineHeight: 1.2
  amount-row:
    fontFamily: "JetBrains Mono"
    fontSize: 15px
    fontWeight: 600
    lineHeight: 1.25
  number-tabular:
    fontFamily: "JetBrains Mono"
    fontSize: 13px
    fontWeight: 500
    lineHeight: 1.3

# Bo góc: KHÔNG dùng thang đều 4/8/12/16/28 kiểu Material, mà là hệ bất đối xứng
# — mỗi surface có 1 góc "bẻ" khác các góc còn lại, gợi nhớ mép giấy gấp trong sổ tay
radius:
  chip: 10px
  input: 14px
  row: 16px
  card: "18px 18px 18px 6px"        # 3 góc tròn đều, góc dưới-trái bẻ nhọn — chữ ký hình khối của NotePay
  card-alt: "6px 18px 18px 18px"    # biến thể soi gương, dùng xen kẽ trong list để tạo nhịp điệu
  sheet-top: "24px 24px 0 0"
  pill: 999px
  avatar: 999px

spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 40px

# Đổ bóng: bỏ hệ "elevation dp" phẳng của Material. Dùng bóng đổ mềm, lệch nhẹ,
# mô phỏng ánh sáng chiếu chéo lên giấy — không dùng bóng màu, không dùng 0px phẳng tuyệt đối như bản iOS cũ
elevation:
  resting:
    boxShadow: "0px 1px 2px rgba(31,27,22,0.06)"
  raised:
    boxShadow: "0px 4px 10px rgba(31,27,22,0.08), 0px 1px 2px rgba(31,27,22,0.05)"
  floating:
    boxShadow: "0px 10px 24px rgba(31,27,22,0.12), 0px 2px 6px rgba(31,27,22,0.06)"
  pressed:
    boxShadow: "0px 0px 0px rgba(31,27,22,0)"   # phẳng hẳn khi nhấn — phản hồi xúc giác qua bóng, không chỉ qua scale

motion:
  fast: 130ms
  normal: 220ms
  emphasized: 300ms
  sheet-spring: 380ms
  easing-standard: cubic-bezier(0.22, 1, 0.36, 1)   # "ease-out-quint" — nảy nhẹ như trang giấy lật, khác FastOutSlowIn của Material
  spring-bounce:
    dampingRatio: 0.7
    stiffness: 320.0

components:
  primary-action-button:
    backgroundColor: "{colors.cta}"
    textColor: "{colors.on-cta}"
    radius: "{radius.pill}"
    height: 52px
    shadow: "{elevation.raised}"
  liquid-navigation-bar:
    backgroundColor: "{colors.surface-raised}"
    indicatorColor: "{colors.primary}"
    height: 62px
    radius: "{radius.pill}"
    shadow: "{elevation.floating}"
    border: "1px solid {colors.border}"
  balance-hero-card:
    backgroundColor: "{colors.surface}"
    radius: "{radius.card}"
    padding: "{spacing.lg}"
    shadow: "{elevation.raised}"
    border: "1px solid {colors.border}"
    accentEdge: "3px solid {colors.primary}"   # dải mực mảnh bên trái card — chi tiết nhận diện riêng, không có ở Material/Apple
  transaction-item-row:
    backgroundColor: "{colors.surface}"
    radius: "{radius.row}"
    padding: "{spacing.md}"
    swipeThreshold: 72px
    divider: "dashed 1px {colors.border-strong}"  # đường kẻ đứt kiểu sổ ghi chép giữa các dòng, thay cho hairline đặc
  amount-display-hero:
    typography: "{typography.amount-hero}"
    textColor: "{colors.text-primary}"
    currencySymbolColor: "{colors.text-muted}"
  category-chip:
    backgroundColor: "{colors.surface-sunken}"
    textColor: "{colors.text-primary}"
    radius: "{radius.chip}"
    height: 34px
    border: "1px solid {colors.border}"
  ai-advisor-card:
    backgroundColor: "{colors.primary-container}"
    borderColor: "{colors.primary}"
    textColor: "{colors.on-primary-container}"
    radius: "{radius.card-alt}"
    shadow: "{elevation.resting}"
---

# NotePay Design Specification v3: Ledger Ink & Warm Paper

## 1. Vì sao tách khỏi Material 3 và bản iOS monochrome cũ

**Vấn đề của bản cũ:**
- Bảng màu B&W thuần (`#000000`/`#FFFFFF`) là công thức của Apple Human Interface Guidelines — không tạo bản sắc riêng, ai nhìn cũng thấy quen mắt "giống app Apple".
- Xanh/đỏ income-expense (`#34C759`/`#FF3B30`) là màu hệ thống iOS mặc định, trùng với hàng nghìn app khác.
- Thang bo góc 8/12/16/20/24 đều và đối xứng là công thức chuẩn của cả Material lẫn iOS — không có điểm nhận diện riêng.
- `elevation: 0px` + border hairline là lối "phẳng hoá" điển hình của thiết kế hệ thống, không có chiều sâu vật lý.

**Hướng đi mới — ẩn dụ "sổ ghi chép" (ledger):**
Một app quản lý chi tiêu về bản chất là một cuốn sổ. NotePay v3 lấy giấy ấm, mực đất nung, và các chi tiết "viết tay" (góc bẻ, đường kẻ đứt, dải mực bên card) làm ngôn ngữ thị giác — thứ không thể nhầm với Material 3 hay bất kỳ app hệ thống nào.

```text
Nền giấy ấm (#F7F3EC) → Card giấy ngà (#FFFDF8), góc bẻ bất đối xứng
                                  ↓
Accent: Đất nung (#B5502E) — không đen/trắng, không tím Material
                                  ↓
Số liệu tài chính: Xanh rêu đục (income) / Đỏ gạch (expense) — cùng họ tông đất, không phải xanh-đỏ hệ thống
```

---

## 2. Hệ màu

### Canvas & Surface (~75%)
| Token | Light | Dark |
|---|---|---|
| background | `#F7F3EC` giấy ngà ấm | `#15130F` đen ấm |
| surface | `#FFFDF8` | `#211E18` |
| text-primary | `#1F1B16` mực ấm | `#F3ECDF` |
| text-secondary | `#6B6255` (~4.8:1) | `#B7AC97` |
| border | `#DCD3C2` | `#3A3327` |

Điểm khác biệt cốt lõi: **không dùng xám lạnh** (`#F2F2F7`, `#1C1C1E` kiểu Apple) và **không dùng trắng/đen tuyệt đối làm nền** — mọi tông đều ngả vàng/nâu nhẹ để tạo cảm giác giấy thật.

### Accent thương hiệu (~15%)
- `primary` = `#B5502E` (đất nung) thay cho đen/trắng thuần hoặc tím Material `#6750A4`.
- `secondary` = `#3E4A3E` (rêu đậm) cho các yếu tố phụ, không cạnh tranh với primary.
- Nút CTA là pill đất nung, không phải pill đen/trắng — nhận diện được ngay cả khi chụp màn hình không có logo.

### Semantic tài chính (~10%)
- Income `#4C7A4A`, Expense `#A63B2E` — đều được "hạ tông" (desaturate) và kéo về họ màu đất, để không chỏi với accent chính như cách xanh-lá-tươi/đỏ-tươi hệ thống thường chỏi với mọi bảng màu.
- *Quy tắc nghiêm ngặt*: đây vẫn là 2 màu DUY NHẤT mang nghĩa cash-flow — không dùng trang trí ở nơi khác.

---

## 3. Bo góc — hệ bất đối xứng thay cho thang đều

Thay vì một thang bán kính áp đều cho mọi surface (kiểu Material `4/8/12/16/28`), NotePay v3 dùng **góc bẻ**: 3 góc bo tròn, 1 góc gần như vuông (6px), luân phiên xoay chiều giữa các thẻ liên tiếp trong danh sách.

- `card`: `18px 18px 18px 6px` — góc dưới-trái bẻ nhọn.
- `card-alt`: `6px 18px 18px 18px` — soi gương, dùng cho thẻ AI Advisor hoặc xen kẽ trong feed để tạo nhịp điệu thị giác, tránh cảm giác "dập khuôn hàng loạt".

Đây là chi tiết dễ nhận ra nhất khi so sánh cạnh-cạnh với bất kỳ app Material 3 hay iOS nào — không app nào khác có góc bẻ này trừ khi cố tình sao chép NotePay.

---

## 4. Đổ bóng — "paper lift" thay cho elevation phẳng

Bỏ hoàn toàn mô hình "elevation dp" của Material (nơi độ cao chỉ là một con số trừu tượng) và bỏ luôn `elevation: 0px` tuyệt đối của bản Apple cũ. Thay bằng bóng đổ vật lý, mô phỏng ánh sáng chiếu chéo:

| Cấp | Dùng cho | Box-shadow |
|---|---|---|
| resting | chip, thẻ AI advisor | `0px 1px 2px rgba(31,27,22,0.06)` |
| raised | balance card, button | `0px 4px 10px rgba(31,27,22,.08), 0px 1px 2px rgba(31,27,22,.05)` |
| floating | nav bar, bottom sheet | `0px 10px 24px rgba(31,27,22,.12), 0px 2px 6px rgba(31,27,22,.06)` |
| pressed | trạng thái nhấn | phẳng hẳn — bóng biến mất tức thời, tạo cảm giác "ấn xuống giấy" |

Bóng luôn dùng màu mực ấm (`rgba(31,27,22,…)`) chứ không dùng đen thuần hay bóng có màu theo accent — giữ cảm giác tự nhiên như ánh sáng thật.

---

## 5. Card & chi tiết nhận diện riêng

- **Balance Hero Card**: có `accentEdge` — một dải mực đất nung dày 3px dọc theo cạnh trái card, như một dải bookmark. Chi tiết này không tồn tại ở bản Material lẫn bản iOS cũ.
- **Transaction Row**: đường phân cách giữa các dòng đổi từ hairline đặc sang **nét đứt** (`dashed 1px`), gợi liên tưởng tới đường xé của biên lai giấy.
- **Category Chip**: nền `surface-sunken` (giấy lõm nhẹ) + viền mảnh, thay vì fill xám phẳng kiểu Material system-fill.

---

## 6. Typography — ghép chữ mới

- **Fraunces** (serif ấm, có cá tính "viết tay hiện đại") cho tiêu đề lớn — thay cho Inter, vốn là lựa chọn an toàn/trung tính dùng trong hầu hết app hệ thống.
- **Manrope** (sans hình học, thân thiện) cho nội dung — thay cho Inter ở phần body, giữ dễ đọc nhưng có nét bo tròn riêng hơn.
- **JetBrains Mono** giữ lại cho số liệu tài chính — đây là lựa chọn đúng, không cần đổi vì tính đơn cách (tabular) là yêu cầu chức năng chứ không phải gu thẩm mỹ.

---

## 7. Nên và không nên

### Nên
- Luân phiên `card` / `card-alt` trong danh sách để tránh lặp góc bẻ cùng chiều liên tục.
- Dùng `accentEdge` chỉ trên các card "hero" hoặc card cần nhấn mạnh — không lạm dụng trên mọi surface.
- Giữ nguyên tắc chỉ 2 màu income/expense mang ý nghĩa tài chính; mọi màu khác (kể cả accent chính) không được dùng để biểu thị lãi/lỗ.

### Không nên
- Không quay lại xám lạnh hệ thống (`#F2F2F7`, `#E5E5EA`) hoặc đen/trắng tuyệt đối làm nền — phá vỡ cảm giác "giấy ấm".
- Không dùng bo góc đối xứng đều ở mọi card — làm mất đi chi tiết nhận diện cốt lõi của phiên bản này.
- Không thêm bóng màu theo accent (ví dụ bóng đất nung) — bóng chỉ dùng tông mực trung tính để giữ cảm giác vật lý tự nhiên.
