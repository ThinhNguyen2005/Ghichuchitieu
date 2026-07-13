# Feature Specification: Liquid Glass UI v2

**Feature Branch**: `002-liquid-glass-ui-v2`

**Created**: 2026-07-13

**Status**: Draft

**Input**: User description: "Feature: Liquid Glass UI v2. Goal: Refine the entire application UI while preserving all existing business logic. No feature changes. Only improve UX, animations, hierarchy, spacing, typography, accessibility and Liquid Glass visual quality."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Unified Visual Hierarchy & Spacing (Priority: P1)

A user navigates through the NotePay application and experiences a clean, modern interface with consistent padding, margins, and legible typography that clearly distinguishes different types of financial information.

**Why this priority**: Correct visual hierarchy and spacing rhythm are the foundation of premium UI/UX, directly impacting readability and overall user satisfaction.

**Independent Test**: Can be verified by navigating to the Home, Transaction List, and Stats screens and observing that all elements align perfectly to a unified grid system and that typography establishes a clear hierarchy of information.

**Acceptance Scenarios**:

1. **Given** the user is on the Home screen, **When** they view the wallet cards and transaction list, **Then** all elements use consistent spacing (based on a standard 8dp grid), card margins align perfectly, and headings have distinct typographic weight from body text.
2. **Given** the user is on the Add Wallet or Add Transaction screens, **When** they look at form input fields and buttons, **Then** form fields have consistent heights, paddings, and alignment, and labels are clearly legible.

---

### User Story 2 - Smooth Glass Micro-animations (Priority: P2)

A user interacts with buttons, bottom sheets, and navigation tabs, seeing fluid, premium micro-animations that provide immediate, satisfying visual feedback.

**Why this priority**: Smooth animations are essential to make the interface feel responsive, premium, and alive, matching the Liquid Glass identity.

**Independent Test**: Interact with navigation tabs, chips, and transaction cards, observing that transitions and visual feedbacks are rendered smoothly without lag or stutter.

**Acceptance Scenarios**:

1. **Given** the user is navigating between screens, **When** they tap on navigation items, **Then** the screen transition animations are smooth (achieving up to 60fps) with consistent slide/fade curves.
2. **Given** the user taps an interactive button or card, **When** they press it, **Then** the component displays a subtle interactive glass scaling or highlight micro-animation.

---

### User Story 3 - Premium Frosted Glass Effects (Priority: P3)

A user interacts with dialogs, bottom sheets, and overlays, observing high-quality frosted glass blur effects with proper contrast.

**Why this priority**: Elevates the aesthetic quality to a premium level while ensuring accessibility and text readability on top of vibrant backdrops.

**Independent Test**: Open modal overlays or bottom sheets and check that backdrop elements are blurred correctly while text remains highly legible.

**Acceptance Scenarios**:

1. **Given** the user opens the Add Subscription or Wallet Picker bottom sheets, **When** the sheet is displayed, **Then** it renders a frosted glass surface with distinct blur radius, a fine border stroke, and readable text contrast.
2. **Given** the user views analytics graphs, **When** cards are layered over the backdrop, **Then** the backdrop vibrancy shines through without compromising readability.

---

### Edge Cases

* **Low-End Devices / Performance Limits**: What happens when running on low-end Android devices? The system must scale back blur radius or fall back to high-quality semi-transparent backgrounds to maintain 60fps performance without stutter.
* **Varying Screen Densities (mdpi to xxxhdpi)**: How do glass effects and borders scale on different screen resolutions? Spacing grids, corner radiuses, and border strokes must adapt proportionally to maintain visual quality across all devices.

## Requirements *(mandatory)*

### Functional Requirements

* **FR-001**: System MUST preserve all existing business logic, validation rules, offline-first behavior, and database structures.
* **FR-002**: System MUST reuse the existing repositories, ViewModels, and navigation graphs exactly as implemented.
* **FR-003**: System MUST unify all screen margins, padding values, and element alignments to adhere to a strict spacing grid (8dp base).
* **FR-004**: System MUST maintain the Liquid Glass visual identity by styling UI containers with frosted glass effects, backdrop blurs, and semi-transparent borders.
* **FR-005**: System MUST ensure text is readable on top of frosted/vibrant backdrops, complying with accessibility contrast standards.
* **FR-006**: All screen navigation transitions and micro-animations MUST run smoothly without stutter (striving for 60fps).

### Key Entities

* *Note: No new business entities are introduced. This feature works entirely on existing entities (Wallet, Transaction, Category, Subscription, BillSplit).*

## Success Criteria *(mandatory)*

### Measurable Outcomes

* **SC-001**: UI transitions and animations maintain a frame rate of 60fps.
* **SC-002**: All main screens load under 200ms with zero visual layout shifts (CLS).
* **SC-003**: 100% of existing unit and instrumentation tests continue to build and pass (zero regression on logic).
* **SC-004**: Text elements on frosted/vibrant backdrops have a minimum contrast ratio of 4.5:1 (except decorative elements).

## Assumptions

* Mobile-first Android native layout.
* The existing Material 3 theme colors and local assets are reused.
* Screen resolution and density vary, but layout scales correctly (responsive).
* No new third-party dependency is introduced unless Jetpack Compose libraries already configured in the project.
