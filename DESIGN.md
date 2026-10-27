---
version: 4.2.0
name: NotePay — Sand & Ink (Editorial)
description: Bộ design token v4 cho NotePay. Phong cách biên tập (editorial) lấy cảm hứng từ các app ảnh lớn, chữ hiển thị lớn kết hợp serif Fraunces, thẻ mực đen làm điểm nhấn dữ liệu, màu cát làm điểm sáng, thanh điều hướng pill đen nổi. Thay thế v3 "Ledger Ink & Warm Paper".

colors:
  # ==== LIGHT ====
  background: "#EFEFEF"            # nền trang, xám trung tính nhạt
  surface: "#FFFFFF"               # thẻ nội dung
  surface-field: "#F4F1EC"         # ô nhập liệu, dải thu/chi trong thẻ ví
  ink: "#0B0B0B"                   # thẻ nhấn (số dư, ngân sách), nút chính, thanh điều hướng
  ink-raised: "#262626"            # nút phụ nằm trong thẻ ink
  sand: "#F2D3A8"                  # điểm sáng: mục đang chọn trong ink, đoạn thanh ngân sách, huy hiệu
  graphite: "#3A3733"              # đoạn thanh phân bổ thứ hai
  clay: "#C98A63"                  # màu nhận diện ví / avatar

  # ==== DATA SERIES ====
  # Biểu đồ và thanh phân bổ. Mỗi chuỗi luôn đi kèm nhãn + giá trị, không bao giờ chỉ dựa vào màu.
  data-1: "#F2D3A8"                # sand
  data-2: "#3A3733"                # graphite
  data-3: "#C98A63"                # clay
  data-4: "#7CC4FF"                # sky, chuỗi thứ tư

  text-primary: "#111111"
  text-secondary: "#5C5C5C"        # >= 5.8:1 trên background và surface
  text-muted: "#767676"            # placeholder/disabled, >= 4.5:1 trên surface
  on-ink: "#FFFFFF"
  on-ink-muted: "#BDBDBD"          # nhãn phụ trên thẻ ink
  on-sand: "#111111"
  border: "#BDBDBD"
  border-soft: "#CFCFCF"

  income: "#2F6B3A"
  on-ink-income: "#8FD99B"
  expense: "#A63B2E"
  on-ink-expense: "#FF9C8C"
  warning: "#8A5A00"
  warning-container: "#FBF1DC"

  # ==== DARK ====
  dark-background: "#141414"
  dark-surface: "#1D1D1D"
  dark-card: "#242424"
  dark-nav: "#2A2A2A"
  dark-text-primary: "#F2F2F2"
  dark-text-secondary: "#A8A8A8"
  dark-border: "#3A3A3A"
  dark-nav-active: "#F2F2F2"
  dark-nav-active-text: "#111111"
  dark-income: "#7FCB8C"
  dark-expense: "#FF8F7E"
  dark-warning: "#F2C26B"
  dark-warning-container: "#3A2D12"
  dark-ink: "#262626"              # bề mặt ink ở dark mode
  dark-ink-border: "#3A3A3A"       # viền 1px bắt buộc kèm dark-ink
  dark-text-muted: "#8A8A8A"
  dark-data-1: "#F2D3A8"
  dark-data-2: "#8A8279"           # graphite phải sáng lên ở dark
  dark-data-3: "#D19A72"
  dark-data-4: "#7CC4FF"
  # sand giữ nguyên ở dark mode làm điểm sáng

typography:
  display-serif:                   # tiêu đề mục và từ nhấn, chỉ dùng cho chữ, không dùng cho số tiền
    fontFamily: "Fraunces"         # Instrument Serif bị loại: không có bộ ký tự vietnamese
    fontStyle: normal
    fontSize: 26px
    fontWeight: 400
    lineHeight: 1.1
  display-sans:
    fontFamily: "Inter"
    fontSize: 28px
    fontWeight: 500
    lineHeight: 1.1
    letterSpacing: -0.02em
  amount-hero:
    fontFamily: "Inter"
    fontSize: 44px
    fontWeight: 500
    lineHeight: 1.1
    letterSpacing: -0.03em
    fontFeatureSettings: "tnum"
  amount-card:
    fontFamily: "Inter"
    fontSize: 20px
    fontWeight: 600
    fontFeatureSettings: "tnum"
  amount-row:
    fontFamily: "Inter"
    fontSize: 15px
    fontWeight: 600
    fontFeatureSettings: "tnum"
  title:
    fontFamily: "Inter"
    fontSize: 16px
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "Inter"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.4
  caption:
    fontFamily: "Inter"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.35
  eyebrow:
    fontFamily: "Inter"
    fontSize: 11px
    fontWeight: 600
    letterSpacing: 0.08em
    textTransform: uppercase

radius:
  xl: 28px                         # thẻ ink, thẻ nhóm giao dịch, thẻ ảnh
  lg: 24px                         # thẻ phụ, cảnh báo, thẻ ví chi tiết
  md: 20px                         # thẻ nhóm ngày, thẻ ví
  sm: 16px                         # ô nhập, dải thu/chi
  xs: 12px                         # thanh phân bổ
  sheet-top: "28px 28px 0 0"
  capsule: 999px

spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px

elevation:
  flat: "none"                     # thẻ nằm phẳng trên nền xám, phân tầng bằng màu
  floating: "0px 10px 24px rgba(0,0,0,0.20)"   # thanh điều hướng nổi
  sheet-card: "0px 10px 24px rgba(0,0,0,0.14)" # thẻ xếp chồng ở đáy
  scrim: "rgba(0,0,0,0.45)"        # nền mờ sau bottom sheet

motion:
  fast: 130ms
  normal: 220ms
  emphasized: 300ms
  sheet-spring: 380ms
  easing-standard: cubic-bezier(0.22, 1, 0.36, 1)
  spring-bounce:
    dampingRatio: 0.7
    stiffness: 320.0

components:
  ink-balance-card:
    backgroundColor: "{colors.ink}"
    darkBackgroundColor: "{colors.dark-ink}"
    darkBorder: "1px solid {colors.dark-ink-border}"
    textColor: "{colors.on-ink}"
    radius: "{radius.xl}"
    padding: "22px 20px"
  segmented-bar:                   # ngân sách / phân bổ
    height: 44px
    radius: "{radius.xs}"
    gap: 2px
    segments: ["{colors.sand}", "{colors.graphite}", "hatched (phần còn lại)"]
  floating-nav:
    backgroundColor: "{colors.ink}"
    activeBackground: "{colors.on-ink}"
    activeText: "{colors.ink}"
    height: 64px
    radius: "{radius.capsule}"
    shadow: "{elevation.floating}"
    bottomInset: 22px
  primary-button:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.on-ink}"
    radius: "{radius.capsule}"
    height: 56px
  button-sand:                     # nút chính nằm trong thẻ ink
    backgroundColor: "{colors.sand}"
    textColor: "{colors.on-sand}"
    radius: "{radius.capsule}"
    height: 48px
  filter-chip:
    selected: "surface + viền 1px {colors.border}, chữ {colors.text-primary}"
    unselected: "trong suốt, chữ {colors.text-secondary}"
    radius: "{radius.capsule}"
    minHeight: 48px
  wallet-selector-chip:            # đổi ví ở thanh trên cùng màn Home
    backgroundColor: "{colors.surface} @ 72%"
    border: "1px solid {colors.hairline}"
    textColor: "{colors.text-primary}"
    radius: "{radius.xs}"
    height: 40px
  day-group-card:                  # nhóm giao dịch theo ngày, các dòng cách nhau bằng khoảng trắng
    backgroundColor: "{colors.surface}"
    radius: "{radius.md}"
    header: "{typography.eyebrow}"
  swiped-row-action:               # nền lộ ra khi vuốt dòng giao dịch
    editBackground: "{colors.ink}"           # dark: {colors.sand}
    editText: "{colors.on-ink}"              # dark: {colors.on-sand}
    deleteBackground: "{colors.expense}"     # dark: {colors.dark-expense}
    deleteText: "{colors.on-ink}"
    width: 96px
  budget-alert-card:
    backgroundColor: "{colors.warning-container}"
    textColor: "{colors.text-primary}"
    radius: "{radius.lg}"
    requires: "icon + chữ, không chỉ màu"
  bottom-sheet:
    backgroundColor: "{colors.surface}"
    radius: "{radius.sheet-top}"
    scrim: "{elevation.scrim}"
---

# NotePay Design Specification v4: Sand & Ink (Editorial)

## 1. Hướng đi

NotePay v4 chuyển từ ẩn dụ "sổ ghi chép giấy ấm" (v3) sang phong cách **biên tập hiện đại**: nền trung tính, **một thẻ mực đen** làm tâm điểm dữ liệu trên mỗi màn, **màu cát** làm điểm sáng, chữ hiển thị lớn kết hợp **serif Fraunces** cho tiêu đề, và **thanh điều hướng pill đen** nổi ở đáy.

Lý do chuyển: dữ liệu tài chính cần số lớn, tương phản cao và phân cấp rõ. Thẻ mực đen với chữ trắng cho tương phản trên 19:1, giúp con số quan trọng nhất luôn là thứ đầu tiên được nhìn thấy.

## 2. Hệ màu

| Vai trò | Light | Dark | Dùng cho |
|---|---|---|---|
| background | `#EFEFEF` | `#141414` | Nền trang |
| surface | `#FFFFFF` | `#1D1D1D` | Thẻ nội dung |
| ink | `#0B0B0B` | `#0B0B0B` | Thẻ nhấn, nút chính, nav |
| sand | `#F2D3A8` | `#F2D3A8` | Điểm sáng, đoạn ngân sách, huy hiệu |
| text-primary | `#111111` | `#F2F2F2` | Chữ chính |
| text-secondary | `#5C5C5C` | `#A8A8A8` | Chữ phụ |
| income | `#2F6B3A` | `#7FCB8C` | Thu nhập |
| expense | `#A63B2E` | `#FF8F7E` | Chi tiêu |
| warning | `#8A5A00` trên `#FBF1DC` | `#F2C26B` trên `#3A2D12` | Cảnh báo ngân sách |

Tương phản đã kiểm tra (WCAG): chữ chính trên surface 18.9:1, chữ phụ trên background 5.8:1, trắng trên ink 19.7:1, `on-ink-muted` trên ink 10.5:1, income/expense trên surface 6.4:1, cảnh báo 5.3:1 (light) và 8.1:1 (dark). Mọi cặp đều đạt 4.5:1 trở lên.

**Quy tắc màu**
- `income` và `expense` là hai màu **duy nhất** mang nghĩa dòng tiền, không dùng trang trí.
- `sand` chỉ làm điểm sáng (mục chọn, đoạn thanh, huy hiệu), không dùng làm nền cả vùng lớn.
- Chữ trên thẻ ink dùng biến thể `*-on-ink` (sáng hơn) để đủ tương phản.
- Trạng thái (ngân sách an toàn/cảnh báo/vượt) luôn có **icon + chữ + màu**, không chỉ dựa vào màu.

## 3. Typography

- **Inter** (400, 500, 600) cho toàn bộ giao diện và số tiền, bật `tnum` (số cùng độ rộng) cho mọi số liệu.
- **Fraunces** (dáng đứng) chỉ cho tiêu đề mục ("Giao dịch gần đây") và từ nhấn trong tiêu đề lớn. **Không dùng cho số tiền.**
  - *Lý do lệch so với bản mock:* Instrument Serif chỉ phát hành bộ ký tự `latin` và `latin-ext`, không có `vietnamese`. Trong Compose, glyph thiếu sẽ rơi sang font dự phòng theo từng ký tự, khiến một từ tiếng Việt hiển thị bằng hai kiểu chữ. Fraunces có bộ `vietnamese` nên được giữ. Dáng nghiêng (italic) chưa đóng gói nên tạm dùng dáng đứng.
- Cỡ chữ nhỏ nhất 12sp. Nhãn phụ trên thẻ ink dùng `on-ink-muted`, không dùng xám đậm.
- Số dư chính: `amount-hero` 44px / 500 / letter-spacing -0.03em.

## 4. Bo góc và khoảng cách

Thang bo góc mới (đồng đều, không còn "góc bẻ" của v3): `28 / 24 / 20 / 16 / 12` và `capsule`.
- Thẻ lớn (ink, nhóm giao dịch, ảnh): 28. Thẻ phụ và cảnh báo: 24. Thẻ ví nhỏ: 20. Ô nhập: 16. Thanh phân bổ: 12. Nút và chip: capsule.
- Khoảng cách theo thang 4: `4 / 8 / 16 / 24 / 32`. Lề màn hình 16. Chừa đáy 120 cho thanh điều hướng nổi.

## 5. Chiều sâu

- Thẻ nằm **phẳng** trên nền xám, phân tầng bằng màu (trắng trên xám, ink trên xám), không đổ bóng.
- Chỉ phần tử nổi mới có bóng: thanh điều hướng (`floating`), thẻ xếp chồng ở đáy (`sheet-card`).
- Bottom sheet dùng nền mờ `scrim` 45%, không dùng bóng.
- Màn Home **không** dùng tấm `sheet-top` trắng phủ nửa dưới: toàn trang là `background`, chỉ dải backdrop ở trên tạo tầng.

## 6. Thành phần chính

- **Ink balance card**: số dư lớn, huy hiệu thay đổi %, hàng nút (nút sand cho hành động chính, nút `ink-raised` cho phụ). Mỗi màn chỉ có một thẻ ink làm tâm điểm. Ở dark mode thẻ dùng `dark-ink` `#262626` kèm viền 1px `#3A3A3A` (ink `#0B0B0B` trên nền `#141414` chỉ đạt 1.07:1 và biến mất). Nền thẻ **luôn** lấy từ token `ink`, không dùng ảnh nền. Dòng thu/chi nằm trực tiếp trên nền ink (không dùng dải nền con) và dùng `on-ink-income` / `on-ink-expense` để đủ tương phản.
- **Segmented bar**: ngân sách hoặc phân bổ. Đoạn đầu `sand`, đoạn hai `graphite`, phần còn lại kẻ chéo. Nhãn phần trăm nằm trong đoạn nếu đủ rộng.
- **Floating nav**: capsule đen, mục đang chọn là capsule trắng, chữ nhãn rõ (không chỉ icon).
- **Day group card**: giao dịch gom theo ngày trên nền trang xám; mỗi ngày là một thẻ `surface` bo `md` (20), tiêu đề nhóm dùng `eyebrow` ở ngoài thẻ, các dòng bên trong cách nhau bằng khoảng trắng, không dùng đường kẻ. Dòng giao dịch xếp kiểu sổ cái: tiêu đề đậm, dòng phụ "Danh mục • Ví", số tiền `amount-row` căn phải (thu có dấu `+` và màu `income`, chi có dấu `−` và `text-primary`).
- **Swipe reveal row**: vuốt ngang để **lộ** nền hành động chứ không tự thực thi. Hai vùng nền rộng 96px: bên trái "Sửa" (nền `ink` / `sand` ở dark), bên phải "Xóa" (nền `expense`). Một lần chạm để thực thi; chạm ra ngoài để đóng. Tổng offset không vượt hai vùng 96px, có ngưỡng rung 50% chiều rộng.
- **Wallet selector chip**: chip ở thanh trên cùng màn Home (icon + tên ví + mũi tên xuống) mở bottom sheet chọn ví, có mục "Tất cả ví" để quay về tổng. Chip cao 40, bo `xs`, nền `surface` 72% + viền 1px `hairline`; **không dùng viền đậm/đen để đánh dấu ví đang chọn** — trạng thái chọn thể hiện trong sheet.
- **Top backdrop**: dải màu trang trí phía sau khối hero, chiếm khoảng 1/3 trên màn hình rồi tan dần vào `background` (điểm dừng 88%). Chỉ dùng gradient từ token `sand` / `clay` / `ink` với alpha thấp, không dùng ảnh bitmap, không đặt nội dung dày lên vùng này.
- **Wallet card** (màn Ví): icon 48, tên, huy hiệu "Đang dùng" (sand), số giao dịch, dải thu/chi nền `surface-field`.
- **Budget alert card**: nền `warning-container`, icon cảnh báo, lời khuyên có số tiền in đậm, nút đóng vùng chạm 48, thanh tiến độ.
- **Hero photo card** (tùy chọn, màn chào hoặc giới thiệu): chữ trên ảnh phải có lớp phủ hoặc làm mờ nền. Không đặt ảnh trên màn dữ liệu dày (danh sách giao dịch, thống kê).

## 7. Nên và không nên

### Nên
- Một thẻ ink làm tâm điểm cho mỗi màn, số lớn nhất là số quan trọng nhất.
- Dùng serif Fraunces tiết chế: tối đa vài từ mỗi tiêu đề.
- Giữ vùng chạm tối thiểu 48dp, nút icon có `contentDescription`.
- `MaterialTheme.colorScheme` là bề mặt màu lúc chạy; `AppTheme.colors` chỉ bổ sung ngữ nghĩa riêng của v4 (`ink`, `inkRaised`, `inkBorder`, `onInk`, `onInkMuted`, `sand`, `onSand`, `graphite`, `clay`, `hairline`, `chartSeries`). Không dựng thêm bảng màu song song thứ ba.

### Không nên
- Không dùng chữ serif cho số tiền hoặc đoạn văn dài.
- Không dùng màu thu/chi để trang trí, không dùng màu đơn lẻ để báo trạng thái.
- Không đặt chữ trực tiếp lên ảnh nếu không có lớp phủ.
- Không thêm bóng đổ cho thẻ phẳng, không dùng gradient màu làm nền thẻ (gradient chỉ được dùng cho dải backdrop trang trí phía sau hero, alpha thấp và tan vào `background`).

## 8. Ánh xạ từ v3

| v3 (Ledger Ink) | v4 (Sand & Ink) |
|---|---|
| Nền giấy `#F7F3EC` | `#EFEFEF` |
| Thẻ `#FFFDF8` | `#FFFFFF` |
| Mực chữ `#1F1B16` / phụ `#6B6255` | `#111111` / `#5C5C5C` |
| Accent đất nung `#B5502E` (nút, nav) | Ink `#0B0B0B` (nút, nav) + sand `#F2D3A8` (điểm sáng) |
| Income `#4C7A4A` | `#2F6B3A` |
| Expense `#A63B2E` | giữ nguyên |
| Bo góc bất đối xứng 18/6 | Bo đều 28 / 24 / 20 / 16 |
| Đường kẻ đứt giữa dòng | Khoảng trắng, không đường kẻ |
| Fraunces + Manrope + JetBrains Mono | Fraunces (display) + Inter (`tnum`) |
| Nav trắng nổi, chỉ báo đất nung | Nav ink, mục chọn capsule trắng |
| Dải mực 3px bên trái thẻ hero | Thẻ ink nguyên khối |

## 9. Giá trị suy ra (v4 không định nghĩa, đã chốt)

| Token | Light | Dark | Ghi chú |
|---|---|---|---|
| `text-muted` | `#767676` | `#8A8A8A` | placeholder/disabled |
| `tertiary` / `tertiary-container` | `#C98A63` / `#F5E9DC` | `#E0B48F` / `#3A2A1C` | clay làm màu thứ ba cho M3 |
| `error-container` | `#F7E4DF` | `#3D1B14` | nền cảnh báo chi tiêu |
| `inverse-surface` | `#0B0B0B` | `#F2F2F2` | Snackbar của M3 |
| `surface-tint` | `#FFFFFF` | `#1D1D1D` | bằng chính surface: giao diện phẳng, không ám màu theo elevation |
| `dark-ink` | — | `#262626` + viền `#3A3A3A` | thẻ ink ở dark mode |
| `dark-data-2` | — | `#8A8279` | graphite phải sáng lên ở dark (giá trị light chỉ đạt 1.6:1 trên `#141414`) |

## 10. Ánh xạ Material 3 `ColorScheme`

`MaterialTheme.colorScheme` là bề mặt màu lúc chạy; `AppTheme.colors` bổ sung ngữ nghĩa v4. Bảng ánh xạ cốt lõi:

| Vai trò M3 | Light | Dark | Dùng cho |
|---|---|---|---|
| `primary` | `#0B0B0B` (ink) | `#F2D3A8` (sand) | nút chính, chỉ báo mục đang chọn |
| `primaryContainer` | `#F2D3A8` | `#3A2F1F` | huy hiệu, chip đang chọn |
| `secondary` | `#3A3733` | `#B8AFA4` | hành động phụ |
| `surface` / `surfaceVariant` | `#FFFFFF` / `#F4F1EC` | `#1D1D1D` / `#242424` | thẻ nội dung, ô nhập |
| `onSurfaceVariant` | `#5C5C5C` | `#A8A8A8` | chữ phụ |
| `outline` / `outlineVariant` | `#BDBDBD` / `#CFCFCF` | `#4A4A4A` / `#3A3A3A` | viền |
| `error` / `errorContainer` | `#A63B2E` / `#F7E4DF` | `#FF8F7E` / `#3D1B14` | chi tiêu, cảnh báo |

> **Trạng thái:** v4 đã triển khai ở tầng token (`Color.kt`, `AppColors.kt`, `AppShapes.kt`, `Type.kt`, `AppTypography.kt`) và ở tầng thành phần cho **thẻ ink hero**, **chip chọn ví trên thanh trên**, **backdrop trên**, **thẻ nhóm ngày theo kiểu sổ cái** và **vuốt lộ nút Sửa/Xóa** trên màn Home. Thẻ ví mini 150×92 đã bị gỡ khỏi màn Home (chọn ví nằm ở chip thanh trên + bottom sheet), tấm trắng `sheet-top` phủ nửa dưới cũng đã gỡ — trang dùng nền `background` xám. Còn ở v3: thanh điều hướng capsule mực, thanh phân bổ có gạch chéo, thẻ ví chi tiết ở màn Ví. Palette id: `"sand"` (mặc định); `"ios"` và `"dynamic"` giữ nguyên; giá trị `"ledger"` đã lưu được ánh xạ sang `"sand"`. Ảnh nền thẻ ví/theo ví đã bỏ: thẻ dùng nền token của theme.