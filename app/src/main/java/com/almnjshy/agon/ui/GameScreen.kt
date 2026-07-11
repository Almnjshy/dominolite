package com.almnjshy.agon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almnjshy.agon.board.BoardEngine
import com.almnjshy.agon.engine.*
import com.almnjshy.agon.rendering.TableSurface
import com.almnjshy.agon.rendering.TableTheme
import com.almnjshy.agon.ui.components.ChainView
import com.almnjshy.agon.ui.components.OpponentHandView
import com.almnjshy.agon.ui.components.PlayerHandView
import com.almnjshy.agon.ui.theme.AgonColors

/** Local single-device hotseat entry point: one local human + AI opponents. */
@Composable
fun GameScreen(
    playerCount: Int,
    difficulty: AIDifficulty,
    onExitToMenu: () -> Unit,
    vm: GameViewModel = viewModel()
) {
    LaunchedEffect(playerCount, difficulty) {
        vm.startNewRound(playerCount, difficulty)
    }

    GameBoard(controller = vm) { s ->
        RoundOverDialog(
            state = s,
            canRestart = true,
            onPlayAgain = { vm.startNewRound(playerCount, difficulty) },
            onExit = onExitToMenu
        )
    }
}

/** Network entry point: state is either computed here (host) or received from the host
 *  (client) — see [com.almnjshy.agon.network.NetworkGameViewModel]. Rendering is shared
 *  with hotseat play through [GameBoard]. */
@Composable
fun NetworkGameScreen(
    vm: com.almnjshy.agon.network.NetworkGameViewModel,
    onExitToMenu: () -> Unit
) {
    GameBoard(controller = vm) { s ->
        RoundOverDialog(
            state = s,
            canRestart = vm.isHost,
            onPlayAgain = { vm.hostStartNewRound() },
            onExit = onExitToMenu
        )
    }
}

/** Shared table rendering for both hotseat and network play. Always draws the local
 *  seat ([GameController.localSeatIndex]) at the bottom and rotates everyone else
 *  around them, so every device sees itself the same way regardless of absolute
 *  player index. */
@Composable
private fun GameBoard(
    controller: GameController,
    roundOverContent: @Composable (GameState) -> Unit
) {
    val state by controller.state.collectAsState()
    val selectedTile by controller.selectedTile.collectAsState()
    val s = state ?: return
    val localSeatIndex = controller.localSeatIndex

    val bounds = remember { BoardEngine.defaultBounds() }
    val unitPx = 92f

    Box(Modifier.fillMaxSize()) {
        TableSurface(theme = TableTheme.CLASSIC_FELT)

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val centerX = with(density) { (maxWidth / 2).toPx() }
            val centerY = with(density) { (maxHeight / 2).toPx() }

            ChainView(
                chain = s.chain,
                bounds = bounds,
                originX = centerX,
                originY = centerY,
                unitPx = unitPx,
                modifier = Modifier.fillMaxSize()
            )

            for (player in s.players) {
                if (player.id == localSeatIndex) continue
                val displaySeat = SeatLayout.displaySeatFor(player.id, localSeatIndex, s.playerCount)
                val alignment = when (displaySeat) {
                    SeatPosition.TOP -> Alignment.TopCenter
                    SeatPosition.LEFT -> Alignment.CenterStart
                    SeatPosition.RIGHT -> Alignment.CenterEnd
                    SeatPosition.TOP_LEFT -> Alignment.TopStart
                    SeatPosition.TOP_RIGHT -> Alignment.TopEnd
                    else -> Alignment.TopCenter
                }
                Column(
                    modifier = Modifier.align(alignment).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SeatLabel(player, isActive = s.currentPlayerIndex == player.id)
                    Spacer(Modifier.height(6.dp))
                    OpponentHandView(tileCount = player.hand.size, seat = displaySeat)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .background(AgonColors.FeltGreenDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("البئر: ${s.boneyard.size}", color = AgonColors.TileIvory)
            s.lastAction?.let { Text(it, color = AgonColors.GoldBright) }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val me = s.players[localSeatIndex]
            SeatLabel(me, isActive = s.currentPlayerIndex == me.id)
            Spacer(Modifier.height(6.dp))
            PlayerHandView(
                hand = me.hand,
                selectedTileId = selectedTile?.id,
                canPlaySide = { tile ->
                    if (!s.roundOver && s.currentPlayerIndex == me.id) GameEngine.playableSides(s, tile) else emptyList()
                },
                onSelect = { controller.selectTile(it) },
                onDragPlay = { tile ->
                    val sides = GameEngine.playableSides(s, tile)
                    if (sides.isNotEmpty()) {
                        controller.selectTile(tile)
                        controller.playSelectedTile(sides.first())
                    }
                }
            )
            if (selectedTile != null && GameEngine.playableSides(s, selectedTile!!).size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = { controller.playSelectedTile(ChainSide.LEFT) }) { Text("يسار") }
                    Button(onClick = { controller.playSelectedTile(ChainSide.RIGHT) }) { Text("يمين") }
                }
            }
        }

        if (s.roundOver) {
            roundOverContent(s)
        }
    }
}

@Composable
private fun SeatLabel(player: PlayerState, isActive: Boolean) {
    val color = if (isActive) AgonColors.GoldBright else AgonColors.TileIvory.copy(alpha = 0.8f)
    Text(
        player.name + if (isActive) " ●" else "",
        color = color,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun RoundOverDialog(
    state: GameState,
    canRestart: Boolean,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            if (canRestart) {
                TextButton(onClick = onPlayAgain) { Text("جولة جديدة") }
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) { Text("القائمة الرئيسية") }
        },
        title = { Text(if (state.blockedRound) "انسداد اللعبة" else "انتهت الجولة") },
        text = {
            val winnerName = state.roundWinnerIndex?.let { state.players[it].name } ?: "لا أحد"
            Text(
                "الفائز: $winnerName" + if (!canRestart) "\nبانتظار المضيف لبدء جولة جديدة..." else ""
            )
        }
    )
}
