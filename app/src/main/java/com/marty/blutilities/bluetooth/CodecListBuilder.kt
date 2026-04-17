package com.marty.blutilities.bluetooth

import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothCodecStatus
import android.content.Context
import com.marty.blutilities.R
import com.marty.blutilities.model.BluetoothCodecInfo

/**
 * Builds the codec selection list based on [BluetoothCodecStatus].
 * Deals with UI only.
 *
 * The order reflects the AOSP Developer Settings screen:
 * 1. "System Selection" (always first)
 * 2. [Codec Header]
 * 3. Available codecs in descending order of quality
 * 4. LDAC sub-rows, if LDAC is currently active
 */
class CodecListBuilder(private val context: Context) {

    fun build(codecStatus: BluetoothCodecStatus?): List<BluetoothCodecInfo> {
        val currentConfig  = codecStatus?.codecConfig
        val selectableList = codecStatus?.codecsSelectableCapabilities.orEmpty()
        val systemSelected = currentConfig?.codecPriority == BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT

        return buildList {
            // System
            add(BluetoothCodecInfo(
                codecName     = context.getString(R.string.codec_system_selection),
                qualityLabel  = context.getString(R.string.codec_system_selection_sub),
                codecType     = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID,
                codecPriority = BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                isSelected    = systemSelected
            ))

            if (selectableList.isEmpty()) {
                // Fallback to static list if codec unavailable
                addAll(staticFallback(currentConfig))
                return@buildList
            }

            // Codecs supported by device
            add(BluetoothCodecInfo(
                codecName = context.getString(R.string.header_codec),
                isHeader  = true
            ))

            selectableList.sortedByDescending { sortOrder(it.codecType) }.forEach { cap ->
                val isActive = currentConfig?.codecType == cap.codecType && !systemSelected
                add(BluetoothCodecInfo(
                    codecName     = typeName(cap.codecType),
                    codecType     = cap.codecType,
                    codecPriority = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST,
                    isSelected    = isActive
                ))
                // LDAC sublines only if this is the active codec
                if (cap.codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC && isActive
                ) {
                    add(BluetoothCodecInfo(
                        codecName = context.getString(R.string.header_ldac_quality),
                        isHeader  = true
                    ))
                    addAll(ldacQualityItems(currentConfig))
                }
            }
        }
    }

    private fun ldacQualityItems(currentConfig: BluetoothCodecConfig?): List<BluetoothCodecInfo> {
        data class LdacPreset(val labelRes: Int, val sub: String, val qualityValue: Int)

        val presets = listOf(
            LdacPreset(R.string.ldac_quality_best,       "990/909 kbps",                                     1000),
            LdacPreset(R.string.ldac_quality_balanced,   "660/606 kbps",                                     1001),
            LdacPreset(R.string.ldac_quality_connection, "330/303 kbps",                                         1002),
            LdacPreset(R.string.ldac_quality_adaptive,   context.getString(R.string.ldac_quality_adaptive_sub), 1003)
        )
        val currentSpecific = currentConfig?.codecSpecific1 ?: -1L

        return presets.map { p ->
            BluetoothCodecInfo(
                codecName     = context.getString(p.labelRes),
                qualityLabel  = p.sub,
                codecType     = BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC,
                codecPriority = p.qualityValue,
                isSelected    = currentSpecific == p.qualityValue.toLong()
            )
        }
    }

    private fun staticFallback(current: BluetoothCodecConfig?): List<BluetoothCodecInfo> =
        listOf(
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
        ).map { type ->
            BluetoothCodecInfo(
                codecName     = typeName(type),
                codecType     = type,
                codecPriority = BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST,
                isSelected    = current?.codecType == type
            )
        }

    fun typeName(type: Int): String = when (type) {
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC      -> "SBC"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC      -> "AAC"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX     -> "aptX"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD  -> "aptX HD"
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC     -> "LDAC"
        5                                               -> "aptX Adaptive"
        6                                               -> "aptX TWS"
        7                                               -> "LC3"
        8                                               -> "Opus"
        else                                            -> "Codec #$type"
    }

    private fun sortOrder(type: Int): Int = when (type) {
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC      -> 0
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC      -> 1
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX     -> 2
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD  -> 3
        BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC     -> 4
        5                                               -> 5   // aptX Adaptive
        7                                               -> 6   // LC3
        8                                               -> 7   // Opus
        else                                            -> -1
    }


    fun resolveActiveLabel(codecStatus: BluetoothCodecStatus?): String? {
        val config = codecStatus?.codecConfig ?: return null
        val type = config.codecType
        val baseName = typeName(type)

        // Only add extra info if it's LDAC
        if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC) {
            val quality = when (config.codecSpecific1.toInt()) {
                1000 -> "990 kbps"
                1001 -> "660 kbps"
                1002 -> "330 kbps"
                1003 -> context.getString(R.string.ldac_quality_adaptive_sub)
                else -> null
            }
            return if (quality != null) "$baseName - $quality" else baseName
        }

        return baseName
    }
}