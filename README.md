# HotspotIPMonitor

![Android](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

**HotspotIPMonitor** is an Android application designed to scan and monitor devices connected to a mobile hotspot.

It helps users discover connected devices on their local network by analyzing IP addresses, network information, MAC addresses, and device manufacturers.

---

## ✨ Features

### Network monitoring

* Detect active hotspot status
* Display local network information
* Show gateway IP address
* Display subnet and available IP range
* Identify the active network interface

### Device discovery

* Scan connected devices on the hotspot network
* Detect devices using:

  * ICMP ping
  * TCP connection fallback
  * ARP table analysis
* Display:

  * IP address
  * MAC address (when available)
  * Manufacturer information
  * Detection method

### User interface

* Modern Material 3 interface
* Built with Jetpack Compose
* Real-time scan progress
* Copy IP addresses directly from the interface

---

## 📱 Screenshots

*Add application screenshots here.*

Example:

```
screenshots/
├── home.png
├── scanning.png
└── devices.png
```

---

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose
* **Architecture:** MVVM
* **Build System:** Gradle Kotlin DSL
* **Minimum Android SDK:** Android compatible device
* **Java Version:** JDK 17

---

## 📂 Project Structure

```
HotspotIPMonitor
│
├── app/
│   └── src/main/java/com/sky/hotspotmonitor
│
├── net/
│   ├── HotspotScanner.kt
│   ├── NetworkInfoProvider.kt
│   ├── OuiLookup.kt
│   └── Models.kt
│
├── ui/
│   ├── MainScreen.kt
│   └── Theme.kt
│
├── MainActivity.kt
└── MainViewModel.kt
```

---

## 🚀 Build the project

### Requirements

* Android Studio
* JDK 17
* Android SDK

Clone the repository:

```bash
git clone git@github.com:AGESILAS3118/HotspotIPMonitor.git

cd HotspotIPMonitor
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

The generated APK will be available at:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔍 How it works

The application scans the hotspot subnet and checks available IP addresses.

Detection strategy:

1. ICMP reachability test
2. TCP port fallback detection
3. ARP table analysis for MAC addresses

The application works locally and does not require an external server.

---

## 🔐 Permissions

The application uses only network-related permissions:

```xml
INTERNET
ACCESS_WIFI_STATE
ACCESS_NETWORK_STATE
```

These permissions are required to analyze the local network.

---

## 🤝 Contributing

Contributions are welcome.

To contribute:

1. Fork the repository
2. Create a new branch

```bash
git checkout -b feature/my-feature
```

3. Commit your changes

```bash
git commit -m "Add new feature"
```

4. Push your branch

```bash
git push origin feature/my-feature
```

5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author

Created by **AGESILAS3118**

````



