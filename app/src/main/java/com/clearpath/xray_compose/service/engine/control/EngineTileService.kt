package com.clearpath.xray_compose.service.engine.control

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.clearpath.xray_compose.enums.EngineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EngineTileService : TileService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repository by lazy { EngineRepository.getInstance(this) }

    override fun onStartListening() {
        super.onStartListening()
        repository.engineStateFlow
            .onEach { state ->
                updateTile(state)
            }
            .launchIn(serviceScope)
    }

    override fun onClick() {
        super.onClick()
        val currentState = repository.engineStateFlow.value
        serviceScope.launch {
            if (currentState == EngineState.STARTED) {
                repository.stopEngine()
            } else if (currentState == EngineState.STOPPED) {
                repository.startActiveProfileEngine()
            }
        }
    }

    private fun updateTile(state: EngineState) {
        val tile = qsTile ?: return
        when (state) {
            EngineState.STARTED -> {
                tile.state = Tile.STATE_ACTIVE
            }

            EngineState.STOPPED -> {
                tile.state = Tile.STATE_INACTIVE
            }

            else -> {
                tile.state = Tile.STATE_INACTIVE
            }
        }
        tile.updateTile()
    }
}
