# Implementation Plan: Liquid Glass UI v2

**Branch**: `002-liquid-glass-ui-v2` | **Date**: 2026-07-13 | **Spec**: [spec.md](file:///d:/Ghichuchitieu/specs/002-liquid-glass-ui-v2/spec.md)

**Input**: Feature specification from `specs/002-liquid-glass-ui-v2/spec.md`

## Summary

Kế hoạch này tập trung vào việc tinh chỉnh giao diện người dùng (UI) và trải nghiệm người dùng (UX) trên toàn bộ ứng dụng NotePay mà không thay đổi bất kỳ logic nghiệp vụ hoặc cấu trúc kiến trúc nào. Cách tiếp cận là áp dụng lưới khoảng cách chuẩn hóa (lưới 8dp), đồng bộ hóa kiểu chữ (typography), nâng cao chất lượng hiệu ứng thị giác kính (Liquid Glass), và triển khai các hiệu ứng chuyển động (micro-animations, transitions) mượt mà đạt tần số quét 60fps, đồng thời bảo đảm khả năng phản hồi tốt trên cả các thiết bị cấu hình thấp.

## Technical Context

**Language/Version**: Kotlin 1.9+ / Java 17

**Primary Dependencies**: Jetpack Compose, Material 3, Room, Hilt, SharedPreferences

**Storage**: Room Database (SQLite local) & SharedPreferences

**Testing**: JUnit, Compose Test, Mockk

**Target Platform**: Android (API 26+)

**Project Type**: Mobile Application (Android)

**Performance Goals**: UI transitions và hiệu ứng chuyển động giữ vững tần số quét 60fps, toàn bộ màn hình chính tải dưới 200ms với chỉ số dịch chuyển bố cục (CLS) bằng 0.

**Constraints**: Hoạt động hoàn toàn offline (Offline-first), bảo vệ dữ liệu cục bộ (Privacy-first), tuyệt đối không để xảy ra regression về logic nghiệp vụ.

**Scale/Scope**: Tinh chỉnh UI trên 6 nhóm màn hình đặc trưng: HomeScreen, WalletScreen (Add/Edit), AddTransactionScreen, TransactionListScreen, StatsScreen, và SubscriptionScreen.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

*   **UI Consistency & Liquid Glass**: Đảm bảo không thay đổi hoặc lược bỏ Liquid Glass, Blur, Vibrancy, Frosted surfaces. Việc tinh chỉnh v2 phải kế thừa và nâng cao chất lượng hiệu ứng kính hiện tại.
*   **Architecture & Freeze**: Tuân thủ MVVM + Repository + StateFlow. Đóng băng hoàn toàn kiến trúc, không tạo thêm bất kỳ UseCase, Repository, hay Service nào ngoài các lớp sẵn có trên đĩa.
*   **Compose Guidelines**: Composable nhỏ gọn, sử dụng State Hoisting.
*   **Performance**: Tránh recomposition thừa, không tạo đối tượng mới trực tiếp trong hàm Compose.
*   **Quality Gates**: Biên dịch thành công (compile), đóng gói debug thành công (`assembleDebug`), không đưa thêm cảnh báo cảnh báo.
*   **Zero Surprise Rule**: Giữ nguyên tính năng hoạt động, chỉ thay đổi giao diện theo hướng tinh tế và nhất quán hơn.

## Project Structure

### Documentation (this feature)

```text
specs/002-liquid-glass-ui-v2/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
└── quickstart.md        # Phase 1 output
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
│       │   ├── home/
│       │   ├── wallet/
│       │   ├── addtransaction/
│       │   ├── list/
│       │   ├── stats/
│       │   └── subscription/
│       └── theme/       # Liquid Glass theme configurations
```

**Structure Decision**: Cấu trúc module đơn (`app/`) tuân thủ chuẩn Clean Architecture đã đóng băng. Các cải tiến UI chỉ tác động vào lớp giao diện `ui/feature/` và các thành phần dùng chung trong `ui/component/`.

## Complexity Tracking & Intentional Simplifications (Ponytail Mode)

Tuân thủ nghiêm ngặt nguyên lý đóng băng kiến trúc và giảm thiểu độ phức tạp:
1. **Không thêm abstraction**: Các cải tiến hiệu ứng hoặc căn chỉnh lưới được đưa vào trực tiếp thông qua Modifier tùy chỉnh hoặc cấu phần dùng chung sẵn có.
2. **Kế thừa trạng thái hiện có**: Toàn bộ ViewModels và StateFlow giữ nguyên tên biến, kiểu dữ liệu và logic truyền tải để đảm bảo không lỗi logic.
3. **Cơ chế Fallback Blur cho thiết bị yếu**: Tích hợp điều kiện kiểm tra hiệu năng để chuyển đổi hiệu ứng blur đắt đỏ sang màu nền bán trong suốt đơn giản trên các thiết bị Android đời cũ hoặc cấu hình thấp.
