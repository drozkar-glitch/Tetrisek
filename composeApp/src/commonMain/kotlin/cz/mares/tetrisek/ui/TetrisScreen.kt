package cz.mares.tetrisek.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.mares.tetrisek.engine.Difficulty
import cz.mares.tetrisek.engine.SHAPES
import cz.mares.tetrisek.engine.TetrisEngine
import cz.mares.tetrisek.engine.TetrominoType
import kotlinx.coroutines.delay

val colorCyan = Color(0xFF00FFFF)
val colorBlue = Color(0xFF0088FF)
val colorOrange = Color(0xFFFFA500)
val colorYellow = Color(0xFFFFFF00)
val colorGreen = Color(0xFF00FF00)
val colorPurple = Color(0xFFBB00FF)
val colorRed = Color(0xFFFF0033)

fun getColorForType(type: TetrominoType?): Color {
    return when (type) {
        TetrominoType.I -> colorCyan
        TetrominoType.J -> colorBlue
        TetrominoType.L -> colorOrange
        TetrominoType.O -> colorYellow
        TetrominoType.S -> colorGreen
        TetrominoType.T -> colorPurple
        TetrominoType.Z -> colorRed
        null -> Color.DarkGray
    }
}

@Composable
fun TetrisScreen(engine: TetrisEngine) {
    val gameState by engine.gameState.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val haptics = rememberHaptics()
    val scoreStorage = rememberScoreStorage()

    var highScore by remember(gameState.difficulty) {
        mutableStateOf(scoreStorage.getHighScore(gameState.difficulty))
    }

    LaunchedEffect(gameState.score, gameState.difficulty) {
        val storedHigh = scoreStorage.getHighScore(gameState.difficulty)
        if (gameState.score > storedHigh) {
            scoreStorage.saveHighScore(gameState.difficulty, gameState.score)
            highScore = gameState.score
        } else {
            highScore = storedHigh
        }
    }

    var flashingRows by remember { mutableStateOf<List<Int>>(emptyList()) }

    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0A1A),
                Color(0xFF140024)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8E8EE),
                Color(0xFFD0D0DC)
            )
        )
    }

    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A24)
    val surfaceColor = if (isDarkTheme) Color(0xFF1A1A24).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f)
    val borderColor = if (isDarkTheme) Color(0xFF4A527A) else Color.LightGray
    val gridBackgroundColor = if (isDarkTheme) Color(0xFF1E2238).copy(alpha = 0.85f) else Color(0xFF282C44).copy(alpha = 0.9f)
    val controlButtonColor = if (isDarkTheme) Color(0xFF2A2A35).copy(alpha = 0.8f) else Color(0xFFE0E0E0).copy(alpha = 0.9f)
    val overlayColor = if (isDarkTheme) Color(0xFF0A0A1A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)

    LaunchedEffect(gameState.clearTrigger) {
        if (gameState.clearTrigger > 0 && gameState.clearedRows.isNotEmpty()) {
            haptics.heavy()
            flashingRows = gameState.clearedRows
            delay(150)
            flashingRows = emptyList()
        }
    }

    LaunchedEffect(gameState.isGameOver, gameState.level, gameState.isPaused, gameState.difficulty) {
        if (!gameState.isGameOver && !gameState.isPaused) {
            while (true) {
                val speed = when (gameState.difficulty) {
                    Difficulty.EASY -> (1000 - (gameState.level - 1) * 35).coerceAtLeast(150).toLong()
                    Difficulty.MEDIUM -> (1000 - (gameState.level - 1) * 50).coerceAtLeast(100).toLong()
                    Difficulty.HARD -> (800 - (gameState.level - 1) * 55).coerceAtLeast(70).toLong()
                }
                delay(speed)
                engine.moveDown()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // HORNÍ PANEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreBox("SKÓRE", gameState.score.toString(), textColor)
                ScoreBox("REKORD", highScore.toString(), textColor)
                ScoreBox("LEVEL", gameState.level.toString(), textColor)
                ScoreBox("ŘADY", gameState.linesCleared.toString(), textColor)

                IconButton(onClick = {
                    haptics.light()
                    engine.togglePause()
                }) {
                    Text(
                        text = if (gameState.isPaused) "▶" else "⏸",
                        fontSize = 24.sp,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STŘEDNÍ ČÁST
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Levý sloupec
                Column(
                    modifier = Modifier.width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PiecePreviewBox("NÁSLEDUJÍCÍ", gameState.nextPiece, textColor, surfaceColor, borderColor)
                    PiecePreviewBox("SCHRÁNKA", gameState.holdPiece, textColor, surfaceColor, borderColor)

                    OutlinedButton(
                        onClick = {
                            haptics.light()
                            engine.hold()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor, containerColor = surfaceColor)
                    ) {
                        Text("Ulož", fontSize = 12.sp)
                    }

                    // Volba obtížnosti pod tlačítkem Ulož
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "OBTÍŽNOST",
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                haptics.light()
                                engine.cycleDifficulty()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor, containerColor = surfaceColor)
                        ) {
                            Text(gameState.difficulty.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Volba startovního levelu
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "START LVL",
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(surfaceColor, RoundedCornerShape(8.dp)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    .clickable {
                                        haptics.light()
                                        engine.changeStartLevel(-1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = gameState.startLevel.toString(),
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                    .clickable {
                                        haptics.light()
                                        engine.changeStartLevel(1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Hlavní herní plocha s glow efektem
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(0.5f)
                        .border(2.dp, borderColor.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .background(gridBackgroundColor, RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cellWidth = size.width / 10
                        val cellHeight = size.height / 20

                        for (y in 0 until 20) {
                            for (x in 0 until 10) {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.12f),
                                    topLeft = Offset(x * cellWidth, y * cellHeight),
                                    size = Size(cellWidth, cellHeight),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }

                        for (y in 0 until 20) {
                            for (x in 0 until 10) {
                                val type = gameState.grid[y][x]
                                if (type != null) {
                                    drawGlowingBlock(x, y, cellWidth, cellHeight, getColorForType(type))
                                }
                            }
                        }

                        val ghost = engine.getGhostPiece()
                        if (ghost != null && !gameState.isPaused) {
                            val color = getColorForType(ghost.type).copy(alpha = 0.3f)
                            for (pos in ghost.getAbsolutePositions()) {
                                if (pos.y >= 0) {
                                    drawGlowingBlock(pos.x, pos.y, cellWidth, cellHeight, color, isGhost = true)
                                }
                            }
                        }

                        val current = gameState.currentPiece
                        if (current != null && !gameState.isPaused) {
                            val color = getColorForType(current.type)
                            for (pos in current.getAbsolutePositions()) {
                                if (pos.y >= 0) {
                                    drawGlowingBlock(pos.x, pos.y, cellWidth, cellHeight, color)
                                }
                            }
                        }

                        for (rowY in flashingRows) {
                            drawRoundRect(
                                color = colorCyan.copy(alpha = 0.4f),
                                topLeft = Offset(0f, rowY * cellHeight - 4f),
                                size = Size(size.width, cellHeight + 8f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.9f),
                                topLeft = Offset(0f, rowY * cellHeight),
                                size = Size(size.width, cellHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }

                    if (gameState.isPaused && !gameState.isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(overlayColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PAUZA", color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                        }
                    }

                    if (gameState.isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(overlayColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("KONEC HRY", color = colorRed, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        haptics.heavy()
                                        engine.restart()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colorCyan, contentColor = Color.Black)
                                ) {
                                    Text("Hrát znovu", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SPODNÍ PANEL: Ovládání
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.light()
                        engine.moveLeft()
                    },
                    modifier = Modifier.size(64.dp).background(controlButtonColor, CircleShape)
                ) { Text("◀", fontSize = 28.sp, color = textColor) }

                IconButton(
                    onClick = {
                        haptics.light()
                        engine.rotate()
                    },
                    modifier = Modifier.size(64.dp).background(controlButtonColor, CircleShape)
                ) { Text("↻", fontSize = 28.sp, color = textColor) }

                IconButton(
                    onClick = {
                        haptics.heavy()
                        engine.hardDrop()
                    },
                    modifier = Modifier.size(64.dp).background(controlButtonColor, CircleShape)
                ) { Text("▼", fontSize = 28.sp, color = textColor) }

                IconButton(
                    onClick = {
                        haptics.light()
                        engine.moveRight()
                    },
                    modifier = Modifier.size(64.dp).background(controlButtonColor, CircleShape)
                ) { Text("▶", fontSize = 28.sp, color = textColor) }
            }
        }
    }
}

@Composable
fun ScoreBox(label: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = textColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PiecePreviewBox(label: String, type: TetrominoType?, textColor: Color, surfaceColor: Color, borderColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = textColor.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .background(surfaceColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (type != null) {
                Canvas(modifier = Modifier.size(40.dp)) {
                    val blocks = SHAPES[type]!!
                    val cellSize = size.width / 4f
                    val offsetX = size.width / 2f
                    val offsetY = size.height / 2f

                    for (pos in blocks) {
                        drawGlowingBlock(
                            x = pos.x,
                            y = pos.y,
                            width = cellSize,
                            height = cellSize,
                            color = getColorForType(type),
                            offsetX = offsetX + (if (type == TetrominoType.O || type == TetrominoType.I) -cellSize / 2 else 0f),
                            offsetY = offsetY + (if (type == TetrominoType.I) -cellSize / 2 else 0f)
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.drawGlowingBlock(
    x: Int, y: Int,
    width: Float, height: Float,
    color: Color,
    isGhost: Boolean = false,
    offsetX: Float = 0f,
    offsetY: Float = 0f
) {
    val posX = (x * width) + offsetX
    val posY = (y * height) + offsetY
    val padding = 1f

    if (isGhost) {
        drawRoundRect(
            color = color,
            topLeft = Offset(posX + padding, posY + padding),
            size = Size(width - (padding * 2), height - (padding * 2)),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )
    } else {
        drawRoundRect(
            color = color.copy(alpha = 0.2f),
            topLeft = Offset(posX - 2f, posY - 2f),
            size = Size(width + 4f, height + 4f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        drawRoundRect(
            color = color.copy(alpha = 0.6f),
            topLeft = Offset(posX + padding, posY + padding),
            size = Size(width - (padding * 2), height - (padding * 2)),
            cornerRadius = CornerRadius(4f, 4f)
        )

        val innerPadding = width * 0.2f
        drawRoundRect(
            color = color,
            topLeft = Offset(posX + innerPadding, posY + innerPadding),
            size = Size(width - (innerPadding * 2), height - (innerPadding * 2)),
            cornerRadius = CornerRadius(2f, 2f)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(posX + padding + 1f, posY + padding + 1f),
            size = Size((width - (padding * 2)) / 3f, (height - (padding * 2)) / 3f),
            cornerRadius = CornerRadius(1f, 1f)
        )
    }
}
