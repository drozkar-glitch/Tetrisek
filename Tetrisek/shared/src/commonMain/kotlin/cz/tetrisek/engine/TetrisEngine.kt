package cz.tetrisek.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

// --- DEFINICE KOSTEK A TVARŮ (Toto v předchozím kódu chybělo) ---
enum class TetrominoType { I, J, L, O, S, T, Z }

data class Pos(val x: Int, val y: Int)

val SHAPES = mapOf(
    TetrominoType.I to listOf(Pos(-1, 0), Pos(0, 0), Pos(1, 0), Pos(2, 0)),
    TetrominoType.J to listOf(Pos(-1, -1), Pos(-1, 0), Pos(0, 0), Pos(1, 0)),
    TetrominoType.L to listOf(Pos(1, -1), Pos(-1, 0), Pos(0, 0), Pos(1, 0)),
    TetrominoType.O to listOf(Pos(0, 0), Pos(1, 0), Pos(0, 1), Pos(1, 1)),
    TetrominoType.S to listOf(Pos(0, 0), Pos(1, 0), Pos(-1, 1), Pos(0, 1)),
    TetrominoType.T to listOf(Pos(-1, 0), Pos(0, 0), Pos(1, 0), Pos(0, 1)),
    TetrominoType.Z to listOf(Pos(-1, 0), Pos(0, 0), Pos(0, -1), Pos(1, -1))
)

data class Tetromino(
    val type: TetrominoType,
    val x: Int = 4,
    val y: Int = 0,
    val rotation: Int = 0
) {
    fun getAbsolutePositions(): List<Pos> {
        val blocks = SHAPES[type] ?: emptyList()
        return blocks.map { pos ->
            // Rotace bloku kolem středu
            val (rx, ry) = when (rotation % 4) {
                0 -> pos.x to pos.y
                1 -> -pos.y to pos.x
                2 -> -pos.x to -pos.y
                3 -> pos.y to -pos.x
                else -> pos.x to pos.y
            }
            Pos(x + rx, y + ry)
        }
    }
}
// ---------------------------------------------------------------

// Datová třída reprezentující stav hry
data class GameState(
    val grid: Array<Array<TetrominoType?>> = Array(20) { Array(10) { null } },
    val currentPiece: Tetromino? = null,
    val nextPiece: TetrominoType? = null,
    val holdPiece: TetrominoType? = null,
    val score: Int = 0,
    val level: Int = 1,
    val linesCleared: Int = 0,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false
)

class TetrisEngine {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var canHold = true

    init {
        startNewGame()
    }

    fun startNewGame() {
        val firstPiece = generateRandomPieceType()
        val nextPieceType = generateRandomPieceType()

        _gameState.update {
            GameState(
                currentPiece = Tetromino(firstPiece),
                nextPiece = nextPieceType
            )
        }
        canHold = true
    }

    // --- FUNKCE PRO PAUZU ---
    fun togglePause() {
        if (!_gameState.value.isGameOver) {
            _gameState.update { it.copy(isPaused = !it.isPaused) }
        }
    }

    fun pause() {
        if (!_gameState.value.isGameOver && !_gameState.value.isPaused) {
            _gameState.update { it.copy(isPaused = true) }
        }
    }

    fun resume() {
        if (!_gameState.value.isGameOver && _gameState.value.isPaused) {
            _gameState.update { it.copy(isPaused = false) }
        }
    }
    // -------------------------

    fun moveLeft() {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return
        val current = _gameState.value.currentPiece ?: return
        val newPiece = current.copy(x = current.x - 1)
        if (isValidPosition(newPiece)) {
            _gameState.update { it.copy(currentPiece = newPiece) }
        }
    }

    fun moveRight() {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return
        val current = _gameState.value.currentPiece ?: return
        val newPiece = current.copy(x = current.x + 1)
        if (isValidPosition(newPiece)) {
            _gameState.update { it.copy(currentPiece = newPiece) }
        }
    }

    fun moveDown() {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return
        val current = _gameState.value.currentPiece ?: return
        val newPiece = current.copy(y = current.y + 1)

        if (isValidPosition(newPiece)) {
            _gameState.update { it.copy(currentPiece = newPiece) }
        } else {
            lockPiece()
        }
    }

    fun rotate() {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return
        val current = _gameState.value.currentPiece ?: return
        val newPiece = current.copy(rotation = (current.rotation + 1) % 4)
        if (isValidPosition(newPiece)) {
            _gameState.update { it.copy(currentPiece = newPiece) }
        } else {
            // "Wall kick" (odsunutí od zdi při rotaci)
            val kickedLeft = newPiece.copy(x = newPiece.x - 1)
            if (isValidPosition(kickedLeft)) {
                _gameState.update { it.copy(currentPiece = kickedLeft) }
                return
            }
            val kickedRight = newPiece.copy(x = newPiece.x + 1)
            if (isValidPosition(kickedRight)) {
                _gameState.update { it.copy(currentPiece = kickedRight) }
            }
        }
    }

    fun hardDrop() {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return
        var current = _gameState.value.currentPiece ?: return
        while (isValidPosition(current.copy(y = current.y + 1))) {
            current = current.copy(y = current.y + 1)
        }
        _gameState.update { it.copy(currentPiece = current) }
        lockPiece()
    }

    fun hold() {
        if (_gameState.value.isGameOver || !canHold || _gameState.value.isPaused) return
        val currentType = _gameState.value.currentPiece?.type ?: return
        val holdType = _gameState.value.holdPiece

        _gameState.update {
            it.copy(
                holdPiece = currentType,
                currentPiece = if (holdType != null) Tetromino(holdType) else Tetromino(it.nextPiece ?: generateRandomPieceType()),
                nextPiece = if (holdType != null) it.nextPiece else generateRandomPieceType()
            )
        }
        canHold = false
    }

    fun getGhostPiece(): Tetromino? {
        val current = _gameState.value.currentPiece ?: return null
        var ghost = current.copy()
        while (isValidPosition(ghost.copy(y = ghost.y + 1))) {
            ghost = ghost.copy(y = ghost.y + 1)
        }
        return ghost
    }

    private fun lockPiece() {
        val current = _gameState.value.currentPiece ?: return
        val newGrid = _gameState.value.grid.map { it.clone() }.toTypedArray()

        for (pos in current.getAbsolutePositions()) {
            if (pos.y < 0) {
                _gameState.update { it.copy(isGameOver = true) }
                return
            }
            if (pos.y in 0..19 && pos.x in 0..9) {
                newGrid[pos.y][pos.x] = current.type
            }
        }

        val clearedLines = clearLines(newGrid)
        val newScore = _gameState.value.score + calculateScore(clearedLines, _gameState.value.level)
        val newLinesCleared = _gameState.value.linesCleared + clearedLines
        val newLevel = (newLinesCleared / 10) + 1

        _gameState.update {
            it.copy(
                grid = newGrid,
                currentPiece = Tetromino(it.nextPiece ?: generateRandomPieceType()),
                nextPiece = generateRandomPieceType(),
                score = newScore,
                level = newLevel,
                linesCleared = newLinesCleared
            )
        }
        canHold = true
    }

    private fun clearLines(grid: Array<Array<TetrominoType?>>): Int {
        var linesCleared = 0
        for (y in grid.indices.reversed()) {
            if (grid[y].all { it != null }) {
                linesCleared++
                for (i in y downTo 1) {
                    grid[i] = grid[i - 1].clone()
                }
                grid[0] = Array(10) { null }
            }
        }
        return linesCleared
    }

    private fun calculateScore(lines: Int, level: Int): Int {
        val basePoints = when (lines) {
            1 -> 40
            2 -> 100
            3 -> 300
            4 -> 1200
            else -> 0
        }
        return basePoints * level
    }

    private fun isValidPosition(piece: Tetromino): Boolean {
        for (pos in piece.getAbsolutePositions()) {
            if (pos.x < 0 || pos.x >= 10 || pos.y >= 20) return false
            if (pos.y >= 0 && _gameState.value.grid[pos.y][pos.x] != null) return false
        }
        return true
    }

    private fun generateRandomPieceType(): TetrominoType {
        val types = TetrominoType.entries.toTypedArray()
        return types[Random.nextInt(types.size)]
    }

    fun restart() {
        _gameState.value = GameState()
        startNewGame()
    }
}