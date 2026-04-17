package com.marty.blutilities.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Central helper for Bluetooth permission management.
 */
object BluetoothPermissionHelper {

    val permission: String
        get() = Manifest.permission.BLUETOOTH_CONNECT

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}