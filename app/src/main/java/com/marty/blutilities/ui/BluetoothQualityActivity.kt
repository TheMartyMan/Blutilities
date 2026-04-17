package com.marty.blutilities.ui

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.marty.blutilities.R
import com.marty.blutilities.bluetooth.A2dpProfileManager
import com.marty.blutilities.bluetooth.BluetoothPermissionHelper
import com.marty.blutilities.bluetooth.CdmHelper
import com.marty.blutilities.bluetooth.CodecListBuilder
import com.marty.blutilities.databinding.ActivityBluetoothQualityBinding
import com.marty.blutilities.model.BluetoothCodecInfo
import com.marty.blutilities.ui.adapter.CodecListAdapter

/**
 * Dialog-style Activity that mirrors the
 * "Developer Options → Bluetooth audio codec" screen.
 *
 * Handles UI only.
 * - BT proxy lifecycle → [A2dpProfileManager]
 * - Codec list builder → [CodecListBuilder]
 * - Permission management → [BluetoothPermissionHelper]
 */
class BluetoothQualityActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BtQualityActivity"
        private const val REQUEST_BT_PERMISSION   = 1001
        private const val REQUEST_CDM_ASSOCIATION = 1002

        private const val SCRIM_ALPHA_TARGET  = 153
        private const val ANIM_ENTER_MS       = 300L
        private const val ANIM_EXIT_MS        = 200L
    }

    private lateinit var binding: ActivityBluetoothQualityBinding
    private lateinit var codecAdapter: CodecListAdapter
    private lateinit var codecBuilder: CodecListBuilder
    private lateinit var a2dpManager: A2dpProfileManager

    private var a2dpProxy: BluetoothA2dp?    = null
    private var connectedDevice: BluetoothDevice? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)

        binding = ActivityBluetoothQualityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

        codecBuilder = CodecListBuilder(this)
        a2dpManager  = A2dpProfileManager(
            context      = this,
            onConnected  = { proxy, device ->
                a2dpProxy       = proxy
                connectedDevice = device
                populateCodecList()
            },
            onDisconnected = { a2dpProxy = null }
        )

        setupRecyclerView()
        setupCloseButton()
        setupEnterAnimation()

        if (BluetoothPermissionHelper.hasPermission(this)) connectA2dp()
        else requestBtPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        a2dpManager.disconnect()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BT_PERMISSION &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            connectA2dp()
        } else {
            showToast(R.string.bt_permission_denied)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CDM_ASSOCIATION && resultCode == RESULT_OK) {
            populateCodecList()
        }
    }

    // Setup

    private fun setupRecyclerView() {
        codecAdapter = CodecListAdapter(::applyCodecSelection)
        binding.recyclerCodecs.apply {
            layoutManager = LinearLayoutManager(this@BluetoothQualityActivity)
            adapter = codecAdapter
        }
    }

    private fun setupCloseButton() {
        val dismiss = { animateOut(::finish) }
        binding.btnClose.setOnClickListener { dismiss() }
        binding.root.setOnClickListener { dismiss() }
        binding.cardContent.setOnClickListener { /* fogyasztjuk az eseményt – ne záródjon be */ }
    }

    private fun connectA2dp() {
        if (!a2dpManager.connect()) {
            showToast(R.string.bt_not_supported)
            finish()
        }
    }

    private fun requestBtPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(BluetoothPermissionHelper.permission),
            REQUEST_BT_PERMISSION
        )
    }

    // Animations

    private fun setupEnterAnimation() {
        binding.root.post {
            binding.root.background.alpha = 0
            ValueAnimator.ofInt(0, SCRIM_ALPHA_TARGET).run {
                duration = ANIM_ENTER_MS
                addUpdateListener { binding.root.background.alpha = it.animatedValue as Int }
                start()
            }

            binding.cardContent.apply {
                alpha        = 0f
                translationY = 80f
                scaleX       = 0.9f
                scaleY       = 0.9f
                animate()
                    .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(ANIM_ENTER_MS)
                    .setInterpolator(DecelerateInterpolator(2f))
                    .start()
            }
        }
    }

    private fun animateOut(onEnd: () -> Unit) {
        binding.cardContent.animate()
            .alpha(0f).translationY(80f).scaleX(0.9f).scaleY(0.9f)
            .setDuration(ANIM_EXIT_MS)
            .setInterpolator(AccelerateInterpolator(1.5f))
            .start()

        ValueAnimator.ofInt(SCRIM_ALPHA_TARGET, 0).apply {
            duration = ANIM_EXIT_MS
            addUpdateListener { binding.root.background.alpha = it.animatedValue as Int }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                    overridePendingTransition(0, 0)
                }
            })
            start()
        }
    }

    // Codec list

    private fun populateCodecList() {
        val proxy  = a2dpProxy      ?: return showNoDeviceMessage()
        val device = connectedDevice ?: return showNoDeviceMessage()

        val mac = safeDeviceAddress(device) ?: return
        if (!CdmHelper.isAssociated(this, mac)) {
            requestCdmAssociation(mac)
            return
        }

        val codecStatus = readCodecStatus(proxy, device)
        codecAdapter.submitList(codecBuilder.build(codecStatus))
        binding.tvTitle.text = getString(R.string.title_bt_quality, safeDeviceName(device))
    }

    private fun showNoDeviceMessage() {
        codecAdapter.submitList(listOf(
            BluetoothCodecInfo(codecName = getString(R.string.no_device_connected), isHeader = true)
        ))
    }

    private fun readCodecStatus(proxy: BluetoothA2dp, device: BluetoothDevice): BluetoothCodecStatus? {
        if (!BluetoothPermissionHelper.hasPermission(this)) return null
        return runCatching {
            proxy::class.java
                .getMethod("getCodecStatus", BluetoothDevice::class.java)
                .invoke(proxy, device) as? BluetoothCodecStatus
        }.onFailure { Log.e(TAG, "getCodecStatus failed", it) }.getOrNull()
    }

    private fun requestCdmAssociation(mac: String) {
        CdmHelper.requestAssociation(
            context       = this,
            macAddress    = mac,
            onIntentSender = { startIntentSenderForResult(it, REQUEST_CDM_ASSOCIATION, null, 0, 0, 0) },
            onFailure     = { error ->
                Log.e(TAG, "CDM association failed: $error")
                codecAdapter.submitList(codecBuilder.build(null))
            }
        )
    }

    private fun safeDeviceAddress(device: BluetoothDevice): String? = runCatching {
        device.address.takeIf { BluetoothPermissionHelper.hasPermission(this) }
    }.getOrNull()

    private fun safeDeviceName(device: BluetoothDevice): String = runCatching {
        if (BluetoothPermissionHelper.hasPermission(this)) device.name ?: device.address
        else device.address
    }.getOrDefault("?")

    // Apply codec

    private fun applyCodecSelection(info: BluetoothCodecInfo) {
        if (info.isHeader) return
        val proxy  = a2dpProxy      ?: return
        val device = connectedDevice ?: return

        if (!BluetoothPermissionHelper.hasPermission(this)) {
            showToast(R.string.bt_permission_denied)
            return
        }

        runCatching {
            proxy::class.java
                .getMethod("setCodecConfigPreference", BluetoothDevice::class.java, BluetoothCodecConfig::class.java)
                .invoke(proxy, device, buildCodecConfig(info))
            showToast(getString(R.string.codec_applied, info.codecName.trim()))
        }.onFailure {
            Log.e(TAG, "setCodecConfigPreference failed", it)
            showToast(R.string.bt_permission_denied)
        }

        finish()
    }

    private fun buildCodecConfig(info: BluetoothCodecInfo): BluetoothCodecConfig =
        BluetoothCodecConfig.Builder().apply {
            when (info.codecType) {
                BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID -> {
                    setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC)
                    setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT)
                }

                BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
                    if info.codecPriority in 1000..1003 -> {
                    setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC)
                    setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                    setCodecSpecific1(info.codecPriority.toLong())
                }

                else -> {
                    setCodecType(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC)
                    setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                }
            }
        }.build()

    // Helpers

    private fun showToast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}