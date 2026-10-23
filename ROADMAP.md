# Roadmap: Smart Neopixel Xmas Tree Remote Control System

This roadmap outlines the development plan for building an Android Application and corresponding Bluetooth Low Energy (BLE) firmware for the ESP32-C6 microcontroller to remotely control an addressable Neopixel Xmas Tree with an integrated loudspeaker/buzzer.

---

## Progress Overview

| Phase | Description | Status |
| :--- | :--- | :---: |
| **Phase 1** | System Architecture & Technical Specifications | ✅ |
| **Phase 2** | Project Setup & CI/CD Pipeline | ⏳ |
| **Phase 3** | ESP32-C6 Firmware Core & BLE GATT Server | ⏳ |
| **Phase 4** | Neopixel LED Engine & Dynamic Patterns | ⏳ |
| **Phase 5** | Audio / Buzzer Melody Engine | ⏳ |
| **Phase 6** | System Telemetry & Non-Volatile Storage (NVS) | ⏳ |
| **Phase 7** | Android App Architecture & BLE Client Infrastructure | ⏳ |
| **Phase 8** | Android User Interface & Preset Management | ⏳ |
| **Phase 9** | System Integration, Testing & Verification | ⏳ |

---

## Goals

- ✅ Define product concept, architecture, hardware selections, and binary protocol specifications (`CONCEPT.md`, `DESIGN.md`).
- ⏳ Establish CI/CD test automation framework for both Android client and ESP32-C6 firmware.
- ⏳ Implement high-performance ESP32-C6 FreeRTOS firmware with NimBLE stack, Neopixel RMT driver, and LEDC audio melody engine.
- ⏳ Develop a modern Jetpack Compose Android Application for wireless device discovery, lighting/audio customization, and preset management.
- ⏳ Ensure synchronized audio-visual festive themes with low-latency Bluetooth Low Energy control.

---

## Phases

### Phase 1: System Architecture & Technical Specifications
- [x] **Task 1.1: Define Product Concept & Business Use Cases**
  - [x] Subtask 1.1.1: Create `CONCEPT.md` detailing target audience, value proposition, and use cases.
  - [x] Subtask 1.1.2: Evaluate alternative architectures (BLE vs Classic BT vs Wi-Fi, Kotlin Compose vs React Native, RTTTL PWM vs I2S DAC).
- [x] **Task 1.2: Detailed System Design & Interfaces**
  - [x] Subtask 1.2.1: Create `DESIGN.md` specifying binary protocol structures, FreeRTOS tasks, and pin allocations.
  - [x] Subtask 1.2.2: Define PlantUML top-level architectural diagram `TOP_ARCHITECTURE.puml`.

---

### Phase 2: Project Setup & CI/CD Pipeline
- [ ] **Task 2.1: Repository Structure & Build Environment Setup**
  - [ ] Subtask 2.1.1: Create directory tree (`/src/firmware/`, `/src/android/`, `/test/`).
  - [ ] Subtask 2.1.2: Create `src/install.sh` for build tool dependencies.
- [ ] **Task 2.2: Automated Testing & CI/CD Configuration**
  - [ ] Subtask 2.2.1: Create `test/install.sh` for test tools and runners.
  - [ ] Subtask 2.2.2: Set up GitHub Actions workflow (`.github/workflows/ci.yml`) for automated builds and tests on every commit.

---

### Phase 3: ESP32-C6 Firmware Core & BLE GATT Server
- [ ] **Task 3.1: FreeRTOS Core & NimBLE BLE Stack Initialization**
  - [ ] Subtask 3.1.1: Initialize ESP-IDF project for ESP32-C6 target with NimBLE component.
  - [ ] Subtask 3.1.2: Implement `nimble_host_task` for BLE advertising (`XmasTree-ESP32C6`) and GATT connection handling.
- [ ] **Task 3.2: Custom GATT Service & Binary Protocol Parser**
  - [ ] Subtask 3.2.1: Register custom GATT service (`12345678-1234-5678-1234-56789abcdef0`) and characteristics for Lighting, Audio, and Telemetry.
  - [ ] Subtask 3.2.2: Implement `cmd_dispatcher_task` to receive, validate, and parse 8-byte Lighting and 6-byte Audio binary commands into FreeRTOS queues.

---

### Phase 4: Neopixel LED Engine & Dynamic Patterns
- [ ] **Task 4.1: RMT Peripheral LED Driver Implementation**
  - [ ] Subtask 4.1.1: Configure ESP32-C6 RMT peripheral on GPIO 8 for WS2812B timing protocol.
  - [ ] Subtask 4.1.2: Develop `led_engine_task` frame rendering pipeline consuming commands from `xLedQueue`.
- [ ] **Task 4.2: Dynamic Animation Patterns**
  - [ ] Subtask 4.2.1: Implement Static Color pattern and Global Brightness/Speed controls.
  - [ ] Subtask 4.2.2: Implement dynamic lighting patterns (Rainbow Wave, Sparkle, Festive Candle, Twinkle).

---

### Phase 5: Audio / Buzzer Melody Engine
- [ ] **Task 5.1: LEDC PWM Hardware Driver Setup**
  - [ ] Subtask 5.1.1: Configure LEDC peripheral on GPIO 19 for variable frequency tone generation.
  - [ ] Subtask 5.1.2: Implement `audio_engine_task` handling tone frequency, duty cycle, and volume scaling.
- [ ] **Task 5.2: RTTTL Melody Parser & Chiptune Library**
  - [ ] Subtask 5.2.1: Implement Ring Tone Text Transfer Language (RTTTL) score parser.
  - [ ] Subtask 5.2.2: Embed festive melody library (*Jingle Bells*, *We Wish You a Merry Christmas*, *Silent Night*, *Deck the Halls*).

---

### Phase 6: System Telemetry & Non-Volatile Storage (NVS)
- [ ] **Task 6.1: Non-Volatile Storage Manager**
  - [ ] Subtask 6.1.1: Implement `nvs_manager` to save/restore last-used brightness, volume, and active mode flags to NVS flash.
- [ ] **Task 6.2: System Status Telemetry Notifications**
  - [ ] Subtask 6.2.1: Implement 10-byte telemetry binary payload builder (uptime, active pattern ID, active melody ID, connection state, error codes).
  - [ ] Subtask 6.2.2: Enable BLE GATT notifications on the System Status characteristic.

---

### Phase 7: Android App Architecture & BLE Client Infrastructure
- [ ] **Task 7.1: Android App Base Structure**
  - [ ] Subtask 7.1.1: Initialize Kotlin Jetpack Compose Android app project targeting minimum SDK 26+.
  - [ ] Subtask 7.1.2: Configure MVVM architecture dependencies (Jetpack ViewModel, Coroutines, StateFlow, Hilt/Koin DI).
- [ ] **Task 7.2: BLE Client Controller & Binary Serialization**
  - [ ] Subtask 7.2.1: Implement `BleManager` repository for Bluetooth scanning, discovery, and GATT connection lifecycle.
  - [ ] Subtask 7.2.2: Implement binary packet builder utilities (`buildLightingPacket()`, `buildAudioPacket()`) and byte parser unit tests.

---

### Phase 8: Android User Interface & Preset Management
- [ ] **Task 8.1: Jetpack Compose User Interface**
  - [ ] Subtask 8.1.1: Build Device Scanner screen with RSSI indicators and connection status feedback.
  - [ ] Subtask 8.1.2: Build Remote Control Dashboard with color pickers, animation pattern selectors, brightness/speed sliders, and melody player controls.
- [ ] **Task 8.2: Preset Storage & Management**
  - [ ] Subtask 8.2.1: Implement `PresetRepository` using Jetpack DataStore to persist custom audio-visual presets.
  - [ ] Subtask 8.2.2: Build Preset Manager UI for creating, saving, and triggering custom holiday themes.

---

### Phase 9: System Integration, Testing & Verification
- [ ] **Task 9.1: Automated Test Suites**
  - [ ] Subtask 9.1.1: Implement Kotlin unit tests for Android ViewModel state management and BLE serialization.
  - [ ] Subtask 9.1.2: Implement ESP-IDF Unity unit tests for C binary parser and RTTTL score decoder.
- [ ] **Task 9.2: Hardware Integration & UI Workflow Verification**
  - [ ] Subtask 9.2.1: Perform end-to-end integration testing between Android client and ESP32-C6 hardware.
  - [ ] Subtask 9.2.2: Capture UI workflow screenshots and verify telemetry state synchronization under continuous BLE operation.
