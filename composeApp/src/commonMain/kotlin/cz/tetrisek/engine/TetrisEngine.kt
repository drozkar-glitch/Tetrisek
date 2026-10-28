package cz.tetrisek.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

// --- DEFINICE KOSTEK A TVARŮ ---
enum class TetrominoType { I, J, L, O, S, T, Z }

enum class Difficulty(val label: String) {
    EASY("Lehká"),
    MEDIUM("Střední"),
    HARD("Těžká")
}

data class Pos(val x: Int, val y: Int)

val SHAPES = mapOf(
    TetrominoType.I to listOf(Pos(-1, 0), Pos(0, 0), Pos(1, 0), Pos(2, 0)),
    TetrominoType.J to listOf(Pos(-1, -1), Pos(-1, 0), Pos(0, 0), Pos(1, 0)),
    TetrominoType.L to listOf(Pos(1, -1), Pos(-1, 0), Pos(0, 0), Pos(1, 0)),
    TetrominoType.O to listOf(Pos(0, 0), Pos(1, 0), Pos(0, 1), Pos(1, 1)),
    TetrominoType.S to listOf(Pos(0, 0), Pos(1, 0), Pos(-1, 1), Pos(0, 1)),
    TetrominoType.T to listOf(Pos(0, -1), Pos(-1, 0), Pos(0, 0), Pos(1, 0)),
    TetrominoType.Z to listOf(Pos(-1, 0), Pos(0, 0), Pos(0, 1), Pos(1, 1))
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
            val (rx, ry) = when (rotation % 4) {
                0 -> pos.x to pos.y
                1 -> pos.y to -pos.x
                2 -> -pos.x to -pos.y
                3 -> -pos.y to pos.x
                else -> pos.x to pos.y
            }
            Pos(x + rx, y + ry)
        }
    }
}

data class GameState(
    val grid: Array<Array<TetrominoType?>> = Array(20) { Array(10) { null } },
    val currentPiece: Tetromino? = null,
    val nextPiece: TetrominoType? = null,
    val holdPiece: TetrominoType? = null,
    val score: Int = 0,
    val level: Int = 1,
    val startLevel: Int = 1,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val linesCleared: Int = 0,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val clearedRows: List<Int> = emptyList(),
    val clearTrigger: Int = 0
)

class TetrisEngine {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var canHold = true

    init {
        startNewGame()
    }

    fun startNewGame(
        difficulty: Difficulty = _gameState.value.difficulty,
        startLevel: Int = _gameState.value.startLevel
    ) {
        val firstPiece = generateRandomPieceType()
        val nextPieceType = generateRandomPieceType()
        val initialGrid = Array(20) { Array<TetrominoType?>(10) { null } }

        if (difficulty == Difficulty.HARD) {
            val types = TetrominoType.entries.toTypedArray()
            for (y in 16..19) {
                for (x in 0 until 10) {
                    if (Random.nextFloat() < 0.6f) {
                        initialGrid[y][x] = types[Random.nextInt(types.size)]
                    }
                }
                // Pojistka, aby řada nebyla prázdná a zároveň nikdy nebyla kompletně plná
                initialGrid[y][Random.nextInt(10)] = types[Random.nextInt(types.size)]
                initialGrid[y][Random.nextInt(10)] = null
            }
        }

        _gameState.update {
            GameState(
                grid = initialGrid,
                currentPiece = Tetromino(firstPiece),
                nextPiece = nextPieceType,
                level = startLevel,
                startLevel = startLevel,
                difficulty = difficulty
            )
        }
        canHold = true
    }

    fun cycleDifficulty() {
        val nextDifficulty = when (_gameState.value.difficulty) {
            Difficulty.EASY -> Difficulty.MEDIUM
            Difficulty.MEDIUM -> Difficulty.HARD
            Difficulty.HARD -> Difficulty.EASY
        }
        startNewGame(difficulty = nextDifficulty, startLevel = _gameState.value.startLevel)
    }

    fun changeStartLevel(delta: Int) {
        val newStartLevel = (_gameState.value.startLevel + delta).coerceIn(1, 15)
        if (newStartLevel != _gameState.value.startLevel) {
            startNewGame(difficulty = _gameState.value.difficulty, startLevel = newStartLevel)
        }
    }

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
        if (_gameState.value.difficulty == Difficulty.HARD) return null
        val current = _gameState.value.currentPiece ?: return null
        var ghost = current.copy()
        while (isValidPosition(ghost.copy(y = ghost.y + 1))) {
            ghost = ghost.copy(y = ghost.y + 1)
        }
        return ghost
    }

    private fun lockPiece() {
        val current = _gameState.value.currentPiece ?: return
        val newGrid = _gameState.value.grid.map { it.copyOf() }.toTypedArray()

        for (pos in current.getAbsolutePositions()) {
            if (pos.y < 0) {
                _gameState.update { it.copy(isGameOver = true) }
                return
            }
            if (pos.y in 0..19 && pos.x in 0..9) {
                newGrid[pos.y][pos.x] = current.type
            }
        }

        val fullRows = (0 until 20).filter { y -> newGrid[y].all { it != null } }

        val clearedLines = clearLines(newGrid)
        val newScore = _gameState.value.score + calculateScore(clearedLines, _gameState.value.level)
        val newLinesCleared = _gameState.value.linesCleared + clearedLines
        val newLevel = _gameState.value.startLevel + (newLinesCleared / 10)

        _gameState.update {
            it.copy(
                grid = newGrid,
                currentPiece = Tetromino(it.nextPiece ?: generateRandomPieceType()),
                nextPiece = generateRandomPieceType(),
                score = newScore,
                level = newLevel,
                linesCleared = newLinesCleared,
                clearedRows = fullRows,
                clearTrigger = if (fullRows.isNotEmpty()) it.clearTrigger + 1 else it.clearTrigger
            )
        }
        canHold = true
    }

    private fun clearLines(grid: Array<Array<TetrominoType?>>): Int {
        var linesCleared = 0
        var y = grid.lastIndex
        while (y >= 0) {
            if (grid[y].all { it != null }) {
                linesCleared++
                for (i in y downTo 1) {
                    grid[i] = grid[i - 1].copyOf()
                }
                grid[0] = Array(10) { null }
            } else {
                y--
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
        startNewGame(
            difficulty = _gameState.value.difficulty,
            startLevel = _gameState.value.startLevel
        )
    }
}
