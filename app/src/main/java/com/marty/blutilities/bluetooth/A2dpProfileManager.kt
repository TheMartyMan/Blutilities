package com.marty.blutilities.bluetooth

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context

/**
 * BluetoothA2dp proxy lifecycle.
 */

class A2dpProfileManager(
    private val context: Context,
    private val onConnected: (proxy: BluetoothA2dp, device: BluetoothDevice?) -> Unit,
    private val onDisconnected: () -> Unit
) {
    private var proxy: BluetoothA2dp? = null

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, bluetoothProfile: BluetoothProfile) {
            if (profile != BluetoothProfile.A2DP) return
            val a2dp = (bluetoothProfile as BluetoothA2dp).also { proxy = it }
            val device = runCatching { a2dp.connectedDevices.firstOrNull() }.getOrNull()
            onConnected(a2dp, device)
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.A2DP) return
            proxy = null
            onDisconnected()
        }
    }

    /** @return false if Bluetooth is not available on the device */
    fun connect(): Boolean {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return false
        return adapter.getProfileProxy(context, serviceListener, BluetoothProfile.A2DP)
    }

    fun disconnect() {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        proxy?.let { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
        proxy = null
    }
}