# 25 — MX Player Refinement Plan

This document details the specific "Missing Pieces" needed to make StreamCast a perfect functional clone of MX Player.

## 1. Advanced Playback Engine
- [ ] **Background Play**: Keep audio playing when the app is minimized or the screen is off.
- [ ] **A-B Repeat**: Logic to loop a specific user-defined segment of a video.
- [ ] **Sleep Timer**: Menu option to automatically close the player after a set duration.
- [ ] **Kids Lock + Touch Effects**: Standard lock is already done; need to add colorful shapes/animations when the screen is touched while locked.
- [ ] **Orientation Lock**: Dedicated button to force Landscape/Portrait regardless of system settings.

## 2. Audio & Subtitle Management
- [ ] **Multi-Track Audio Selector**: A bottom sheet to pick between different language streams within a file.
- [ ] **Advanced Subtitle Styling**: Menu to adjust font size, text color, border width, and background opacity.
- [ ] **Subtitle Syncing**: Controls to adjust text delay/offset in milliseconds.
- [ ] **Subtitle Gestures**: Vertical drag on subtitle text to reposition; pinch text to change font size.

## 3. Library & File Management
- [ ] **Advanced Sorting**: Sort folders and files by:
    - Date Added
    - File Size
    - Resolution
    - Filename (A-Z/Z-A)
- [ ] **"NEW" Badges**: Visual indicator (tag) for media added to the device in the last 24–48 hours.
- [ ] **Folder Hiding**: Ability to long-press a folder and select "Hide" to exclude it from the scan.
- [ ] **Global Search**: Search for any filename across the entire hierarchical folder structure.
- [ ] **Privacy Folder**: A PIN-protected space to move and hide sensitive videos.

## 4. Audio Processing
- [ ] **10-Band Equalizer**: Built-in equalizer with standard presets (Bass Boost, Vocal, etc.).
- [ ] **Volume Boost (200%)**: Ability to increase volume beyond the hardware limit using software gain.

## 5. Modern 2026 UI Trends
- [ ] **Vertical "Fatafat" Feed**: Dedicated tab for scrolling through short vertical clips (9:16) found on the device.
- [ ] **Glassmorphism Theme**: Dynamic UI that extracts colors from the current video thumbnail to create a glowing background effect.

## 6. Optimization
- [ ] **HW+ Decoder Support**: Implement custom RenderersFactory to support more hardware-accelerated formats.
- [ ] **Multi-Core Decoding**: Ensure the playback engine is utilizing all available CPU cores for 4K/8K files.
