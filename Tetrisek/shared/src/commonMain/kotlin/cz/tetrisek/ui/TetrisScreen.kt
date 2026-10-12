package cz.tetrisek.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.tetrisek.engine.SHAPES
import cz.tetrisek.engine.TetrisEngine
import cz.tetrisek.engine.TetrominoType
import kotlinx.coroutines.delay

// Základní neonové barvy
val colorCyan = Color(0xFF00FFFF)
val colorBlue = Color(0xFF0000FF)
val colorOrange = Color(0xFFFFA500)
val colorYellow = Color(0xFFFFFF00)
val colorGreen = Color(0xFF00FF00)
val colorPurple = Color(0xFF800080)
val colorRed = Color(0xFFFF0000)

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

    // Barvy reagující na den/noc
    val backgroundColor = if (isDarkTheme) Color.Black else Color(0xFFF0F0F0)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val surfaceColor = if (isDarkTheme) Color(0xFF111111) else Color.White
    val borderColor = if (isDarkTheme) Color.DarkGray else Color.LightGray
    val gridBackgroundColor = if (isDarkTheme) Color(0xFF111111) else Color(0xFF222222)
    val controlButtonColor = if (isDarkTheme) Color(0xFF333333) else Color(0xFFE0E0E0)

    // Herní smyčka
    LaunchedEffect(gameState.isGameOver, gameState.level, gameState.isPaused) {
        if (!gameState.isGameOver && !gameState.isPaused) {
            while (true) {
                val speed = (1000 - (gameState.level - 1) * 50).coerceAtLeast(100).toLong()
                delay(speed)
                engine.moveDown()
            }
        }
    }

    // Aplikace systémových insets (bezpečných zón proti překrytí)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // HORNÍ PANEL: Skóre, Level, Řady a tlačítko Pauza
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreBox("SKÓRE", gameState.score.toString(), textColor)
                ScoreBox("LEVEL", gameState.level.toString(), textColor)
                ScoreBox("ŘADY", gameState.linesCleared.toString(), textColor)

                IconButton(onClick = { engine.togglePause() }) {
                    Text(
                        text = if (gameState.isPaused) "▶" else "⏸",
                        fontSize = 24.sp,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STŘEDNÍ ČÁST: Panely vlevo, Hrací plocha vpravo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Levý sloupec (Následující a Schránka)
                Column(
                    modifier = Modifier.width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PiecePreviewBox("NÁSLEDUJÍCÍ", gameState.nextPiece, textColor, surfaceColor, borderColor)
                    PiecePreviewBox("SCHRÁNKA", gameState.holdPiece, textColor, surfaceColor, borderColor)

                    OutlinedButton(
                        onClick = { engine.hold() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Text("Ulož", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Hlavní herní plocha
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(0.5f)
                        .border(2.dp, borderColor, RoundedCornerShape(4.dp))
                        .background(gridBackgroundColor)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cellWidth = size.width / 10
                        val cellHeight = size.height / 20

                        for (y in 0 until 20) {
                            for (x in 0 until 10) {
                                val type = gameState.grid[y][x]
                                if (type != null) {
                                    drawBlock(x, y, cellWidth, cellHeight, getColorForType(type))
                                } else {
                                    drawRect(
                                        color = Color.DarkGray.copy(alpha = 0.2f),
                                        topLeft = Offset(x * cellWidth, y * cellHeight),
                                        size = Size(cellWidth, cellHeight),
                                        style = Stroke(width = 1f)
                                    )
                                }
                            }
                        }

                        val ghost = engine.getGhostPiece()
                        if (ghost != null && !gameState.isPaused) {
                            val color = getColorForType(ghost.type).copy(alpha = 0.4f)
                            for (pos in ghost.getAbsolutePositions()) {
                                if (pos.y >= 0) {
                                    drawBlock(pos.x, pos.y, cellWidth, cellHeight, color, isGhost = true)
                                }
                            }
                        }

                        val current = gameState.currentPiece
                        if (current != null && !gameState.isPaused) {
                            val color = getColorForType(current.type)
                            for (pos in current.getAbsolutePositions()) {
                                if (pos.y >= 0) {
                                    drawBlock(pos.x, pos.y, cellWidth, cellHeight, color)
                                }
                            }
                        }
                    }

                    if (gameState.isPaused && !gameState.isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PAUZA", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        }
                    }

                    if (gameState.isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("KONEC HRY", color = colorRed, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { engine.restart() },
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

            // SPODNÍ PANEL: Grafické ovládání pomocí textových znaků
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Šipka doleva
                IconButton(
                    onClick = { engine.moveLeft() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(controlButtonColor, CircleShape)
                ) {
                    Text("◀", fontSize = 28.sp, color = textColor)
                }

                // Rotace
                IconButton(
                    onClick = { engine.rotate() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(controlButtonColor, CircleShape)
                ) {
                    Text("↻", fontSize = 28.sp, color = textColor)
                }

                // Šipka dolů (Hard Drop)
                IconButton(
                    onClick = { engine.hardDrop() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(controlButtonColor, CircleShape)
                ) {
                    Text("▼", fontSize = 28.sp, color = textColor)
                }

                // Šipka doprava
                IconButton(
                    onClick = { engine.moveRight() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(controlButtonColor, CircleShape)
                ) {
                    Text("▶", fontSize = 28.sp, color = textColor)
                }
            }
        }
    }
}

@Composable
fun ScoreBox(label: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PiecePreviewBox(label: String, type: TetrominoType?, textColor: Color, surfaceColor: Color, borderColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
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
                        drawBlock(
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

fun DrawScope.drawBlock(
    x: Int, y: Int,
    width: Float, height: Float,
    color: Color,
    isGhost: Boolean = false,
    offsetX: Float = 0f,
    offsetY: Float = 0f
) {
    val posX = (x * width) + offsetX
    val posY = (y * height) + offsetY

    if (isGhost) {
        drawRoundRect(
            color = color,
            topLeft = Offset(posX + 2f, posY + 2f),
            size = Size(width - 4f, height - 4f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )
    } else {
        drawRoundRect(
            color = color,
            topLeft = Offset(posX + 1f, posY + 1f),
            size = Size(width - 2f, height - 2f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset(posX + 3f, posY + 3f),
            size = Size(width - 6f, height - 6f),
            cornerRadius = CornerRadius(2f, 2f),
            style = Stroke(width = 1f)
        )
    }
}