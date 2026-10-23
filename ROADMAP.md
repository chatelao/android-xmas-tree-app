# Smart Neopixel Xmas Tree Remote Control System - Roadmap

## Progress Overview

| Phase | Description | Status |
| :--- | :--- | :---: |
| **Phase 1** | Repository Infrastructure & Toolchain Setup | ✅ |
| **Phase 2** | Technical Interfaces & Binary Protocol Specifications | ✅ |
| **Phase 3** | Core Architecture Stubs & Interface Definitions | ✅ |
| **Phase 4** | Subsystem Implementations (Android App & ESP32-C6 Firmware) | ⏳ |
| **Phase 5** | Integration, Preset Management & System Telemetry | ⏳ |
| **Phase 6** | System Verification, Automated Testing & CI/CD Pipeline | ⏳ |

---

## Goals

* ⏳ **Wireless Control:** Establish reliable Bluetooth Low Energy (BLE) communication between Android app and ESP32-C6 microcontroller.
* ⏳ **Dynamic Lighting Engine:** Drive WS2812B/SK6812 Neopixel LED strip using ESP32-C6 hardware RMT peripheral supporting custom patterns, brightness, and speed adjustments.
* ⏳ **Audio Melody Playback:** Drive a speaker/buzzer via LEDC PWM peripheral to parse and play RTTTL festive melodies.
* ⏳ **Preset & Automation Management:** Provide user-defined preset saving and instant recall across Android Jetpack DataStore and ESP32-C6 NVS storage.
* ⏳ **Modern Android User Interface:** Deliver an intuitive Jetpack Compose UI following Material 3 guidelines for real-time control and monitoring.
* ⏳ **Automated Quality Assurance:** Implement robust CI/CD workflows, automated unit tests, and Playwright UI test verification.

---

## Phases

### Phase 1: Repository Infrastructure & Toolchain Setup

Focus on establishing the environment, build scripts, and foundation files needed for full development.

* [x] **Task 1.1: Build & Environment Preparation** ✅
  * [x] Create `src/install.sh` for preparing application development and build tools.
  * [x] Create `test/install.sh` for installing test runners, Playwright, and validation tools.
* [x] **Task 1.2: CI/CD Pipeline Skeleton** ✅
  * [x] Set up initial GitHub Actions workflow configuration.
  * [x] Configure dependency caching for Gradle and ESP-IDF builds.

---

### Phase 2: Technical Interfaces & Binary Protocol Specifications

Interface-first approach: Define shared byte frames, data models, and GATT specifications before functional implementations.

* [x] **Task 2.1: BLE GATT Profile Definition** ✅
  * [x] Define Custom Service UUID (`12345678-1234-5678-1234-56789abcdef0`).
  * [x] Specify Lighting Control Characteristic (`...abcdef1`), Audio Control Characteristic (`...abcdef2`), and Telemetry Characteristic (`...abcdef3`).
* [x] **Task 2.2: Binary Protocol Specification Stubs** ✅
  * [x] Define Lighting Control byte frame parser interface (8 bytes: Command, Pattern ID, RGB, Brightness, Speed, Reserved).
  * [x] Define Audio Control byte frame parser interface (6 bytes: Command, Melody ID, State, Volume, Tempo, Reserved).
  * [x] Define System Status / Telemetry payload structure (10 bytes: Uptime, Active Pattern, Active Melody, Brightness, Volume, Connection State, Error Code).

---

### Phase 3: Core Architecture Stubs & Interface Definitions

Create modular stubs for both Android and ESP32-C6 to enable parallel development.

* [x] **Task 3.1: Android Architectural Stubs** ✅
  * [x] Implement `BleManager` interface for GATT scanning, connection management, and frame serialization.
  * [x] Implement `MainViewModel` UI state contracts (`StateFlow`) and UI interaction interfaces.
  * [x] Implement `PresetRepository` interface contracts for saving/loading user configurations.
* [x] **Task 3.2: ESP32-C6 Firmware Architecture Stubs** ✅
  * [x] Define FreeRTOS task handles and synchronization queues (`xLedQueue`, `xAudioQueue`).
  * [x] Define `nimble_host_task` GATT server write/read callback interfaces.
  * [x] Define `led_engine_task` interface for receiving animation frames.
  * [x] Define `audio_engine_task` interface for parsing tone/melody commands.
  * [x] Define `nvs_manager` interface for key-value flash storage.

---

### Phase 4: Subsystem Implementations

Parallel implementation of client and firmware functionality against established interfaces.

* [ ] **Task 4.1: Android Application Implementation** ⏳
  * [ ] Implement Jetpack Compose UI (Device Scan Screen, Remote Control Dashboard, Preset Manager Screen).
  * [ ] Implement `android.bluetooth` connection handling with Kotlin Coroutines and `callbackFlow`.
  * [ ] Implement binary frame encoder for lighting and audio control commands.
* [ ] **Task 4.2: ESP32-C6 Neopixel Lighting Engine** ⏳
  * [ ] Implement RMT peripheral driver for WS2812B/SK6812 timing on GPIO 8.
  * [ ] Implement pattern rendering algorithms (Static Color, Rainbow Wave, Sparkle, Festive Candle, Twinkle).
* [ ] **Task 4.3: ESP32-C6 Audio / Melody Engine** ⏳
  * [ ] Implement LEDC PWM peripheral driver on GPIO 19.
  * [ ] Implement RTTTL melody score parser and playback controller (Jingle Bells, We Wish You a Merry Christmas, Silent Night, Deck the Halls).

---

### Phase 5: Integration, Preset Management & System Telemetry

Connect client and firmware subsystems, enabling end-to-end user features.

* [ ] **Task 5.1: Device State Synchronization & Telemetry** ⏳
  * [ ] Connect ESP32-C6 `cmd_dispatcher_task` to dispatch incoming BLE byte payloads to queue handlers.
  * [ ] Enable periodic telemetry notifications over BLE GATT Telemetry Characteristic.
  * [ ] Update Android `MainViewModel` to reactively reflect current tree status.
* [ ] **Task 5.2: Preset Management Implementation** ⏳
  * [ ] Implement local Jetpack DataStore persistence on Android for saving multi-parameter themes.
  * [ ] Implement ESP32-C6 NVS storage to persist active default pattern, volume, and brightness across reboots.

---

### Phase 6: System Verification, Automated Testing & CI/CD Pipeline

Ensure system quality, reliability, and automated validation.

* [ ] **Task 6.1: Automated Unit Testing** ⏳
  * [ ] Write Android unit tests using JUnit 5 and MockK for `BleManager` frame packing/unpacking and ViewModel transitions.
  * [ ] Write ESP32-C6 firmware unit tests using Espressif Unity for binary packet parsing and RTTTL score conversion.
* [ ] **Task 6.2: Frontend UI & Workflow Verification** ⏳
  * [ ] Implement Playwright tests for capturing Compose UI screens and end-to-end workflows.
* [ ] **Task 6.3: CI/CD Finalization** ⏳
  * [ ] Finalize GitHub Actions workflow to run unit tests, build Android APK, compile firmware binary, and capture UI test screenshots.
