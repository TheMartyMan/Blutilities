package com.marty.blutilities.bluetooth

import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender

object CdmHelper {

    /** Returns the MAC of all CDM-associated BT devices. */
    fun associatedAddresses(context: Context): Set<String> {
        val cdm = context.getSystemService(CompanionDeviceManager::class.java)
        return cdm.myAssociations.map { it.deviceMacAddress?.toString() ?: "" }.toSet()
    }

    /** True if the specified device is already associated. */
    fun isAssociated(context: Context, macAddress: String): Boolean =
        associatedAddresses(context).any {
            it.replace(":", "").equals(macAddress.replace(":", ""), ignoreCase = true)
        }

    /**
     * Start CDM association.
     */
    fun requestAssociation(
        context: Context,
        macAddress: String,
        onIntentSender: (IntentSender) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cdm = context.getSystemService(CompanionDeviceManager::class.java)
        val filter = BluetoothDeviceFilter.Builder()
            .setAddress(macAddress)
            .build()
        val request = AssociationRequest.Builder()
            .addDeviceFilter(filter)
            .setSingleDevice(true)
            .build()

        cdm.associate(request, object : CompanionDeviceManager.Callback() {
            @Deprecated("Deprecated in Java")
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                onIntentSender(chooserLauncher)
            }
            override fun onFailure(error: CharSequence?) {
                onFailure(error?.toString() ?: "CDM association failed")
            }
        }, null)
    }
}