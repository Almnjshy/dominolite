package com.almnjshy.agon.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Whatever is driving the on-screen game — a local hotseat [GameViewModel] or a
 * network-synced view model — implements this so [com.almnjshy.agon.ui.GameScreen] can
 * stay completely agnostic of where the state actually comes from.
 */
interface GameController {
    val state: StateFlow<GameState?>
    val selectedTile: StateFlow<DominoTile?>

    /** Which seat *this device* is playing as. Always 0 in local hotseat play; assigned
     *  during the lobby handshake in network play. Used to rotate the table so the local
     *  player always renders at the bottom. */
    val localSeatIndex: Int

    fun selectTile(tile: DominoTile)
    fun playSelectedTile(side: ChainSide)
}
