# Onboarding NotePay (3 bước tài chính)

## Goal
Thay `AuthWelcomeScreen` bằng onboarding 3 bước cho NotePay: Tiền tệ & Ngôn ngữ → Ví → Nhắc nhở. Toàn bộ nằm ở `ui/feature/onboarding`, dùng `AppTheme` + Hilt.

## Hiện trạng (đã verify)
- 7 file mang `package com.smartreminder.ui.onboarding*` nhưng nằm trong `com/notepay/ui/feature/onboarding` → **không compile**.
- Import chết: `com.smartreminder.R`, `CueTheme`, `CueSpacing`, `SmartReminderTheme`, `UserGoal`, `UserPreferencesRepository`, `UserPreferencesSyncCoordinator`, `TimeCalculator`. Không tồn tại trong repo.
- Token `CueTheme` không có ở `AppColors`: `accent`, `accentStrong`, `accentContainer`, `cta`, `onCta`, `surfaceSubtle`.
- Thiếu toàn bộ string `onboarding_*`, `goal_*`, `timeline_*`; chỉ có `action_back`.
- NotePay dùng Hilt (`@HiltViewModel`), không dùng ViewModelFactory.
- Ví mặc định đã được seed sẵn (`SeedCallback` → `Wallet.default()` = "Tiền mặt"), nên bước 2 sửa ví có sẵn, không tạo mới.

## Bảng map token (dùng cho mọi task UI)
| Cue (chết) | NotePay |
|---|---|
| `CueTheme.colors.accent` / `accentStrong` / `cta` | `AppTheme.colors.primary` |
| `CueTheme.colors.onCta` | `MaterialTheme.colorScheme.onPrimary` |
| `CueTheme.colors.accentContainer` | `MaterialTheme.colorScheme.primaryContainer` |
| `CueTheme.colors.surfaceSubtle` | `AppTheme.colors.surfaceSunken` |
| `CueTheme.colors.{background,surface,textPrimary,textSecondary,textMuted,border,borderStrong}` | giữ nguyên tên, đổi `CueTheme` → `AppTheme` |
| `CueSpacing.Xs/Sm/Md/Lg/Xl` | `AppTheme.dimensions.spaceExtraSmall/Small/Medium/Large/ExtraLarge` |
| `SmartReminderTheme` (preview) | `NotePayTheme` |

## Tasks

- [ ] **1. Đổi package 7 file** — `com.smartreminder.ui.onboarding` → `com.notepay.ui.feature.onboarding`; `.components` / `.steps` tương ứng. Import nội bộ đổi theo. → Verify: `grep -rn "smartreminder" app/src/main/java/com/notepay/ui/feature/onboarding` trả về rỗng.

- [ ] **2. Thay token theme** — áp bảng map trên cho `OnboardingScreen.kt`, `components/OnboardingComponents.kt`, `steps/Step1*.kt`, `steps/Step2*.kt`. Xoá import `CueTheme`, `CueSpacing`, `SmartReminderTheme`. → Verify: `grep -rn "Cue\(Theme\|Spacing\)\|SmartReminderTheme" app/src/main/java/com/notepay/ui/feature/onboarding` rỗng.

- [ ] **3. Viết lại state & action** — `OnboardingUiState.kt`: `OnboardingStep{CURRENCY, WALLET, REMINDER}`, field `currencyCode`, `languageCode`, `walletName`, `walletIconKey`, `reminderEnabled`, `reminderHour`, `reminderMinute`, `isSaving`, `saveError`. `OnboardingAction.kt`: `SelectCurrency`, `SelectLanguage`, `SelectWalletIcon`, `UpdateWalletName`, `ToggleReminder`, `UpdateReminderTime`, `NavigateToStep`, `Complete`. → Verify: file chỉ import `com.notepay.*`, không còn `UserGoal`.

- [ ] **4. Viết lại ViewModel + bỏ Factory** — `OnboardingViewModel.kt` thành `@HiltViewModel` / `@Inject constructor(walletRepository, appSettingsDataStore, @ApplicationContext context)`.
  - **Hydrate**: đọc DataStore (`first()`) + `walletRepository.observeActive().firstOrNull()`. Nếu ví `null`, hiển thị default trên UI (`"Tiền mặt"` / `Wallet.ICON_CASH`) nhưng **giữ `existingWalletId = null`** — không tự insert.
  - **`Complete` theo thứ tự cố định**: ① `withTimeoutOrNull(1_000) { observeActive().first { it != null } }` chờ `SeedCallback` commit; ② upsert ví — nếu vẫn `null` thì `Wallet.default().copy(name = …, iconKey = …)`, nếu có thì `existing.copy(...)` để **không tạo ví thứ hai**; ③ `setActive(id)`; ④ `setCurrencyCode`; ⑤ `setDailyReminderEnabled` + `setReminderTime` + `ReminderScheduler.scheduleDailyReminder`; ⑥ phát `isCompleted = true`.
  - **Không** gọi `LocaleHelper.applyLocale` ở đây — chuyển sang `SelectLanguage` (task 5).
  - `catch (e: CancellationException) { throw e }` **trước** `catch (e: Exception) { saveError = true }`.
  - **Xoá** `OnboardingViewModelFactory.kt`.
  → Verify: `./gradlew.bat :app:assembleDebug` không lỗi ở 4 file onboarding.

- [ ] **5. Bước 1 — Tiền tệ & Ngôn ngữ** — `steps/Step1RhythmScreen.kt` → `Step1CurrencyScreen.kt`: 2 nhóm single-select (VND/USD/EUR/JPY, Tiếng Việt/English/Theo hệ thống). `SelectLanguage` trong ViewModel gọi **ngay** `setAppLanguage` + `LocaleHelper.applyLocale` — bước 2/3 hiển thị đúng ngôn ngữ mới, và giá trị đã vào DataStore trước khi `Complete`. → Verify: chọn English ở bước 1 → tiêu đề bước 2 đổi sang tiếng Anh ngay.

- [ ] **6. Bước 2 — Ví** — `Step2GoalsScreen.kt` → `Step2WalletScreen.kt`: `OutlinedTextField` (`defaultMinSize(minHeight = 48.dp)`, `onValueChange` cắt `take(Wallet.MAX_NAME_LENGTH)` để không vỡ `require` trong `Wallet.init`) + hàng chọn icon `ICON_CASH/BANK/MOMO/CARD` qua `WalletUiHelper.getIcon(iconKey)`. Nút Continue `enabled = !isSaving && walletName.isNotBlank()`. → Verify: xoá trắng tên → nút Continue xám; gõ lại → bật.

- [ ] **7. Bước 3 — Nhắc nhở** — `Step3TimelineScreen.kt` → `Step3ReminderScreen.kt`: `Switch` + `TimePicker`/preset cho giờ nhắc. Khi gạt ON: nếu `NotificationHelper.hasNotificationPermission(context)` → `ToggleReminder(true)`; nếu không → `rememberLauncherForActivityResult(RequestPermission())` cho `POST_NOTIFICATIONS`, callback `granted == true` mới bật, ngược lại **giữ switch OFF** + hiện `onboarding_notification_denied`. → Verify: bật rồi bấm Deny → switch tự về OFF, hiện dòng cảnh báo; `Complete` không đặt lịch nhắc.

- [ ] **8. Dọn component chết + BackHandler** — `components/OnboardingComponents.kt`: giữ `CuePrimaryButton` (đổi tên `OnboardingPrimaryButton`, dùng `AppTheme.colors.primary`) và `ProgressDotsIndicator`. **Xoá** `CueDailyRhythm`, `CueTimePickerBottomSheet`, `ThemeToggleButton`, import `TimeCalculator`/`DateFormat`. Sửa `OnboardingScreen.kt`: bỏ `CueTimePickerBottomSheet`, giữ `userScrollEnabled = false`, trỏ 3 screen mới, thêm `BackHandler(enabled = currentStep != OnboardingStep.CURRENCY) { NavigateToStep(previousStep) }` để phím Back hệ thống lùi bước thay vì thoát app. → Verify: bấm Back ở bước 2 → về bước 1, không thoát app; ở bước 1 → thoát app.

- [ ] **9. Strings 2 ngôn ngữ** — thêm vào **cả** `res/values/strings.xml` và `res/values-en/strings.xml` (đồng bộ 1:1): `onboarding_step_currency_title/subtitle`, `onboarding_currency_vnd/usd/eur/jpy`, `onboarding_language_vi/en/system`, `onboarding_step_wallet_title/subtitle`, `onboarding_wallet_name_label`, `onboarding_wallet_icon_cash/bank/momo/card`, `onboarding_step_reminder_title/subtitle`, `onboarding_reminder_toggle`, `onboarding_reminder_time`, `onboarding_notification_denied`, `onboarding_error_save`, `action_continue`, `action_skip`, `action_start_using_notepay`, `onboarding_custom_time`, `onboarding_quick_select_title`, `onboarding_dialog_reminder_title`. → Verify: `grep -c 'name="onboarding_' ` bằng nhau ở 2 file; `grep -rn '"[A-ZĐ][a-zà-ỹ]' app/.../onboarding --include=*.kt` không còn chuỗi cứng.

- [ ] **10. Nối navigation** — `NotePayNavHost.kt:259` thay `AuthWelcomeScreen(...)` bằng `OnboardingRoute(onComplete = { navController.navigate(Route.Home.path) { popUpTo(Route.Welcome.path) { inclusive = true } } })`. **Không** gọi `onCompleteWelcome()` trong lambda này — cờ `hasSeenWelcome` được ghi trong ViewModel ở bước ⑥ của `Complete`, nên state đã nhất quán trước khi navigate. `OnboardingRoute.kt`: `LaunchedEffect(uiState.isCompleted) { if (it) onComplete() }` — chỉ điều hướng một lần, không dùng `isCompleted` cho bất kỳ logic nào khác. `AuthWelcomeScreen.kt` + `GoogleSignInCard.kt` giữ nguyên file (Utilities/Backup vẫn dùng), chỉ bỏ import khỏi NavHost. → Verify: xoá data → thấy 3 bước → vào Home; tắt mở lại app → vào thẳng Home, không nháy màn welcome.

- [ ] **11. Build + test** — `./gradlew.bat :app:assembleDebug` rồi `./gradlew.bat :app:testDebugUnitTest`. → Verify: BUILD SUCCESSFUL, 0 warning unused import trong 4 file onboarding.

## Done When
- [ ] `./gradlew.bat :app:assembleDebug` pass.
- [ ] `grep -rn "smartreminder" app/src` rỗng.
- [ ] Xoá data app → chạy → 3 bước hoạt động, chọn VND + tiếng Việt + nhắc 20:30 → vào Home, mở Settings thấy đúng giá trị đã lưu.
- [ ] Từ chối quyền thông báo → switch nhắc về OFF, vẫn vào được Home, không crash.
- [ ] Xoá trắng tên ví → nút Continue xám, không bấm được.
- [ ] Bấm Back hệ thống ở bước 2/3 → lùi bước, không thoát app.
- [ ] Sau `Complete`, Room chỉ có **1** ví (không nhân đôi "Tiền mặt").
- [ ] `values/strings.xml` và `values-en/strings.xml` cùng số key `onboarding_*`.

## Notes
- **Google Sign-In mất khỏi luồng đầu tiên.** Vẫn còn ở Utilities và Backup/Restore. Nếu cần giữ, thêm nút "Đăng nhập Google" ở bước 3 (dùng lại `GoogleSignInCard`, không viết mới).
- `AppDimensions` không có bậc 12dp; dùng `spaceMedium` (16dp) thay vì thêm token mới.
- **Seed race (đã xác minh)**: `SeedCallback.onCreate` chạy `CoroutineScope(Dispatchers.IO).launch` không đồng bộ — query đầu tiên có thể thấy `null`. Đã xử lý ở task 4 bước ① và ②.
- **Locale không recreate Activity**: `AndroidManifest.xml:32` có `configChanges="locale|layoutDirection"`. Nhưng trên API < 33 `LocaleHelper.applyLocale` dùng `updateConfiguration` nên không tự recompose — đổi ngôn ngữ ở bước 1 là cách duy nhất để bước 2/3 hiển thị đúng.
- **Double navigation không xảy ra**: `MainViewModel.startDestination` chỉ đọc một lần làm tham số `startDestination`, không phải collector điều hướng.
- `ReminderScheduler` là `object`, gọi trực tiếp bằng `context` từ ViewModel — không cần inject.
- **Chưa xử lý**: người dùng bấm 2 lần liên tiếp nút "Bắt đầu" → đã chặn bằng `enabled = !isSaving`. Nếu muốn chắc hơn, thêm guard `if (_uiState.value.isSaving) return` đầu `Complete` (đã có sẵn pattern này ở code Cue cũ).
