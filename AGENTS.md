# Shared agent instructions

All agents working in this repository, including Codex and Gemini, use the
existing `.agents/` directory as the shared source of truth.

At the start of a task:

1. Read `.agents/rules/core-protocol.md`.
2. Read `.agents/memory/MEMORY.md` and task-relevant memory files.
3. Read `.agents/rules/request-routing.md`, then load only the matching role
   and skill material.
4. Review relevant `.agents/*/handoff.md` files before duplicating previous
   investigation.

When work needs to be handed to another agent, create a concise dedicated
handoff at `.agents/<task>/handoff.md`. Do not overwrite existing briefings,
progress files, or handoffs belonging to another workstream.

# Build

./gradlew.bat :app:assembleDebug

# Unit test

./gradlew.bat :app:testDebugUnitTest

# Device test

./gradlew.bat :app:connectedDebugAndroidTest

# Quy tắc sửa code

- Đọc file liên quan trước khi sửa.
- Chỉ sửa trong phạm vi nhiệm vụ.
- Không thêm dependency khi chưa được yêu cầu.
- Không thay Room schema ngoài nhiệm vụ migration.
- Không thay UI khi task chỉ liên quan logic.
- Mọi logic parse mới phải có unit test.
- Không bỏ qua lỗi build có sẵn; phải báo rõ lỗi nào có trước thay đổi.
- Giữ nguyên UI/UX và kiến trúc đã có, trừ khi yêu cầu nói rõ là thiết kế lại.
- Sau khi sửa Android/Kotlin, chạy build hoặc test liên quan và báo kết quả thật.

User instructions take precedence over this file.
