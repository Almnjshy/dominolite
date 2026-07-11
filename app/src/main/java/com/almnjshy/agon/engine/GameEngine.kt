package com.almnjshy.agon.engine

/**
 * Pure functions only. No mutation, no Android/Compose types. Every function takes a
 * GameState and returns a new GameState. This makes the engine trivially unit-testable
 * and keeps rendering code completely dumb (it just reads the latest state).
 */
object GameEngine {

    /** Starts a brand-new round for `playerCount` players (2, 3 or 4). `kinds[i]` says
     *  whether seat i is the local human, a remote human (network play), or AI. */
    fun newRound(
        playerCount: Int,
        playerNames: List<String>,
        kinds: List<PlayerKind>,
        aiDifficulties: List<AIDifficulty> = List(playerCount) { AIDifficulty.MEDIUM }
    ): GameState {
        require(playerCount in 2..4) { "Agon supports 2-4 players" }
        require(playerNames.size == playerCount)
        require(kinds.size == playerCount)

        val handSize = SeatLayout.handSizeFor(playerCount)

        var deck = DominoTile.standardSet().shuffled()
        val hands = mutableListOf<List<DominoTile>>()
        repeat(playerCount) {
            hands.add(deck.take(handSize))
            deck = deck.drop(handSize)
        }

        val players = (0 until playerCount).map { i ->
            PlayerState(
                id = i,
                name = playerNames[i],
                kind = kinds[i],
                aiDifficulty = aiDifficulties.getOrElse(i) { AIDifficulty.MEDIUM },
                hand = hands[i]
            )
        }

        // Player holding the highest double leads; if nobody has one, highest-pip tile leads.
        val starter = players.indices.maxByOrNull { i ->
            val bestDouble = players[i].hand.filter { it.isDouble }.maxOfOrNull { it.pipSum } ?: -1
            bestDouble
        } ?: 0

        return GameState(
            playerCount = playerCount,
            players = players,
            boneyard = deck,
            currentPlayerIndex = starter
        )
    }

    fun isValidPlay(state: GameState, tile: DominoTile, side: ChainSide): Boolean {
        if (state.chain.isEmpty()) return true
        val end = if (side == ChainSide.LEFT) state.leftEnd else state.rightEnd
        return end != null && tile.hasValue(end)
    }

    fun playableSides(state: GameState, tile: DominoTile): List<ChainSide> {
        if (state.chain.isEmpty()) return listOf(ChainSide.RIGHT)
        return ChainSide.entries.filter { isValidPlay(state, tile, it) }
    }

    fun hasAnyValidMove(state: GameState, playerIndex: Int): Boolean =
        state.players[playerIndex].hand.any { playableSides(state, it).isNotEmpty() }

    /** Plays `tile` from the current player's hand onto `side` of the chain. */
    fun playTile(state: GameState, tile: DominoTile, side: ChainSide): GameState {
        val player = state.currentPlayer
        require(tile in player.hand) { "Tile not in current player's hand" }
        require(isValidPlay(state, tile, side)) { "Illegal move" }

        val isFirstTile = state.chain.isEmpty()
        val chainTile = ChainTile(tile, isSpinnerOrientation = tile.isDouble)

        val newChain = if (isFirstTile) {
            listOf(chainTile)
        } else if (side == ChainSide.LEFT) {
            listOf(chainTile) + state.chain
        } else {
            state.chain + chainTile
        }

        val newLeftEnd: Int
        val newRightEnd: Int
        if (isFirstTile) {
            newLeftEnd = tile.left
            newRightEnd = tile.right
        } else if (side == ChainSide.LEFT) {
            newLeftEnd = tile.otherEnd(state.leftEnd!!)
            newRightEnd = state.rightEnd!!
        } else {
            newLeftEnd = state.leftEnd!!
            newRightEnd = tile.otherEnd(state.rightEnd!!)
        }

        val newHand = player.hand.filterNot { it.id == tile.id }
        val newPlayers = state.players.toMutableList().also {
            it[state.currentPlayerIndex] = player.copy(hand = newHand)
        }

        var newState = state.copy(
            players = newPlayers,
            chain = newChain,
            leftEnd = newLeftEnd,
            rightEnd = newRightEnd,
            consecutivePasses = 0,
            lastAction = "${player.name} played ${tile.left}|${tile.right}"
        )

        if (newHand.isEmpty()) {
            newState = endRound(newState, winnerIndex = state.currentPlayerIndex, blocked = false)
        } else {
            newState = advanceTurn(newState)
            newState = checkForBlock(newState)
        }
        return newState
    }

    /** Draws one tile from the boneyard into the current player's hand. */
    fun drawTile(state: GameState): GameState {
        if (state.boneyard.isEmpty()) return state
        val drawn = state.boneyard.first()
        val remaining = state.boneyard.drop(1)
        val player = state.currentPlayer
        val newPlayers = state.players.toMutableList().also {
            it[state.currentPlayerIndex] = player.copy(hand = player.hand + drawn)
        }
        return state.copy(
            players = newPlayers,
            boneyard = remaining,
            lastAction = "${player.name} drew a tile"
        )
    }

    /** Current player has no valid move and an empty boneyard: pass the turn. */
    fun pass(state: GameState): GameState {
        val newState = state.copy(
            consecutivePasses = state.consecutivePasses + 1,
            lastAction = "${state.currentPlayer.name} passed"
        )
        return checkForBlock(advanceTurn(newState))
    }

    fun advanceTurn(state: GameState): GameState {
        if (state.roundOver) return state
        val next = (state.currentPlayerIndex + 1) % state.playerCount
        return state.copy(currentPlayerIndex = next)
    }

    private fun checkForBlock(state: GameState): GameState {
        if (state.roundOver) return state
        if (state.consecutivePasses >= state.playerCount) {
            val winner = state.players.indices.minByOrNull { i ->
                state.players[i].hand.sumOf { it.pipSum }
            }
            return endRound(state, winnerIndex = winner, blocked = true)
        }
        return state
    }

    private fun endRound(state: GameState, winnerIndex: Int?, blocked: Boolean): GameState {
        val pointsFromOthers = state.players
            .filterIndexed { i, _ -> i != winnerIndex }
            .sumOf { it.hand.sumOf { t -> t.pipSum } }

        val newPlayers = if (winnerIndex != null) {
            state.players.mapIndexed { i, p ->
                if (i == winnerIndex) p.copy(matchScore = p.matchScore + pointsFromOthers) else p
            }
        } else state.players

        return state.copy(
            players = newPlayers,
            roundOver = true,
            roundWinnerIndex = winnerIndex,
            blockedRound = blocked,
            lastAction = if (blocked) "Round blocked" else "${winnerIndex?.let { state.players[it].name }} emptied their hand"
        )
    }

    /** Is the whole match over (someone hit 100 points)? */
    fun isMatchOver(state: GameState, targetScore: Int = 100): Boolean =
        state.players.any { it.matchScore >= targetScore }
}
