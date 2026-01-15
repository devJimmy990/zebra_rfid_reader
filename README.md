# rfid_zebra_reader

[![pub package](https://img.shields.io/pub/v/rfid_zebra_reader.svg)](https://pub.dev/packages/rfid_zebra_reader)
[![GitHub](https://img.shields.io/github/stars/devJimmy990/rfid_zebra_reader?style=social)](https://github.com/devJimmy990/rfid_zebra_reader)

A Flutter plugin for seamless integration with Zebra RFID readers. Built specifically for Zebra TC27 and compatible devices, providing real-time tag scanning, antenna power control, and full Android 13+ support.

---

## 📦 Installation

Add to your `pubspec.yaml`:

```yaml
dependencies:
  rfid_zebra_reader: ^0.0.2
```

Then run:

```bash
flutter pub get
```

---

## ⚙️ Android Configuration

### **IMPORTANT:** Add Maven Repository

The Zebra RFID SDK is hosted on GitHub. You need to add the repository to your app's Gradle configuration.

<details>
<summary><b>📘 Kotlin DSL</b> (build.gradle.kts) - Click to expand</summary>

<br>

If your app uses **Kotlin DSL**, add this to `your_app/android/build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        
        // Add Zebra RFID SDK repository
        maven {
            url = uri("https://raw.githubusercontent.com/devJimmy990/rfid_zebra_reader/main/android/maven")
        }
    }
}
```

</details>

<details>
<summary><b>📗 Groovy</b> (build.gradle) - Click to expand</summary>

<br>

If your app uses **Groovy**, add this to `your_app/android/build.gradle`:

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        
        // Add Zebra RFID SDK repository
        maven {
            url "https://raw.githubusercontent.com/devJimmy990/rfid_zebra_reader/main/android/maven"
        }
    }
}
```

</details>

<br>

**That's it!** The plugin will automatically download and configure the Zebra SDK.

---

## 🚀 Quick Start

```dart
import 'package:rfid_zebra_reader/rfid_zebra_reader.dart';

// Initialize SDK
await ZebraRfidReader.initialize();

// Listen for tag events
ZebraRfidReader.eventStream.listen((event) {
  if (event.type == RfidEventType.tagRead) {
    print('Tags: ${event.tags}');
  }
});

// Connect to reader
await ZebraRfidReader.connect();

// Start scanning
await ZebraRfidReader.startInventory();

// Stop scanning
await ZebraRfidReader.stopInventory();
```

---

## 📚 Main Functions

### **Core Methods**

| Method | Description |
|--------|-------------|
| `initialize()` | Initialize the Zebra RFID SDK |
| `getAllAvailableReaders()` | Get list of available RFID readers |
| `connect({String? readerName})` | Connect to RFID reader (auto-selects if name not provided) |
| `disconnect()` | Disconnect from current reader |
| `isConnected()` | Check if reader is connected |
| `startInventory()` | Start scanning for RFID tags |
| `stopInventory()` | Stop tag scanning |
| `setAntennaPower(int level)` | Set antenna power (0-270 dBm) |
| `getAntennaPower()` | Get current antenna power level |
| `getPlatformVersion()` | Get Android platform version |

### **Event Stream**

| Property | Type | Description |
|----------|------|-------------|
| `eventStream` | `Stream<RfidEvent>` | Listen for real-time RFID events |

---

## 📋 Models & Classes

### **Models** (`lib/src/models/`)

<details>
<summary><b>RfidEvent</b> - RFID event representation</summary>

<br>

**Properties:**

- `type` - Event type (tagRead, trigger, connected, disconnected, error)
- `tags` - List of scanned tags (for tagRead events)
- `triggerPressed` - Trigger state (for trigger events)
- `errorMessage` - Error description (for error events)
- `readerName` - Reader name (for connection events)

</details>

<details>
<summary><b>RfidTag</b> - Scanned RFID tag</summary>

<br>

**Properties:**

- `tagId` - EPC tag identifier
- `rssi` - Signal strength (dBm)
- `antennaId` - Antenna that detected the tag
- `count` - Number of times tag was read

</details>

---

### **Services** (`lib/src/services/`)

<details>
<summary><b>ZebraRfidReader</b> - Main service class</summary>

<br>

**Key Features:**

- SDK initialization and lifecycle management
- Reader connection and configuration
- Tag inventory operations
- Antenna power control
- Event streaming with full logging

</details>

<details>
<summary><b>AppLogger</b> - Logging utility</summary>

<br>

**Features:**

- Multi-level logging (debug, info, warning, error, critical)
- In-memory log storage (max 500 entries)
- Export logs to clipboard
- Real-time log updates via ChangeNotifier

</details>

---

### **UI Screens** (`lib/src/screens/`)

<details>
<summary><b>LogViewerScreen</b> - Debug log viewer</summary>

<br>

**Features:**

- Color-coded log levels
- Filter by log level
- Auto-scroll toggle
- Copy logs to clipboard
- Clear log history
- Expandable entries with error details

</details>

---

## 🎯 Event Types

```dart
enum RfidEventType {
  tagRead,          // Tags were scanned
  trigger,          // Hardware trigger pressed/released
  connected,        // Reader connected
  disconnected,     // Reader disconnected
  readerAppeared,   // New reader detected
  readerDisappeared,// Reader removed
  initialized,      // SDK initialized
  error,            // Error occurred
  unknown,          // Unknown event
}
```

---

## 🛠️ Requirements

- **Device:** Zebra TC27 or compatible Zebra RFID device
- **Android:** API 26+ (Android 8.0+) with full Android 13+ support
- **Flutter:** 3.3.0+
- **Dart:** 3.0.0+

⚠️ **Note:** Requires actual Zebra RFID hardware. Will not work in emulators.

---

## 🔧 Troubleshooting

<details>
<summary><b>❌ Build Error: "Could not find com.zebra.rfid:rfid-api3:2.0.5.238"</b></summary>

<br>

**Solution:** You forgot to add the Maven repository! See the [Android Configuration](#️-android-configuration) section above.

</details>

<details>
<summary><b>🔍 No Readers Found</b></summary>

<br>

**Solutions:**

- Ensure Bluetooth is enabled
- Run on actual Zebra device (not emulator)
- Grant all required permissions
- Restart the device

</details>

<details>
<summary><b>🔌 Connection Failed</b></summary>

<br>

**Solutions:**

- Check reader is powered on
- Verify Bluetooth connection
- Ensure no other app is using the reader
- Try disconnecting and reconnecting

</details>

---

## 📖 API Documentation

For complete API documentation, visit: [pub.dev/documentation/rfid_zebra_reader](https://pub.dev/documentation/rfid_zebra_reader/latest/)

---

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting a PR.

---

## 👨‍💻 Author

**Jimmy (@devJimmy990)**

- GitHub: [@devJimmy990](https://github.com/devJimmy990)
- Plugin: [rfid_zebra_reader](https://github.com/devJimmy990/rfid_zebra_reader)

---

## 🙏 Acknowledgments

- Built with Zebra RFID SDK v2.0.5.238
- Designed for Zebra TC27 and compatible devices
- Special thanks to the Flutter community

---

## 📞 Support

- **Issues:** [GitHub Issues](https://github.com/devJimmy990/rfid_zebra_reader/issues)
- **Discussions:** [GitHub Discussions](https://github.com/devJimmy990/rfid_zebra_reader/discussions)
- **Documentation:** [pub.dev](https://pub.dev/packages/rfid_zebra_reader)

---

**⭐ If this plugin helped you, please star the repo!**

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/devJimmy990">devJimmy990</a>
</p>
