---
version: 2.0.0
name: NotePay iOS Monochrome & Pure Clarity
description: Single source of truth for NotePay Android design tokens, iOS-inspired monochrome palette, typography, elevation, motion, and interaction rules.

colors:
  # Light Mode Monochrome Canvas (~80%) - iOS System Grouped Baseline
  background: "#F2F2F7"          # Apple iOS System Grouped Background
  surface: "#FFFFFF"             # Apple Pure White Card / Secondary Grouped
  surface-subtle: "#F8F8FA"      # Subtle elevated container
  surface-container: "#E5E5EA"   # Apple System Fill / Quaternary
  surface-variant: "#E5E5EA"     # Subtle input container

  text-primary: "#000000"        # Apple Primary Label (#000000 / #1C1C1E)
  text-secondary: "#6C6C70"      # Apple Secondary Label (~5.1:1 WCAG AA)
  text-muted: "#8E8E93"          # Apple Tertiary / Placeholder Label

  border: "#D1D1D6"              # Apple System Gray 4 Border
  border-subtle: "#E5E5EA"       # Subtle divider line
  separator: "rgba(60, 60, 67, 0.29)" # Apple System Separator (0x4A3C3C43)

  # Light Mode Action & Primary (~15%) - Pure High-Contrast Black
  primary: "#000000"             # Pitch Black Action
  on-primary: "#FFFFFF"          # Pure White on Black
  primary-container: "#E5E5EA"   # Inactive / subtle button container
  on-primary-container: "#000000"

  secondary: "#3A3A3C"
  on-secondary: "#FFFFFF"
  secondary-container: "#E5E5EA"
  on-secondary-container: "#1C1C1E"

  cta: "#000000"                 # Primary Action: Solid Black Pill
  on-cta: "#FFFFFF"

  # Dark Mode Monochrome Canvas (~80%) - Apple True Black OLED
  dark-background: "#000000"     # Pure OLED True Black
  dark-surface: "#1C1C1E"        # Apple Dark Secondary Grouped Background
  dark-surface-subtle: "#242426" # Slightly elevated dark container
  dark-surface-container: "#2C2C2E" # Apple Dark Tertiary Fill
  dark-surface-variant: "#2C2C2E"

  dark-text-primary: "#FFFFFF"   # Pure Crisp White Label
  dark-text-secondary: "#8E8E93" # Apple Dark Secondary Label (~5.4:1)
  dark-text-muted: "#636366"     # Apple Dark Placeholder / Disabled

  dark-border: "#38383A"         # Apple Dark Separator Strong
  dark-border-subtle: "#2C2C2E"  # Apple Dark Subtle Border
  dark-separator: "rgba(84, 84, 88, 0.60)" # Apple Dark Separator (0x99545458)

  # Dark Mode Action & Primary (~15%) - Pure High-Contrast White
  dark-primary: "#FFFFFF"        # Crisp White Action
  dark-on-primary: "#000000"     # Pure Black on White
  dark-primary-container: "#2C2C2E"
  dark-on-primary-container: "#FFFFFF"

  dark-secondary: "#8E8E93"
  dark-on-secondary: "#000000"
  dark-secondary-container: "#2C2C2E"
  dark-on-secondary-container: "#FFFFFF"

  dark-cta: "#FFFFFF"            # Primary Action: Solid White Pill in Dark Mode
  dark-on-cta: "#000000"

  # Semantic Financial Status (~5% - Strictly for Cash Flow, Never Decorative)
  income: "#34C759"              # Apple Human Interface Guideline Green
  income-container: "#E8F8EE"
  dark-income: "#30D158"         # Apple Dark Vibrant Green
  dark-income-container: "#0B2E16"

  expense: "#FF3B30"             # Apple Human Interface Guideline Red
  expense-container: "#FEECEB"
  dark-expense: "#FF453A"        # Apple Dark Vibrant Red
  dark-expense-container: "#3D0C09"

  warning: "#FF9F0A"             # Apple Amber Warning
  warning-container: "#FFF9DB"
  dark-warning: "#FFD60A"
  dark-warning-container: "#3B3200"

  error: "#FF3B30"
  error-container: "#FFDAD6"
  dark-error: "#FF453A"
  dark-error-container: "#93000A"

typography:
  display-large:
    fontFamily: Inter
    fontSize: 34px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: -0.02em
  headline-large:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: 600
    lineHeight: 1.27
  title-large:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: 600
    lineHeight: 1.25
  title-medium:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: 600
    lineHeight: 1.33
  body-large:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: 400
    lineHeight: 1.29
  body-medium:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: 400
    lineHeight: 1.33
  caption:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.33
    letterSpacing: 0.02em
  eyebrow:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: 600
    lineHeight: 1.36
    letterSpacing: 0.06em
    textTransform: uppercase

  # Monospace Tabular Numerics for Financial Exactness
  amount-hero:
    fontFamily: JetBrains Mono
    fontSize: 34px
    fontWeight: 700
    lineHeight: 1.18
    letterSpacing: -0.01em
  amount-card:
    fontFamily: JetBrains Mono
    fontSize: 24px
    fontWeight: 700
    lineHeight: 1.25
  amount-row:
    fontFamily: JetBrains Mono
    fontSize: 16px
    fontWeight: 600
    lineHeight: 1.25
  number-tabular:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.28

rounded:
  xs: 4px       # indicator dot / micro tag
  sm: 8px       # corner8: small chip / badge / popup menu
  md: 12px      # corner12: input field / secondary card
  lg: 16px      # corner16: prominent surface / transaction row / modal
  xl: 20px      # corner20: large picker sheet / glass panel
  xxl: 24px     # corner24: hero balance card / bottom sheet top corners
  capsule: 9999px # capsule (50%): tab indicator / action pill button
  full: 9999px  # circle: icon button / category avatar / FAB

spacing:
  xs: 4px       # spaceExtraSmall
  sm: 8px       # spaceSmall / paddingSmall
  md: 16px      # spaceMedium / paddingMedium (primary screen gutter)
  lg: 24px      # spaceLarge / paddingLarge (section gap)
  xl: 32px      # spaceExtraLarge (hero top spacing)
  xxl: 48px     # empty state & splash spacing

motion:
  fast: 120ms
  normal: 200ms
  emphasized: 280ms
  sheet-spring: 350ms
  easing-standard: cubic-bezier(0.2, 0.0, 0, 1.0) # FastOutSlowIn
  spring-bounce:
    dampingRatio: 0.65
    stiffness: 400.0

components:
  primary-action-button:
    backgroundColor: "{colors.cta}"
    textColor: "{colors.on-cta}"
    rounded: "{rounded.capsule}"
    height: 54px
  liquid-navigation-bar:
    backgroundColor: "{colors.surface}"
    backdropBlur: 20px
    indicatorColor: "{colors.primary}"
    height: 64px
    rounded: "{rounded.capsule}"
  balance-hero-card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.xxl}"
    padding: "{spacing.lg}"
    elevation: 0px
    border: "1px solid {colors.border-subtle}"
  transaction-item-row:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
    swipeThreshold: 72px
  amount-display-hero:
    typography: "{typography.amount-hero}"
    textColor: "{colors.text-primary}"
    currencySymbolColor: "{colors.text-muted}"
  category-chip:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.capsule}"
    height: 36px
  ai-advisor-card:
    backgroundColor: "{colors.surface-subtle}"
    borderColor: "{colors.border}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.xl}"
---

# NotePay Design Specification: iOS Monochrome & Pure Clarity

## 1. Philosophy: Pure iOS Minimalism
NotePay follows the design ethos of **Apple iOS System Financial Applications**: no rainbow gradients, no distracting primary brand colors, and zero decorative visual clutter.

```text
Light Mode:
Pure Black Action (#000000) on White Card (#FFFFFF) over System Gray (#F2F2F7)
                                  ↓
Dark Mode:
Crisp White Action (#FFFFFF) on Dark Surface (#1C1C1E) over True Black OLED (#000000)
                                  ↓
Semantics (Only for cashflow):
Apple Green (#34C759 / #30D158) for Income | Apple Red (#FF3B30 / #FF453A) for Expense
```

### Key Principles
1. **Monochrome Dominance**: The primary visual identity is high-contrast Black & White. All primary buttons, tab indicators, active states, and title headers use pure black in light mode and pure white in dark mode.
2. **True Black OLED Dark Mode**: Background is `#000000`, surfaces are `#1C1C1E`. Saves battery on OLED displays and offers seamless edge-to-edge immersion.
3. **Tabular Monospace Numerics**: Money amounts are strictly rendered in `JetBrains Mono` with identical digit widths. Numbers do not jitter during transitions or live calculations.
4. **Restraint over Decoration**: Color is treated as information, not decoration. Only two colors are permitted to stand out: Income Green and Expense Red.

---

## 2. Color System: The iOS Black & White Architecture

### Canvas & Surface Structure (~80%)
- **Light Mode**:
  - `background`: `#F2F2F7` (Apple System Grouped Background)
  - `surface`: `#FFFFFF` (Pure White Card / Modal Surface)
  - `surface-container`: `#E5E5EA` (Apple System Gray 5 Fill)
  - `text-primary`: `#000000` (High contrast, 15.8:1 ratio)
  - `text-secondary`: `#6C6C70` (Apple Secondary Label, 5.1:1 ratio, WCAG AA compliant)
  - `separator`: `#D1D1D6` / `rgba(60, 60, 67, 0.29)`
- **Dark Mode**:
  - `dark-background`: `#000000` (Apple OLED True Black)
  - `dark-surface`: `#1C1C1E` (Apple System Dark Surface)
  - `dark-surface-container`: `#2C2C2E` (Apple System Dark Gray 4 Fill)
  - `dark-text-primary`: `#FFFFFF` (High contrast, 16.2:1 ratio)
  - `dark-text-secondary`: `#8E8E93` (Apple Dark Secondary Label, 5.4:1 ratio)
  - `dark-separator`: `#38383A` / `rgba(84, 84, 88, 0.60)`

### Primary Action & Active Accent (~15%)
- **Light Mode Action**: Solid Black (`#000000`) with crisp White text/icon (`#FFFFFF`).
- **Dark Mode Action**: Solid White (`#FFFFFF`) with pitch Black text/icon (`#000000`).
- **Inactive / Ghost States**: Neutral Gray Container (`#E5E5EA` Light / `#2C2C2E` Dark).

### Semantic Financial Status (~5%)
- **Income (Cash In)**: `#34C759` (Light) / `#30D158` (Dark).
- **Expense (Cash Out)**: `#FF3B30` (Light) / `#FF453A` (Dark).
- **Warning (Threshold approaching)**: `#FF9F0A` (Light) / `#FFD60A` (Dark).
- *Strict Rule*: No decorative buttons, backgrounds, or app bars may use red, green, or blue. These colors are strictly reserved for cashflow values and status badges.

---

## 3. Typography: Dual-Font Architecture

NotePay pairs `Inter` for human prose with `JetBrains Mono` for financial numeracy:

| Token | Font | Size / Weight | Line Height | Usage |
|---|---|---|---|---|
| `amount-hero` | JetBrains Mono | 34sp / Bold (700) | 1.18 | Large transaction amount input |
| `amount-card` | JetBrains Mono | 24sp / Bold (700) | 1.25 | Primary balance on Hero Card |
| `amount-row` | JetBrains Mono | 16sp / SemiBold (600) | 1.25 | Transaction list item cash values |
| `number-tabular` | JetBrains Mono | 14sp / Medium (500) | 1.28 | Percentages, dates, account numbers |
| `display-large` | Inter | 34sp / SemiBold (600) | 1.20 | Large screen titles, month header |
| `headline-large` | Inter | 22sp / SemiBold (600) | 1.27 | Section headers, sheet titles |
| `title-large` | Inter | 20sp / SemiBold (600) | 1.25 | TopAppBar titles, dialog titles |
| `body-large` | Inter | 17sp / Regular (400) | 1.29 | Default form fields, body text |
| `body-medium` | Inter | 15sp / Regular (400) | 1.33 | Secondary descriptions, note text |
| `caption` | Inter | 12sp / Medium (500) | 1.33 | Timestamps, Vietnamese words spelling |
| `eyebrow` | Inter | 11sp / SemiBold (600) | 1.36 | Category badges, uppercase tags |

---

## 4. Spacing & Shapes

### Spatial Scale (4dp Core Rhythm)
- `4dp (xs)`: Micro gaps between badge icon and text.
- `8dp (sm)`: Compact internal chip padding, small control margins.
- `16dp (md)`: **Primary Screen Gutter**, standard form field spacing.
- `24dp (lg)`: Card internal padding, section breaks.
- `32dp (xl)`: Hero card top margin, empty state gaps.

### Shape Scale (`AppShapes`)
- `8dp (corner8)`: Small filter chips, menu popovers.
- `12dp (corner12)`: Form inputs, date selector cells.
- `16dp (corner16)`: Transaction list items, card surfaces, dialogs.
- `20dp (corner20)`: Category grid container, bottom action panels.
- `24dp (corner24)`: Hero balance card, bottom sheet top corners.
- `Capsule (50% / 9999px)`: Primary CTA button, sliding tab indicator pills.
- `Circle (9999px)`: Category icon avatars, round action buttons.

---

## 5. Components & iOS Monochrome Behavior

### 1. Primary Action Button (`LiquidButton`)
- In Light Mode: Background `#000000`, Text/Icon `#FFFFFF`.
- In Dark Mode: Background `#FFFFFF`, Text/Icon `#000000`.
- Shape: Full Capsule (`AppTheme.shapes.capsule`).
- Minimum height: `54dp`.
- Press animation: Subtle scaling down to `0.97f` on touch down with fast spring release.

### 2. Balance Hero Card (`BalanceCard`)
- Clean white card (`#FFFFFF`) in Light Mode, dark surface (`#1C1C1E`) in Dark Mode.
- No heavy colorful gradients. Thin border stroke (`1.dp` solid `#D1D1D6` / `#38383A`).
- Large balance number in `JetBrains Mono` (`amount-card`).
- Income/Expense indicators using subtle background tint with green/red indicator arrows.

### 3. Transaction Type Selector (Income vs Expense)
- Sliding pill indicator moving horizontally over a segmented track.
- Active tab background is pure Black (Light) / White (Dark).
- Inactive text is subtle gray (`#6C6C70` / `#8E8E93`).

### 4. Liquid Navigation Tab Bar (`NotePayBottomBar`)
- Floating pill dock above the bottom edge.
- Background uses subtle frosted glass (`backdropBlur: 20px`) with semi-transparent white/dark surface.
- Active tab indicator: Solid Black/White pill slider.

---

## 6. Do's and Don'ts

### Do's
- **DO** use `MaterialTheme.colorScheme.primary` for primary action buttons so they automatically switch between Black (Light Mode) and White (Dark Mode).
- **DO** use `MaterialTheme.colorScheme.onPrimary` for text/icons placed inside primary buttons.
- **DO** keep backgrounds neutral: `#F2F2F7` for Light Mode and `#000000` for Dark Mode.
- **DO** reserve Green (`#34C759`) and Red (`#FF3B30`) strictly for cash flow data (Income / Expense).

### Don'ts
- **DON'T** introduce colored brand accents (e.g. green, blue, purple buttons). Keep primary actions strictly Black and White.
- **DON'T** hardcode raw hex colors like `Color(0xFF1B7F4F)` inside Composables. Always reference semantic tokens.
- **DON'T** apply drop shadows with color tint. Use neutral elevation or subtle 1dp hairline borders (`#D1D1D6` / `#38383A`).
