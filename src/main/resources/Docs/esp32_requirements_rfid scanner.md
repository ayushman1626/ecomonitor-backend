# Hardware & Firmware Requirements: ESP32 + MFRC522 BLE RFID Scanner

This document outlines the requirements and configuration parameters for developing the physical RFID scanner device.

---

## 1. Hardware Architecture

### Components
1.  **Microcontroller**: ESP32 Development Board (e.g., ESP32-WROOM-32D).
2.  **RFID Reader Module**: MFRC522 (13.56 MHz RFID reader/writer).
3.  **RFID Tags**: MIFARE Classic 1K or compatible passive tags (mounted on the smart bins).
4.  **Feedback Peripherals**:
    *   **Status LED**: Red/Green or RGB LED to indicate BLE connection and scanning status.
    *   **Buzzer**: Active 5V piezo buzzer for audible scan feedback.
5.  **Power Supply**: Rechargeable Li-Po/Li-ion battery (with a TP4056 charging board or integrated battery module) to make it portable.

### ESP32 to MFRC522 Pin Connections (SPI)

| MFRC522 Pin | ESP32 Pin (VSPI) | Purpose |
| :--- | :--- | :--- |
| **SDA (SS)** | GPIO 5 | Chip Select / SPI Slave Select |
| **SCK** | GPIO 18 | SPI Clock |
| **MOSI** | GPIO 23 | Master Out Slave In |
| **MISO** | GPIO 19 | Master In Slave Out |
| **IRQ** | *Not Connected* | Interrupt request (not required for polling) |
| **GND** | GND | Ground |
| **RST** | GPIO 22 | Reset Pin |
| **3.3V** | 3.3V | VCC (Power) |

*Caution: Connecting MFRC522 to 5V will damage the board. Ensure it is connected to the 3.3V pin of the ESP32.*

---

## 2. BLE Configuration & GATT Specifications

The ESP32 must initialize a BLE server and configure the following GATT attributes:

### A. Device Info Advertising
*   **Advertising Name**: `EcoMonitor-RFID-Scanner` (or similar prefix).
*   **Advertised Service**: `4F4D4E49-0001-1000-8000-00805F9B34FB` (Service UUID).

### B. Service Profile & GATT Attributes
```
Service UUID: 4F4D4E49-0001-1000-8000-00805F9B34FB

  ├── Characteristic 1 (Command Characteristic)
  │     UUID: 4F4D4E49-0002-1000-8000-00805F9B34FB
  │     Properties: WRITE
  │     Value size: 1 byte
  │
  └── Characteristic 2 (Data/Tag Characteristic)
        UUID: 4F4D4E49-0003-1000-8000-00805F9B34FB
        Properties: READ, NOTIFY
        Value size: Variable (up to 32 bytes)
```

---

## 3. Firmware Logic & Behavior Requirements

### A. Initialization (On Boot)
1.  Initialize SPI bus and MFRC522 chip.
2.  Initialize the BLE Device, Server, and advertising flags.
3.  Start advertising BLE services.
4.  LED Status: **Blinking Blue/Red** (Slowly) - Waiting for client connection.

### B. Bluetooth Connection & Handshake
1.  Acknowledge connection request from the mobile application.
2.  Stop advertising once connected to conserve energy.
3.  LED Status: **Solid Blue/Green** (Dim) - Connected.

### C. Processing the "Scan" Command (Write trigger)
1.  Monitor the **Command Characteristic** (`4F4D4E49-0002-1000-8000-00805F9B34FB`).
2.  When the client writes value `0x01`:
    *   Initialize a **10-second scanning timer** in code.
    *   Set LED Status: **Fast Blinking Green** - Scanning Active.
    *   Activate the MFRC522 card-seeking routine (`PICC_IsNewCardPresent()` and `PICC_ReadCardSerial()`).

### D. RFID Tag Scanning Outcomes
*   **Outcome 1: Tag Detected (Success)**
    *   Read the tag's Unique Identifier (UID).
    *   Convert the UID bytes into a formatted uppercase hexadecimal string (e.g., `"53 A2 9B 1C"`).
    *   Write the UID string to the **Data Characteristic** (`4F4D4E49-0003-1000-8000-00805F9B34FB`).
    *   Send a BLE **Notify** containing the tag string to the subscribed mobile app.
    *   Sound the Buzzer: **Single short beep (200ms)**.
    *   LED Status: **Solid Green (for 1.5s)** then return to connected state.
    *   Exit scanning state.
*   **Outcome 2: Timeout (Failure)**
    *   If no tag is read within 10 seconds.
    *   Write the string `"TIMEOUT_ERR"` to the **Data Characteristic**.
    *   Send a BLE **Notify** containing `"TIMEOUT_ERR"`.
    *   Sound the Buzzer: **Double short beeps (100ms on, 100ms off, 100ms on)**.
    *   LED Status: **Solid Red (for 1.5s)** then return to connected state.
    *   Exit scanning state.

### E. Disconnection Lifecycle
1.  Upon mobile client disconnection or connection drop, clean up BLE memory handles.
2.  Re-start BLE Advertising.
3.  LED Status: **Blinking Blue/Red** (Slowly).

---

## 4. Power Management & Reliability

*   **Sleep Mode**: If no BLE connection is active for 5 consecutive minutes, the ESP32 should enter **Deep Sleep** (saving battery).
*   **Wake up**: A physical momentary push button should be wired to a GPIO pin (configured as external wakeup interrupt, e.g., GPIO 12 or 33) to wake up the ESP32 from Deep Sleep.
*   **Battery Telemetry (Optional)**: Provide a 10K/10K resistor voltage divider linked to an ADC pin to read battery voltage level. If the battery is below 3.4V, flash the status LED red to warn the driver.
