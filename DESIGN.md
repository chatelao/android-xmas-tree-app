# Detailed System Design: Smart Neopixel Xmas Tree Remote Control System

## 1. System Overview & Architecture

This document provides the detailed technological design for the Smart Neopixel Xmas Tree Remote Control System, deriving implementation choices from `CONCEPT.md` and the top-level project goal.

The system comprises a modern Android application acting as a Bluetooth Low Energy (BLE) client and an ESP32-C6 microcontroller firmware core driving an addressable WS2812B/SK6812 Neopixel LED strip and a PWM-driven loudspeaker/buzzer.

### Top-Level Architecture Diagram
Below is the system module layout and communications architecture.

![Top Architecture Overview](TOP_ARCHITECTURE.puml)

---

## 2. Technical Stack & Toolchain

### 2.1 Android Application Stack
* **Programming Language:** Kotlin 1.9+
* **UI Framework:** Jetpack Compose (Declarative UI) with Material 3 Design Guidelines.
* **Architecture Pattern:** MVVM (Model-View-ViewModel) + Clean Architecture repository pattern.
* **Asynchronous Execution:** Kotlin Coroutines & `StateFlow` / `SharedFlow`.
* **Bluetooth Stack:** Native Android Bluetooth API (`android.bluetooth`) wrapped in a thread-safe reactive BLE Service.
* **Local Persistence:** Jetpack DataStore (Preferences / Proto) for preset management.
* **Testing Stack:** JUnit 5, MockK, AndroidX Test, Robolectric, Playwright (for automated UI/workflow capture).

### 2.2 ESP32-C6 Firmware Stack
* **Development Framework:** ESP-IDF v5.1+ (Official Espressif IoT Development Framework).
* **Operating System:** FreeRTOS (ESP-IDF SMP / dual-core / single-core model).
* **Programming Language:** C (C11 standard) / C++17 where appropriate.
* **BLE Stack:** Apache NimBLE (Lightweight BLE stack integrated into ESP-IDF).
* **Peripheral Drivers:**
  * **Neopixel LEDs:** ESP32 RMT (Remote Control Transceiver) peripheral driver with encoder timing for WS2812B/SK6812.
  * **Audio/Speaker:** LEDC (LED Control) PWM peripheral driver for precise frequency generation and RTTTL parsing.
* **Storage:** ESP-IDF NVS (Non-Volatile Storage) library for saving device state across power cycles.
* **Testing Stack:** Unity Test Framework (embedded C unit tests) & Mock peripherals for host emulation.

### 2.3 Build Tools & CI/CD
* **Android Build System:** Gradle 8.x with Kotlin DSL.
* **Firmware Build System:** CMake & `idf.py`.
* **Continuous Integration:** GitHub Actions runner executing automated test suites, artifact generation, and code quality checks.

---

## 3. Major Technological Choices

### Choice 1: Android BLE Integration Library & Framework
* **Alternative A (Chosen): Native `android.bluetooth` API with Custom Coroutines/Flow Wrapper**
  * *Description:* Build a thin, robust BLE manager layer around Android's native BLE APIs using Kotlin `callbackFlow` and Coroutines.
  * *Pros:* Zero external dependencies, full transparency over connection lifecycles and MTU negotiations, lightweight footprint, no library deprecation risk.
  * *Cons:* Requires managing Android BLE edge-case state machines (e.g., GATT status 133 error handling).
* **Alternative B: Reactive BLE Library (`RxAndroidBle`)**
  * *Description:* Use `RxAndroidBle` library based on RxJava for BLE operations.
  * *Pros:* Handles connection state transitions and retries automatically.
  * *Cons:* Requires importing heavy RxJava dependencies into a Jetpack Compose / Coroutines idiomatic codebase; added project complexity.
* **Alternative C: Nordic Semiconductor Android BLE Library (`no.nordicsemi.android:ble`)**
  * *Description:* Use Nordic's BLE library for Android.
  * *Pros:* Well-tested library abstraction for queueing BLE requests.
  * *Cons:* Additional third-party dependency; opinionated architectural constraints that may clash with Compose ViewModel state modeling.

### Choice 2: Firmware Operating System & Task Architecture
* **Alternative A (Chosen): FreeRTOS Task & Queue Multitasking (ESP-IDF Native)**
  * *Description:* Separate BLE stack handling, LED animation rendering, and RTTTL audio playback into dedicated FreeRTOS tasks communicating via synchronized queues.
  * *Pros:* Excellent real-time performance; prevents animation frame drops while playing complex melodies or processing incoming BLE requests.
  * *Cons:* Requires careful queue sizing and concurrency locking (mutexes) for shared state.
* **Alternative B: Single Thread Bare-Metal Event Loop**
  * *Description:* Execute all logic inside a continuous `while(1)` loop using millisecond timer checks.
  * *Pros:* Simple memory structure, no task context switching overhead.
  * *Cons:* High risk of timing jitter; Neopixel driver timing or long audio tones can block incoming BLE packet processing.
* **Alternative C: Arduino Core on ESP32-C6 (`Arduino.h`)**
  * *Description:* Use Arduino framework wrappers (`Adafruit_NeoPixel`, `Tone` library).
  * *Pros:* Quick prototype setup using community libraries.
  * *Cons:* Less hardware control, overhead in memory/flash footprint, less optimal BLE stack integration compared to ESP-IDF NimBLE.

### Choice 3: BLE Command Protocol & Payload Serialization
* **Alternative A (Chosen): Compact Fixed-Header Binary Byte Frame Protocol**
  * *Description:* Binary payload format with opcode header, parameter fields, and optional checksum.
  * *Pros:* Extremely small memory footprint, low latency transmission, minimal byte size (fits easily within default 23-byte ATT MTU).
  * *Cons:* Requires byte-level packing/unpacking documentation and bitwise parsing in both Kotlin and C.
* **Alternative B: JSON String Serialization over GATT**
  * *Description:* Send formatted JSON strings (e.g., `{"pattern": 2, "color": "#FF0000"}`).
  * *Pros:* Human-readable, easy to extend with new key-value pairs.
  * *Cons:* Requires multi-packet fragment handling due to GATT MTU limits; slow string parsing overhead on microcontroller.
* **Alternative C: Protocol Buffers (Protobuf) over GATT**
  * *Description:* Define `.proto` schemas and compile binary stubs for Kotlin and C.
  * *Pros:* Strictly typed, auto-generated serialization stubs.
  * *Cons:* Additional build toolchain dependencies (Protobuf compiler), bloats firmware binary size.

### Choice 4: Neopixel LED Driver Hardware Strategy
* **Alternative A (Chosen): ESP32-C6 RMT (Remote Control) Peripheral Driver**
  * *Description:* Use hardware RMT peripheral to output precise sub-microsecond pulse trains required by WS2812B timing specifications.
  * *Pros:* Completely non-blocking DMA/RMT hardware timing, immune to CPU interrupt delays, native ESP-IDF driver support.
  * *Cons:* Requires configuring RMT symbol timing descriptors.
* **Alternative B: Bit-Banging GPIO with Disabled Interrupts**
  * *Description:* Directly toggle GPIO pins in assembly/C loops while temporarily disabling CPU interrupts.
  * *Pros:* No specialized hardware peripheral required.
  * *Cons:* Disabling interrupts causes BLE packet drops and audio clock jitter; highly unsafe for real-time systems.
* **Alternative C: SPI Peripheral DMA Emulation**
  * *Description:* Output pixel data over SPI MOSI line by formatting SPI bytes to mimic WS2812B high/low signal ratios.
  * *Pros:* Fast DMA transfer.
  * *Cons:* Consumes SPI hardware bus that may be needed for external memory or displays; higher memory overhead per LED pixel.

### Choice 5: Preset & Configuration Storage Strategy
* **Alternative A (Chosen): ESP32-C6 NVS (Non-Volatile Storage) + Android Jetpack DataStore**
  * *Description:* Android app stores user-created presets in local DataStore. Firmware persists current system defaults (active pattern, volume, brightness) in ESP32 NVS flash partition.
  * *Pros:* Fast wear-leveled flash key-value access on ESP32; instant preset recall in Android app.
  * *Cons:* Requires key-length and data-type management in NVS.
* **Alternative B: SPIFFS / LittleFS File System on ESP32 Flash**
  * *Description:* Create a flash filesystem partition to store preset files as JSON files.
  * *Pros:* Arbitrary file size storage.
  * *Cons:* Unnecessary partition allocation and file I/O overhead for small state variables.
* **Alternative C: EEPROM Emulation Partition**
  * *Description:* Emulate legacy raw EEPROM byte addresses in flash.
  * *Pros:* Simple byte address access.
  * *Cons:* Outdated pattern in ESP-IDF; lacks key-value indexing and type safety.

---

## 4. Technical Interfaces & Protocol Specifications

### 4.1 Bluetooth Low Energy (BLE) GATT Profile
* **Device Name:** `XmasTree-ESP32C6`
* **Custom Service UUID:** `12345678-1234-5678-1234-56789abcdef0`

#### GATT Characteristics Table

| Characteristic Name | UUID | Properties | Description |
| :--- | :--- | :--- | :--- |
| **Lighting Control** | `12345678-1234-5678-1234-56789abcdef1` | Write, Write w/o Resp | Controls LED animation, colors, brightness, speed. |
| **Audio Control** | `12345678-1234-5678-1234-56789abcdef2` | Write, Write w/o Resp | Controls melody playback, tone generation, volume. |
| **System Status / Telemetry** | `12345678-1234-5678-1234-56789abcdef3` | Read, Notify | Telemetry feedback (active mode, uptime, errors). |

---

### 4.2 Binary Protocol Payload Structures

#### 4.2.1 Lighting Control Payload (8 Bytes)

| Byte Offset | Field Name | Data Type | Range / Description |
| :---: | :--- | :---: | :--- |
| `0` | `Command ID` | `uint8_t` | `0x01` = Set Pattern, `0x02` = Set Static Color, `0x03` = Set Brightness |
| `1` | `Pattern ID` | `uint8_t` | `0`: Off, `1`: Static, `2`: Rainbow Wave, `3`: Sparkle, `4`: Festive Candle, `5`: Twinkle |
| `2` | `Red` | `uint8_t` | Primary Red component (`0 - 255`) |
| `3` | `Green` | `uint8_t` | Primary Green component (`0 - 255`) |
| `4` | `Blue` | `uint8_t` | Primary Blue component (`0 - 255`) |
| `5` | `Brightness` | `uint8_t` | Global LED Brightness (`0 - 100` %) |
| `6` | `Animation Speed`| `uint8_t` | Speed scaling (`1 - 100`, where 100 = fastest) |
| `7` | `Reserved` | `uint8_t` | Padding / Future alignment (`0x00`) |

#### 4.2.2 Audio Control Payload (6 Bytes)

| Byte Offset | Field Name | Data Type | Range / Description |
| :---: | :--- | :---: | :--- |
| `0` | `Command ID` | `uint8_t` | `0x10` = Play Song, `0x11` = Stop Audio, `0x12` = Play Frequency Tone, `0x13` = Set Volume |
| `1` | `Melody ID` | `uint8_t` | `0`: Silent, `1`: Jingle Bells, `2`: We Wish You a Merry Christmas, `3`: Silent Night, `4`: Deck the Halls |
| `2` | `Playback State` | `uint8_t` | `0x00`: Stop, `0x01`: Play, `0x02`: Pause |
| `3` | `Volume Level` | `uint8_t` | Speaker Volume (`0 - 100` %) |
| `4` | `Tempo Modifier` | `uint8_t` | Tempo multiplier (`50 - 200` %, 100 = default speed) |
| `5` | `Reserved` | `uint8_t` | Padding (`0x00`) |

#### 4.2.3 System Status / Telemetry Payload (10 Bytes - Read/Notify)

| Byte Offset | Field Name | Data Type | Range / Description |
| :---: | :--- | :---: | :--- |
| `0 - 3` | `System Uptime` | `uint32_t` | Uptime in seconds (Little Endian) |
| `4` | `Current Pattern` | `uint8_t` | Active LED Pattern ID |
| `5` | `Current Melody` | `uint8_t` | Active Melody ID |
| `6` | `Current Brightness`| `uint8_t` | Current Brightness (`0 - 100` %) |
| `7` | `Current Volume` | `uint8_t` | Current Volume (`0 - 100` %) |
| `8` | `Connection State` | `uint8_t` | `0`: Disconnected, `1`: Connected |
| `9` | `Error Code` | `uint8_t` | `0x00`: OK, `0x01`: NVS Error, `0x02`: LED Timing Error |

---

## 5. Component Architecture

### 5.1 Android Software Components (MVVM)
* **`MainActivity` / Compose Navigation:** Application lifecycle host and screen composition (Device Scan Screen, Remote Control Dashboard, Preset Manager Screen).
* **`MainViewModel`:** Maintains UI state (`StateFlow`), handles user interaction events, triggers BLE command dispatches.
* **`BleManager` Repository:** Encapsulates BLE connection lifecycle, GATT scanning, auto-reconnect, and binary packet building (`buildLightingPacket()`, `buildAudioPacket()`).
* **`PresetRepository`:** Jetpack DataStore store for saving/loading user audio-visual presets.

### 5.2 ESP32-C6 Firmware Components (FreeRTOS Tasks)
* **`nimble_host_task`:** Handles BLE Advertising, GATT server callbacks for Characteristic Write/Read events.
* **`cmd_dispatcher_task`:** Receives raw GATT byte arrays from BLE callbacks, validates packet integrity, and dispatches to system queues.
* **`led_engine_task`:** Priority: High. Reads pattern commands from `xLedQueue`, renders frame buffers, and pushes to hardware via RMT driver.
* **`audio_engine_task`:** Priority: Medium. Reads song/tone commands from `xAudioQueue`, parses RTTTL score strings, and drives LEDC PWM output pin.
* **`nvs_manager`:** Saves last-used settings to flash and reloads them at boot.

### 5.3 Hardware Pin & Peripheral Allocation Table

| Peripheral | ESP32-C6 Pin | Hardware Resource | Operational Mode |
| :--- | :---: | :--- | :--- |
| **Neopixel LED Strip Data** | `GPIO 8` | RMT Channel 0 | Pulse-code encoding (800 kHz bit clock) |
| **Audio Loudspeaker / Buzzer** | `GPIO 19` | LEDC Timer 0 / Channel 0 | Variable frequency PWM (Tone generation) |
| **Status Indicator LED** | `GPIO 15` | Standard GPIO | Output (Blink on BLE activity) |
| **Power Supply** | `5V / VBUS` | External or USB-C | Powers LEDs & ESP32-C6 |

---

## 6. Testing & CI/CD Strategy

1. **Android Unit Testing:**
   * Unit tests for `BleManager` byte serialization/deserialization logic using JUnit 5.
   * ViewModel unit tests using MockK to verify state flow transitions on command dispatch.
2. **Firmware Unit & Integration Testing:**
   * Embedded C unit tests using Espressif Unity test framework for binary packet parser and state machine.
   * RTTTL melody parser test suite checking note-to-frequency conversions.
3. **Automated Verification Scripts:**
   * `src/install.sh`: Script to prepare application build environments.
   * `test/install.sh`: Script to set up test runners and validation tools.

---

## 7. Summary of Discarded Alternatives

| Category | Discarded Alternative | Rationale for Discarding |
| :--- | :--- | :--- |
| **Android BLE Stack** | `RxAndroidBle` (Alt B) | Unnecessary RxJava dependency overhead in Kotlin Coroutine/Compose app. |
| **Android BLE Stack** | Nordic BLE Library (Alt C) | Added external dependency with rigid opinionated architecture. |
| **Firmware Architecture** | Single Thread Bare-Metal Event Loop (Alt B) | Causes animation stutters and missed BLE GATT interrupts under load. |
| **Firmware Architecture** | Arduino Core for ESP32-C6 (Alt C) | Heavy abstractions, higher memory footprint, and less direct RMT/LEDC control. |
| **Protocol Serialization** | JSON Serialization over GATT (Alt B) | Payload sizes exceed default GATT MTU, causing string parsing overhead on CPU. |
| **Protocol Serialization** | Protobuf over GATT (Alt C) | Complex toolchain requirement and bloated binary firmware size. |
| **LED Hardware Strategy** | Bit-Banging GPIO (Alt B) | Unsafe interrupt disabling causes BLE drops and audio clock jitter. |
| **LED Hardware Strategy** | SPI DMA Output (Alt C) | Consumes hardware SPI bus and increases memory overhead per pixel. |
| **Storage Strategy** | SPIFFS / LittleFS Partition (Alt C) | Excessive overhead for storing small key-value configuration flags. |
| **Storage Strategy** | EEPROM Emulation (Alt B) | Deprecated pattern in ESP-IDF; lacks wear-leveling and key-value naming. |
