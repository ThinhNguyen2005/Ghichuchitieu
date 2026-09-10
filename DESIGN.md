---
name: NotePay
colors:
  primary: "#0A84FF"
  surface: "#FFFFFF"
  background: "#F2F2F7"
rounded:
  lg: 24px
  full: 999px
spacing:
  md: 16px
components:
  action-button:
    height: 52px
  navigation:
    height: 64px
---

## Overview
Preserve the existing Compose theme, navigation and financial workflows. Tokens above
describe existing baseline colors and dimensions; Android implementation uses dp.
User-selected colors and dark mode remain controlled by MaterialTheme and AppTheme.

## Colors
Use existing semantic theme colors for solid surfaces, content, selection and disabled state.

## Typography
Retain existing MaterialTheme/AppTheme typography and labels.

## Layout
Preserve control bounds, spacing, sheet geometry and navigation placement.

## Elevation & Depth
Only the navigation tab bar may sample a backdrop or use liquid lens/blur.
Enable only on supported Android/device configurations with hardware acceleration
and the saved user preference. Unsupported/disabled configurations use solid tabs.

## Shapes
Preserve the existing capsule buttons, rounded panels and circular add action.

## Components
Buttons, sliders, switches, cards, floating add and sheets use solid surfaces.
Keep click/disabled/accessibility behavior. The navigation add button stays solid.

## Do's and Don'ts
Do not change financial logic, Room schema or screen layout for this rendering cleanup.
Do not claim hardware acceptance from compilation alone.
