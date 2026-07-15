# Implementation Plan: Baseline Specification

**Branch**: `001-baseline-spec` | **Date**: 2026-07-13 | **Spec**: [spec.md](file:///d:/Ghichuchitieu/specs/001-baseline-spec/spec.md)

**Input**: Feature specification from `specs/001-baseline-spec/spec.md`

## Summary

Tài liệu này lập kế hoạch triển khai cơ sở (Baseline) cho dự án NotePay. Tập trung vào việc tích hợp và kiểm soát chặt chẽ 3 thành phần chính đã làm rõ: chỉ sử dụng đơn vị tiền tệ cơ sở duy nhất, phân tích tài chính ngoại tuyến bằng mô hình cục bộ Gemini Nano, và thu thập thông tin biến động số dư qua Android Notification Listener Service, tất cả đều tuân thủ các nguyên tắc thiết kế Liquid Glass và kiến trúc Clean MVVM của dự án.

## Technical Context

**Language/Version**: Kotlin 1.9+ / Java 17

**Primary Dependencies**: Jetpack Compose, Material 3, Room, Hilt, SharedPreferences

**Storage**: Room Database (SQLite local) & SharedPreferences (persisting custom categories list)

**Testing**: JUnit, Compose Test, Mockk

**Target Platform**: Android (API 26+)

**Project Type**: Mobile Application (Android)

**Performance Goals**: Không xảy ra recomposition thừa ở các danh sách giao dịch, giữ vững tốc độ khung hình 60fps trên giao diện kính Liquid Glass.

**Constraints**: Hoạt động hoàn toàn offline (Offline-first), bảo vệ dữ liệu cục bộ (Privacy-first).

**Scale/Scope**: Hỗ trợ nhiều ví, không giới hạn số lượng giao dịch lưu trữ ngoại tuyến.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

* **UI Consistency & Liquid Glass**: Đảm bảo không thay đổi hoặc lược bỏ Liquid Glass, Blur, Vibrancy, Lens distortion, Frosted surfaces.
* **Architecture**: Tuân thủ MVVM + Repository + UseCase + StateFlow. Tuyệt đối không để business logic lọt vào UI.
* **Compose Guidelines**: Composable phải nhỏ gọn, sử dụng State Hoisting, không trùng lặp UI.
* **Performance**: Tránh recomposition dư thừa, không tạo đối tượng mới trực tiếp trong hàm Compose.
* **Quality Gates**: Compile và assembleDebug thành công, pass lint, không tự ý xóa code cũ.
* **Dependencies & Theme**: Không tự ý đổi dependencies, Theme hoặc Navigation.
* **Zero Surprise Rule**: Giữ nguyên trạng thái giao diện và hành vi hiện tại của các thành phần Liquid Glass, chỉ thay đổi phạm vi được yêu cầu.

## Project Structure

### Documentation (this feature)

```text
specs/001-baseline-spec/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
app/
├── src/main/java/com/notepay/
│   ├── data/
│   │   ├── local/       # Room DB, Entity (Wallet, Transaction, Subscription, BillSplit), DAO
│   │   ├── mapper/      # DB to Domain entity mappers
│   │   └── repository/  # Repository implementations (*Impl.kt)
│   ├── domain/
│   │   ├── model/       # Pure Kotlin business entities
│   │   ├── repository/  # Repository interfaces
│   │   └── usecase/     # Usecases (AddTransaction, SuggestCategory, etc.)
│   ├── service/         # Notification Listener Service
│   └── ui/
│       ├── feature/     # Featurized screen sub-packages containing ViewModels and Screens
│       └── theme/       # Liquid Glass theme configurations
```

**Structure Decision**: Cấu trúc module đơn (`app/`) tuân theo mô hình Clean Architecture phân tách 3 lớp rõ rệt: Data, Domain và UI.

## Complexity Tracking & Intentional Simplifications (Ponytail Mode)

Dự án áp dụng triệt để nguyên lý "Lazy Senior Dev" (Ponytail Mode) nhằm loại bỏ mã thừa và giữ kiến trúc gọn nhẹ nhất có thể:

1. **Tích hợp Ngân sách vào Ví (No separate Budget table)**: 
   Hạn mức ngân sách (`budgetLimitCents`) được thiết lập trực tiếp dưới dạng trường tùy chọn thuộc thực thể `WalletEntity` thay vì tách riêng bảng Room. Điều này giúp loại bỏ hoàn toàn các câu lệnh JOIN phức tạp, lược bỏ DAO/Repository/UseCase riêng của ngân sách và tối ưu hóa việc quản lý trực tiếp trong luồng Ví (`AddWalletViewModel`, `StatsViewModel`).

2. **Lưu trữ danh mục qua SharedPreferences**:
   Các danh mục tùy chỉnh không được lưu bằng Room DB mà được quản lý qua `SharedPreferences` thông qua lớp `CategoryRepositoryImpl`. Danh sách danh mục được đăng ký động trong bộ nhớ ở lớp `Category`. Giải pháp này giúp thời gian phản hồi truy vấn danh mục đạt mức tức thời (<1ms) mà không phải gánh thêm chi phí bảo trì cơ sở dữ liệu.

3. **Bộ máy luật phân tích tài chính offline (`StatsInsightsEngine`)**:
   Để loại bỏ thư viện Gemini Nano SDK có kích thước nhị phân lớn và yêu cầu tài nguyên cao, NotePay sử dụng bộ máy luật heuristic thuần Kotlin `StatsInsightsEngine.kt` để thực hiện phân tích số liệu, cảnh báo ngân sách động (`dynamicDailyBudget`) và đề xuất gợi ý. Cơ chế này hoạt động 100% offline, phản hồi lập tức (<10ms) và tương thích hoàn toàn với mọi thiết bị Android.

4. **Gọi trực tiếp Repository từ ViewModel cho các tính năng đơn giản**:
   Đối với các nghiệp vụ đơn giản như quản lý gói nhắc nhở đăng ký định kỳ, `SubscriptionViewModel` gọi trực tiếp `SubscriptionRepository` thay vì đi qua lớp UseCase trung gian trống rỗng, giảm thiểu boilerplate code.

5. **Phát hiện đăng ký định kỳ trong luồng Stats**:
   Thuật toán phát hiện gói đăng ký định kỳ (so khớp tần suất giao dịch trong khoảng 27-33 ngày) được thực thi trực tiếp bên trong `StatsViewModel` và dịch vụ chạy ngầm của thông báo, không tạo thêm một lớp xử lý `SubscriptionDetector` riêng.
