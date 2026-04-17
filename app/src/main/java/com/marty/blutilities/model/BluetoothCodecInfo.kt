package com.marty.blutilities.model

/**
 * Represents a single Bluetooth audio codec option shown in the picker list.
 *
 * @param codecName Human-readable codec name (e.g. "LDAC", "AAC")
 * @param qualityLabel Sub-quality label if applicable (e.g. "990 kbps · Best quality"),
 * null for codecs without sub-quality (SBC, AAC, aptX...)
 * @param codecType BluetoothCodecConfig.SOURCE_CODEC_TYPE_* constant
 * @param codecPriority BluetoothCodecConfig.CODEC_PRIORITY_* or a quality-specific value
 * @param isSelected Whether this entry is the currently active selection
 * @param isHeader True for section-header rows (no action on click)
 */
data class BluetoothCodecInfo(
    val codecName: String,
    val qualityLabel: String? = null,
    val codecType: Int = -1,
    val codecPriority: Int = 0,
    val isSelected: Boolean = false,
    val isHeader: Boolean = false
)