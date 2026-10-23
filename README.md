# Smart Neopixel Xmas Tree Remote Control System

An Android application and ESP32-C6 Bluetooth Low Energy (BLE) firmware system designed to remotely control an addressable Neopixel Xmas Tree with an integrated loudspeaker/buzzer.

## Overview

The system consists of two primary components:
1. **Android Client Application:** Built with Kotlin, Jetpack Compose, and Material 3, providing an intuitive interface for managing lighting patterns, audio playback, and custom presets over BLE.
2. **ESP32-C6 Firmware:** Developed using ESP-IDF v5.1+ and NimBLE, featuring non-blocking Neopixel driving via the RMT peripheral, RTTTL melody playback via LEDC PWM, and state persistence via NVS flash storage.

## Documentation Links

- [Conceptual Overview & Use Cases (CONCEPT.md)](CONCEPT.md)
- [Detailed Technical Architecture & Specifications (DESIGN.md)](DESIGN.md)
- [Project Roadmap & Status (ROADMAP.md)](ROADMAP.md)
- [Top-Level Architecture Diagram (TOP_ARCHITECTURE.puml)](TOP_ARCHITECTURE.puml)

## Quick Start & Setup

### Environment Preparation

To prepare application build dependencies and directory structures:
```bash
./src/install.sh
```

To prepare test tools and verification suites:
```bash
./test/install.sh
```

## License

This project is open-source and available under the MIT License.
