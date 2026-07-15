<!--
SYNC IMPACT REPORT:
- Version change: 1.0.0 -> 1.1.0
- List of modified principles:
  - Expanded and refined core principles and constraints with project-specific rules.
- Added sections:
  - NotePay Specific Constraints (Inviolable)
    - Preserve Design Language
    - Never Remove Existing Features
    - UI Changes Are Local
    - Dependency Policy
    - Existing Components First
    - Architecture Preservation
    - Minimal Diff Policy
    - Build Before Finish
    - Regression Protection
    - Git Safety
    - Explain Breaking Changes
    - Forbidden Actions
    - Investigation First
    - Default Behavior
    - Zero Surprise Rule
- Removed sections: None
- Templates requiring updates:
  - .specify/templates/plan-template.md (✅ aligned)
  - .specify/templates/spec-template.md (✅ aligned)
  - .specify/templates/tasks-template.md (✅ aligned)
- Follow-up TODOs: None
-->

# NotePay Constitution

## Core Principles

### I. UI Consistency First
Không được phép tự ý thay đổi giao diện người dùng (UI) đã được chỉ định. Không được gỡ bỏ LiquidGlass ra khỏi cấu trúc giao diện. Tuyệt đối không thay đổi màu sắc trừ khi có yêu cầu cụ thể trong mô tả tác vụ.

### II. Clean Architecture & MVVM
Hệ thống tuân thủ nghiêm ngặt kiến trúc MVVM phối hợp cùng Repository và UseCase, giao tiếp qua StateFlow. Tuyệt đối không đưa hoặc xử lý logic nghiệp vụ (business logic) bên trong tầng giao diện UI.

### III. Compose Best Practices
Các hàm Composable phải được thiết kế nhỏ gọn, thực hiện một nhiệm vụ duy nhất và tái sử dụng tối đa. Đảm bảo trạng thái được nâng lên trên (State hoisted). Tránh trùng lặp code giao diện (no duplicate UI).

### IV. Performance & Recomposition
Kiểm soát chặt chẽ recomposition để tránh các tiến trình vẽ lại dư thừa gây suy giảm hiệu năng. Tuyệt đối không khởi tạo đối tượng trực tiếp bên trong hàm Compose mà không sử dụng cơ chế ghi nhớ (`remember`).

### V. Quality Gates
Mọi thay đổi mã nguồn trước khi được nghiệm thu phải đảm bảo: biên dịch thành công (compile), đóng gói debug thành công (`assembleDebug`), và vượt qua kiểm tra lỗi tĩnh (`lint`). Tuyệt đối không tự ý xóa code cũ nếu chưa có sự xác nhận rõ ràng của người dùng.

### VI. Dependencies & Framework Constraints
Không thay đổi hay thêm mới bất kỳ thư viện phụ thuộc (dependency) nào nếu chưa hỏi ý kiến. Không sửa đổi Theme, Navigation hoặc chuyển đổi Material3 sang các thư viện giao diện khác trừ khi có yêu cầu trực tiếp từ mô tả tác vụ.

### VII. Pre-Commit Discipline
Trước khi thực hiện commit mã nguồn lên Git, lập trình viên và Agent bắt buộc phải thực thi các bước kiểm tra: chạy `git diff` để rà soát thay đổi, thực hiện tự soát lỗi (self-review), biên dịch thử (compile) và chạy đóng gói kiểm tra (assemble).

## Development Style: Ponytail Mode

Bạn là một Senior Developer lười biếng. Lười biếng ở đây nghĩa là làm việc tối ưu hiệu năng, chứ không phải cẩu thả. Đoạn code tốt nhất là đoạn code không bao giờ phải viết.

Trước khi viết bất kỳ đoạn code nào, hãy tự đặt câu hỏi và dừng lại ở mức đầu tiên phù hợp:
1. Tính năng này có thực sự cần thiết hay không? (YAGNI - You Aren't Gonna Need It)
2. Thư viện chuẩn (standard library) của ngôn ngữ/nền tảng đã hỗ trợ tính năng này chưa? Hãy dùng nó.
3. Tính năng gốc của hệ điều hành/nền tảng (native platform) có đáp ứng được không? Hãy dùng nó.
4. Dependency đã được cài đặt sẵn trong dự án có giải quyết được không? Hãy dùng nó.
5. Đoạn code này có thể viết ngắn gọn trên một dòng không? Hãy viết trên một dòng.
6. Chỉ sau khi đã đi qua hết các bước trên: Viết lượng code tối thiểu cần thiết để tính năng hoạt động.

Quy tắc thực thi:
- Không tạo ra các lớp trừu tượng (abstraction) trừ khi có yêu cầu rõ ràng.
- Không thêm bất kỳ dependency mới nào nếu có thể tránh được.
- Không viết code boilerplate (code khuôn mẫu dư thừa) khi không ai yêu cầu.
- Ưu tiên xóa code hơn là thêm code. Ưu tiên những giải pháp đơn giản (boring) hơn những giải pháp phức tạp/thông minh (clever). Sử dụng ít tệp tin nhất có thể.
- Đặt câu hỏi chất vấn đối với những yêu cầu phức tạp: "Bạn có thực sự cần X không, hay Y đã đủ đáp ứng rồi?"
- Khi có hai cách tiếp cận bằng stdlib cùng kích thước dòng code, hãy chọn giải pháp bao phủ được tất cả các trường hợp biên (edge-case-correct). Lười biếng nghĩa là viết ít code hơn, chứ không phải giải thuật yếu hơn.
- Đánh dấu những đoạn tối giản hóa có chủ ý bằng comment bắt đầu bằng `ponytail:`. Nếu giải pháp rút gọn đó có giới hạn nhất định (như khóa toàn cục, quét O(n²), suy nghiệm thô sơ), hãy nêu rõ giới hạn đó trong comment và ghi chú hướng nâng cấp về sau.

## NotePay Specific Constraints (Inviolable)

### 1. Preserve Design Language
This project has an established design language. It is **NOT** acceptable to redesign the application unless explicitly requested.
The following are considered project identity and must be preserved:
* Liquid Glass visual language
* Blur
* Vibrancy
* Lens distortion
* Frosted surfaces
* Glass transitions
* Interactive highlights
* Glass navigation
* Existing motion design
* Existing spacing rhythm
* Existing color philosophy

Do **not** replace them with Material defaults, Surface, Card, Box, or any flat UI.
If a requested feature conflicts with the design language:
* preserve the existing Liquid Glass implementation
* extend it
* never replace it

### 2. Never Remove Existing Features
Unless explicitly instructed:
* never delete an existing feature
* never simplify an implementation
* never replace a custom component with a standard Compose component
* never remove animations
* never remove visual effects
* never remove interactions

Bug fixing must preserve behavior.

### 3. UI Changes Are Local
A task only authorizes changes required to solve that task.
Examples:
* Changing typography ≠ Changing navigation.
* Changing Home Screen ≠ Changing Theme.
* Changing Theme ≠ Changing animations.
* Changing animation ≠ Changing dependency graph.

### 4. Dependency Policy
Never remove a dependency without proving it is unused.
Before removing a dependency:
* search all usages
* search reflection usage
* search generated code usage
* search indirect wrapper usage

If uncertain, keep the dependency.

### 5. Existing Components First
Before creating a new component:
* Search for an existing implementation.
* Reuse it.
* Extend it.
* Never duplicate it.
* Never rewrite it.

### 6. Architecture Preservation & Freeze
The current architecture is considered stable.
Agents must prefer extending existing modules over introducing new architectural layers.
Creating new repositories, new abstractions, new services, new use cases, or new packages requires explicit justification.
**Reuse before rewrite.** Do not migrate:
* MVVM
* Repository
* UseCases
* StateFlow
* Navigation
* DI
* Theme

unless explicitly requested.

### 7. Minimal Diff Policy
Every task should produce the smallest possible diff.
Prefer: modify 5 lines instead of rewrite 200 lines.

### 8. Build Before Finish
A task is not complete until:
* project compiles
* assembleDebug succeeds
* no new warnings introduced
* no existing feature regressed

### 9. Regression Protection
Before editing any file:
* identify what the file currently provides.

After editing:
* verify every previous capability still exists.

Example: If `LiquidButton` originally provides blur, vibrancy, lens, ripple, and backdrop sampling, the final version must still provide them unless explicitly requested otherwise.

### 10. Git Safety
* Never overwrite large files.
* Never replace an entire file when a local patch is sufficient.
* Prefer minimal edits.
* Never discard existing code.
* Never delete code that cannot be regenerated.

### 11. Explain Breaking Changes
Before making any breaking change, stop and explain:
* what will change
* what will disappear
* why it is necessary

Wait for confirmation.

### 12. Forbidden Actions
Without explicit approval, NEVER:
* replace Liquid Glass with Material Surface
* flatten the UI
* remove blur
* remove backdrop
* remove vibrancy
* remove lens
* remove InteractiveHighlight
* remove custom animations
* replace custom components with standard Compose widgets
* delete dependencies
* rewrite Theme
* rewrite Navigation
* rewrite ColorScheme
* rewrite Typography
* rewrite architecture

### 13. Investigation First
Before modifying code:
1. Locate every call site.
2. Understand why the current implementation exists.
3. Preserve all existing capabilities.
4. If an implementation appears "too complicated", assume it exists for a reason.

Never simplify code until you understand what would be lost. If uncertain, stop and ask.

### 14. Default Behavior
When asked to fix a bug:
* DO NOT refactor.
* DO NOT redesign.
* DO NOT modernize.
* DO NOT optimize unrelated code.
* DO NOT improve architecture.
* Only fix the requested issue. Everything else stays exactly the same.

### 15. Zero Surprise Rule
The project owner values stability over novelty.
Unless explicitly instructed, every visual element should remain pixel-identical after your changes.
If your implementation changes the appearance, animation, interaction, spacing, color, typography, navigation, or visual effects outside the requested scope, stop and ask for confirmation.
Preserve the existing Liquid Glass identity at all costs.

## Development Workflow

Quy trình phát triển phần mềm cho dự án NotePay sẽ tuân thủ nghiêm ngặt theo 8 bước sau:
1. **Constitution** (Xác lập Hiến pháp)
2. **Specify** (Tạo Đặc tả)
3. **Clarify** (Làm rõ các điểm mơ hồ)
4. **Plan** (Lập Kế hoạch Kỹ thuật)
5. **Tasks** (Phân chia Tác vụ)
6. **Analyze** (Đánh giá mức độ đồng bộ và nhất quán)
7. **Implement** (Triển khai code từng bước)
8. **Converge** (Kiểm tra độ phủ và hoàn thiện tính năng)

## Governance
- Hiến pháp này là tối cao đối với mọi hoạt động phát triển tính năng và sửa lỗi trong dự án NotePay.
- Mọi pull request hoặc review code phải đối chiếu trực tiếp với các nguyên tắc trong Hiến pháp này.
- Các sửa đổi đối với Hiến pháp phải được ghi nhận phiên bản rõ ràng, có sự đồng thuận và đi kèm kế hoạch cập nhật tài liệu liên quan.

**Version**: 1.1.0 | **Ratified**: 2026-07-13 | **Last Amended**: 2026-07-13
