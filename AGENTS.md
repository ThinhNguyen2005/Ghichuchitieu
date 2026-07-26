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

Preserve established UI/UX and architecture unless a request explicitly asks
for a redesign. Prefer minimal, evidence-based changes. After Android/Kotlin
edits, run relevant Gradle compilation or focused tests and report the actual
result.

User instructions take precedence over this file.
