package cz.tetrisek.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class TetrominoType { I, J, L, O, S, T, Z }

data class Position(val x: Int, val y: Int)

// Tvar definujeme jako seznam bloků relativně ke středu rotace (0,0)
data class Tetromino(val type: TetrominoType, val blocks: List<Position>, val position: Position) {
    fun getAbsolutePositions(): List<Position> = blocks.map { Position(it.x + position.x, it.y + position.y) }
}

val SHAPES = mapOf(
    TetrominoType.I to listOf(Position(-1, 0), Position(0, 0), Position(1, 0), Position(2, 0)),
    TetrominoType.J to listOf(Position(-1, -1), Position(-1, 0), Position(0, 0), Position(1, 0)),
    TetrominoType.L to listOf(Position(1, -1), Position(-1, 0), Position(0, 0), Position(1, 0)),
    TetrominoType.O to listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1)),
    TetrominoType.S to listOf(Position(0, 0), Position(1, 0), Position(-1, 1), Position(0, 1)),
    TetrominoType.T to listOf(Position(-1, 0), Position(0, 0), Position(1, 0), Position(0, 1)),
    TetrominoType.Z to listOf(Position(-1, 0), Position(0, 0), Position(0, 1), Position(1, 1))
)

data class GameState(
    val grid: List<List<TetrominoType?>> = List(20) { List(10) { null } },
    val currentPiece: Tetromino? = null,
    val nextPiece: TetrominoType? = null,
    val holdPiece: TetrominoType? = null,
    val canHold: Boolean = true,
    val score: Int = 0,
    val level: Int = 1,
    val linesCleared: Int = 0,
    val isGameOver: Boolean = false
)

class TetrisEngine {
    private val width = 10
    private val height = 20

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        spawnPiece()
    }

    private fun getRandomType(): TetrominoType = TetrominoType.entries[Random.nextInt(TetrominoType.entries.size)]

    private fun spawnPiece() {
        val state = _gameState.value
        if (state.isGameOver) return

        val next = state.nextPiece ?: getRandomType()
        val newNext = getRandomType()

        val newPiece = Tetromino(
            type = next,
            blocks = SHAPES[next]!!,
            position = Position(width / 2 - 1, 1)
        )

        if (checkCollision(newPiece, state.grid)) {
            _gameState.update { it.copy(isGameOver = true, currentPiece = newPiece) }
        } else {
            _gameState.update {
                it.copy(
                    currentPiece = newPiece,
                    nextPiece = newNext,
                    canHold = true
                )
            }
        }
    }

    fun moveLeft() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        val moved = piece.copy(position = Position(piece.position.x - 1, piece.position.y))
        if (!checkCollision(moved, state.grid)) {
            _gameState.update { it.copy(currentPiece = moved) }
        }
    }

    fun moveRight() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        val moved = piece.copy(position = Position(piece.position.x + 1, piece.position.y))
        if (!checkCollision(moved, state.grid)) {
            _gameState.update { it.copy(currentPiece = moved) }
        }
    }

    fun moveDown(): Boolean {
        val state = _gameState.value
        val piece = state.currentPiece ?: return false
        val moved = piece.copy(position = Position(piece.position.x, piece.position.y + 1))

        return if (!checkCollision(moved, state.grid)) {
            _gameState.update { it.copy(currentPiece = moved) }
            true
        } else {
            lockPiece()
            false
        }
    }

    fun hardDrop() {
        var dropped = true
        var dropDistance = 0
        while (dropped) {
            val state = _gameState.value
            val piece = state.currentPiece ?: break
            val moved = piece.copy(position = Position(piece.position.x, piece.position.y + 1))
            if (!checkCollision(moved, state.grid)) {
                _gameState.update { it.copy(currentPiece = moved) }
                dropDistance++
            } else {
                dropped = false
            }
        }
        // Body navíc za rychlý pád
        _gameState.update { it.copy(score = it.score + (dropDistance * 2)) }
        lockPiece()
    }

    fun rotate() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (piece.type == TetrominoType.O) return

        // Rotace o 90 stupňů po směru hodinových ručiček
        val rotatedBlocks = piece.blocks.map { Position(-it.y, it.x) }
        val rotatedPiece = piece.copy(blocks = rotatedBlocks)

        // Jednoduchý "wall kick" – pokud se po rotaci překrývá, zkusíme ji posunout
        var finalPiece = rotatedPiece
        if (checkCollision(finalPiece, state.grid)) {
            finalPiece = rotatedPiece.copy(position = Position(piece.position.x + 1, piece.position.y))
            if (checkCollision(finalPiece, state.grid)) {
                finalPiece = rotatedPiece.copy(position = Position(piece.position.x - 1, piece.position.y))
                if (checkCollision(finalPiece, state.grid)) {
                    return // Nelze rotovat, chybí místo
                }
            }
        }
        _gameState.update { it.copy(currentPiece = finalPiece) }
    }

    fun hold() {
        val state = _gameState.value
        if (!state.canHold || state.currentPiece == null) return

        val currentType = state.currentPiece.type
        val heldType = state.holdPiece

        if (heldType == null) {
            _gameState.update {
                it.copy(
                    holdPiece = currentType,
                    currentPiece = null,
                    canHold = false
                )
            }
            spawnPiece()
        } else {
            val newPiece = Tetromino(
                type = heldType,
                blocks = SHAPES[heldType]!!,
                position = Position(width / 2 - 1, 1)
            )
            _gameState.update {
                it.copy(
                    holdPiece = currentType,
                    currentPiece = newPiece,
                    canHold = false
                )
            }
        }
    }

    fun getGhostPiece(): Tetromino? {
        val state = _gameState.value
        var piece = state.currentPiece ?: return null
        while (true) {
            val moved = piece.copy(position = Position(piece.position.x, piece.position.y + 1))
            if (checkCollision(moved, state.grid)) {
                break
            }
            piece = moved
        }
        return piece
    }

    private fun lockPiece() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return

        val newGrid = state.grid.map { it.toMutableList() }.toMutableList()

        for (pos in piece.getAbsolutePositions()) {
            if (pos.y in 0 until height && pos.x in 0 until width) {
                newGrid[pos.y][pos.x] = piece.type
            }
        }

        _gameState.update { it.copy(grid = newGrid, currentPiece = null) }
        clearLines()
        spawnPiece()
    }

    private fun clearLines() {
        val state = _gameState.value
        val newGrid = state.grid.filter { row -> row.any { it == null } }.map { it.toMutableList() }.toMutableList()
        val linesCleared = height - newGrid.size

        if (linesCleared > 0) {
            while (newGrid.size < height) {
                newGrid.add(0, MutableList(width) { null })
            }

            val newTotalLines = state.linesCleared + linesCleared
            val newLevel = (newTotalLines / 10) + 1

            val basePoints = when (linesCleared) {
                1 -> 40
                2 -> 100
                3 -> 300
                4 -> 1200
                else -> 0
            }
            val pointsEarned = basePoints * state.level

            _gameState.update {
                it.copy(
                    grid = newGrid,
                    score = it.score + pointsEarned,
                    linesCleared = newTotalLines,
                    level = newLevel
                )
            }
        }
    }

    private fun checkCollision(piece: Tetromino, grid: List<List<TetrominoType?>>): Boolean {
        for (pos in piece.getAbsolutePositions()) {
            if (pos.x < 0 || pos.x >= width || pos.y >= height) {
                return true
            }
            if (pos.y >= 0 && grid[pos.y][pos.x] != null) {
                return true
            }
        }
        return false
    }

    fun restart() {
        _gameState.value = GameState()
        spawnPiece()
    }
}