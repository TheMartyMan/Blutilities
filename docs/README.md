**English** | [Magyar](README_HU.md)

<p align="center">
  <img src="images/icon.png" width="128" height="128" alt="Blutilities icon">
  <h1 align="center">Blutilities</h1>
</p>

<p align="center">
  <strong>Bluetooth audio codec management directly from your Quick Settings.</strong>
</p>

<br>

## Overview

**Blutilities** is a lightweight, Material Design-inspired Android utility that makes managing your Bluetooth audio experience effortless. Instead of digging deep into Android's Developer Options, Blutilities provides a convenient **Quick Settings Tile** to switch between active audio codecs on the fly. 

It is designed with audiophiles in mind, offering extensive support for high-res codecs—especially **LDAC**—allowing you to easily force specific playback qualities (e.g., 990 kbps, 660 kbps) for your connected A2DP devices.

## Key Features

* 🎛️ **Quick Settings Integration:** 1-tap access to your active Bluetooth audio configuration.
* 🎧 **Smart Codec Switching:** Instantly switch between SBC, AAC, aptX, aptX HD, and LDAC.
* ⚡ **Deep LDAC Control:** Override system defaults and manually select your preferred LDAC playback quality (990 kbps, 660 kbps, 330 kbps, or Adaptive).
* 🎨 **Material Design UI:** A modern, clean, and fluid dialog-style interface that feels completely native to Android.
* 🧠 **Context-Aware:** The tile dynamically updates its state and is only active when a supported Bluetooth A2DP device is connected.

## Supported languages
* English
* Hungarian

Translations are welcome.

## Screenshots

<p align="center">
  <img src="images/screenshot_1.png" width="250" alt="Quick Settings Tile"> &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="images/screenshot_2.png" width="250" alt="Codec Selection Dialog">
</p>

## How to Use

1. Install the app
2. Swipe down twice to open your expanded Quick Settings panel.
3. Tap the **Edit** (pencil) icon.
4. Find **Blutilities** in the available tiles and drag it into your active tiles area.
5. Connect your Bluetooth headphones/earbuds.
6. Tap the tile to open the codec selection dialog and choose your preferred audio quality!

## Permissions

For Blutilities to function correctly, it requires the `BLUETOOTH_CONNECT` permission (on Android 12+) to detect connected devices, read their supported capabilities, and apply your preferred audio codec settings.

---
*Built with Kotlin.*