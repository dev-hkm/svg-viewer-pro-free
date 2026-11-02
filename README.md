# SVG Viewer Pro Free

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![Zero Ads](https://img.shields.io/badge/Cost-100%25%20Free%20%26%20No%20Ads-brightgreen.svg)]()

> **The ultimate modern, privacy-first SVG viewer, interactive inspector, motion player, and ultra-high-res 8K exporter for Android.** Built with Jetpack Compose, Material Design 3, and an ultra-crisp vector rendering engine.

---

## ✨ Key Features

### 🔍 Deep Vector Zoom & Infinite Clarity
- **Sub-Pixel Dynamic Re-Rasterization**: Zoom into intricate vector artwork up to 50x magnification without pixelation or blurriness.
- **Double-Tap & Smooth Gestures**: Fluid pinch-to-zoom, pan, fling, and auto-fit canvas centering.

### 🎬 Full Motion & Animation Engine
- **SMIL & CSS Animation Support**: Plays complex SVG animations powered by SMIL elements (`<animate>`, `<animateTransform>`, `<animateMotion>`) and modern CSS keyframes.
- **JavaScript Motion Support**: Executes interactive and scripted animations faithfully.
- **Playback Controls**: Dedicated play, pause, seek, and loop controls right on the viewport toolbar.

### 🎯 Interactive Element Inspector
- **Tap-to-Inspect**: Tap directly on any shape, path, rect, or text inside the SVG to view DOM hierarchy, tag name, dimensions, fill color, and stroke weight.
- **Isolate & Toggle Visibility**: Temporarily hide or highlight specific vector layers to audit complex multi-group designs.
- **Snippet Copy**: Extract and copy the clean XML/SVG source code of any isolated element directly to your clipboard.

### 🎨 Global Palette Extraction & Batch Recolor
- **Automatic Palette Discovery**: Automatically extracts every unique color token used across gradients, fills, and strokes in your SVG.
- **1-Tap Batch Color Replacement**: Swap any color globally across all instances in real-time.
- **Multi-Level Undo/Redo**: Experiment freely with non-destructive instant revert and reset.

### 📝 Integrated SVG Code Editor
- **Live Syntax Highlighting**: Clean, fast, and legible code highlighting designed for XML/SVG syntax.
- **Quick Snippet Insertion**: Rapidly inject standard SVG primitives, gradients (`<linearGradient>`, `<radialGradient>`), filters, clip-paths, and animation tags.
- **Instant Hot-Reload**: Save your edits to see the canvas update immediately with zero reload lag.

### 🖼️ Ultra-High-Resolution PNG Exporter (up to 8K)
- **Lossless Multi-Resolution Render**: Export any vector graphic directly to PNG in **1080p (FHD)**, **4K (UHD)**, and **8K (Ultra)**.
- **Customizable Canvas Background**: Choose between Obsidian Dark (`#08090C`), Studio Light, Checkerboard Transparency, Pure White, or custom solid tones.

### ⚡ System-Wide Integration & Privacy
- **Direct File Associations**: Opens `.svg` files seamlessly from Telegram, WhatsApp, Chrome, Google Drive, and your device's File Manager.
- **Offline & Private**: Zero internet permissions required for viewing. No telemetry, no analytics, no watermarks, no paywalls.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Language**: [Kotlin 2.0](https://kotlinlang.org)
- **Target SDK**: Android 15 (API 35) | **Min SDK**: Android 8.0 (API 26)
- **Vector Rendering Engine**: Hybrid Chromium WebCore Engine with optimized hardware-accelerated SVG canvas.
- **Architecture**: Single-Activity, MVI / StateFlow-driven unidirectional data flow.
- **Design System**: Urbanist typography, Obsidian Dark Palette, haptic touch feedback, Android predictive back gestures.

---

## 🚀 Getting Started & Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer.
- JDK 17 or JDK 21 (Eclipse Adoptium recommended).
- Android SDK with Platform 35.

### Build Instructions
```bash
# Clone the repository
git clone https://github.com/dev-hkm/svg-viewer-pro-free.git
cd svg-viewer-pro-free

# Build debug APK
./gradlew assembleDebug

# Build release bundle (AAB)
./gradlew bundleRelease
```

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
Copyright (c) 2026 Khanh Minh
```

---

*Crafted with passion for vector artists, designers, and mobile developers.*
