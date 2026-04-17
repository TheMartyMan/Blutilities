package com.marty.blutilities.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.marty.blutilities.R
import com.marty.blutilities.bluetooth.BluetoothPermissionHelper
import com.marty.blutilities.bluetooth.CodecListBuilder
import com.marty.blutilities.ui.BluetoothQualityActivity

/**
 * Quick Settings tile, which is only active (clickable)
 * if an A2DP device is connected – or if BT permission is not yet granted
 * (in which case the Activity that opens will request permission)
 */
class BluetoothQualityTileService : BaseTileService() {

    companion object {
        private const val TAG = "BtQualityTile"
    }

    private val bluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private var a2dpProxy: BluetoothA2dp? = null
    private var isA2dpConnected = false
    private val codecListBuilder by lazy { CodecListBuilder(this) }

    // BroadcastReceiver

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                    isA2dpConnected = state == BluetoothProfile.STATE_CONNECTED
                    Log.d(TAG, "A2DP state → connected=$isA2dpConnected")
                    refreshTile()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val adapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                    if (adapterState == BluetoothAdapter.STATE_OFF) {
                        isA2dpConnected = false
                        refreshTile()
                    }
                }
            }
        }
    }


    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.A2DP) return
            a2dpProxy = proxy as BluetoothA2dp
            isA2dpConnected = BluetoothPermissionHelper.hasPermission(this@BluetoothQualityTileService) &&
                    runCatching { a2dpProxy?.connectedDevices?.isNotEmpty() == true }.getOrDefault(false)
            Log.d(TAG, "A2DP proxy connected, isConnected=$isA2dpConnected")
            refreshTile()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.A2DP) return
            a2dpProxy = null
            isA2dpConnected = false
            refreshTile()
        }
    }

    // Lifecycle

    override fun onCreate() {
        super.onCreate()
        bluetoothAdapter?.getProfileProxy(this, profileListener, BluetoothProfile.A2DP)
        registerReceiver(bluetoothReceiver, IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        a2dpProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
    }

    // BaseTileService

    /**
     * The tile can be clicked without permission so that the Activity can request it.
     * If permission is already granted, it is only active if there is actually a connected device.
     */
    override fun isAvailable(): Boolean =
        !BluetoothPermissionHelper.hasPermission(this) || isA2dpConnected

    override fun isActive(): Boolean = false  // állapotmentes kapcsolóként működik

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onTileClicked() {
        val intent = Intent(this, BluetoothQualityActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onBeforeRefresh() {
        val tile = qsTile ?: return

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter

        tile.subtitle = when {
            adapter == null || !adapter.isEnabled -> getString(R.string.tile_bluetooth_off)
            !isA2dpConnected -> getString(R.string.tile_no_device)
            else -> getCurrentCodecName() ?: getString(R.string.tile_connected)
        }
    }

    private fun getCurrentCodecName(): String? {
        val proxy = a2dpProxy ?: return null

        val device = if (BluetoothPermissionHelper.hasPermission(this)) {
            runCatching { proxy.connectedDevices.firstOrNull() }.getOrNull()
        } else return null

        return try {
            val method = proxy::class.java.getMethod("getCodecStatus", BluetoothDevice::class.java)
            val codecStatus = method.invoke(proxy, device) as? BluetoothCodecStatus
            codecListBuilder.resolveActiveLabel(codecStatus)
        } catch (e: Exception) {
            Log.e(TAG, "Hiba a részletes felirat lekérésekor", e)
            null
        }
    }
}