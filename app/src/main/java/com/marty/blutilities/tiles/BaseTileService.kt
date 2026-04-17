package com.marty.blutilities.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

/**
 * Abstract base class for all Quick Tiles in this app.
 * Subclasses need to implement:
 * - [tileLabel] : the label shown under the tile (optional override)
 * - [isAvailable] : whether the tile should be in STATE_UNAVAILABLE
 * - [isActive] : whether the tile should be in STATE_ACTIVE vs STATE_INACTIVE
 * - [onTileClicked] : what happens when the user taps the tile
 * - [onStartListening]: optional hook when the tile becomes visible
 * - [onStopListening] : optional hook when the tile leaves visibility
 */
abstract class BaseTileService : TileService() {

    companion object {
        private const val TAG = "BaseTileService"
    }

    // Abstract API

    /** Return false to mark the tile as STATE_UNAVAILABLE (greyed out). */
    abstract fun isAvailable(): Boolean

    /** Return true for STATE_ACTIVE (highlighted), false for STATE_INACTIVE. */
    abstract fun isActive(): Boolean

    /** Called when the user taps the tile. */
    abstract fun onTileClicked()

    // Optional hooks for subclasses

    /** Called right before the tile state is refreshed while listening. */
    open fun onBeforeRefresh() {}

    // TileService lifecycle

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "${javaClass.simpleName}: onStartListening")
        refreshTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "${javaClass.simpleName}: onStopListening")
    }

    override fun onTileAdded() {
        super.onTileAdded()
        Log.d(TAG, "${javaClass.simpleName}: onTileAdded")
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (!isAvailable()) return
        Log.d(TAG, "${javaClass.simpleName}: onClick")
        onTileClicked()
    }

    // Helpers

    /**
     * Reads the current availability/active state and applies it to the tile.
     * Call whenever the underlying state changes.
     */
    fun refreshTile() {
        onBeforeRefresh()
        val tile = qsTile ?: return
        tile.state = when {
            !isAvailable() -> Tile.STATE_UNAVAILABLE
            isActive()     -> Tile.STATE_ACTIVE
            else           -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }
}