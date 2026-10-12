package cz.tetrisek.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
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

// Barvy pro neonový vzhled
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

    // Herní smyčka pro automatický padající pohyb
    LaunchedEffect(gameState.isGameOver, gameState.level) {
        if (!gameState.isGameOver) {
            while (true) {
                // Zvyšování rychlosti podle úrovně, maximální rychlost je 100ms
                val speed = (1000 - (gameState.level - 1) * 50).coerceAtLeast(100).toLong()
                delay(speed)
                engine.moveDown()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Horní panel: Skóre, Level a Řady
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreBox("SKÓRE", gameState.score.toString())
            ScoreBox("LEVEL", gameState.level.toString())
            ScoreBox("ŘADY", gameState.linesCleared.toString())
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Střední část: Hold, Hrací plocha, Next
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Odložená kostka
            PiecePreviewBox("HOLD", gameState.holdPiece)

            Spacer(modifier = Modifier.width(16.dp))

            // Hlavní herní plocha
            Box(
                modifier = Modifier
                    .aspectRatio(0.5f) // Poměr stran 10x20
                    .border(2.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                    .background(Color(0xFF111111))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellWidth = size.width / 10
                    val cellHeight = size.height / 20

                    // Vykreslení usazených bloků
                    for (y in 0 until 20) {
                        for (x in 0 until 10) {
                            val type = gameState.grid[y][x]
                            if (type != null) {
                                drawBlock(x, y, cellWidth, cellHeight, getColorForType(type))
                            } else {
                                // Mřížka na pozadí pro lepší přehlednost
                                drawRect(
                                    color = Color.DarkGray.copy(alpha = 0.2f),
                                    topLeft = Offset(x * cellWidth, y * cellHeight),
                                    size = Size(cellWidth, cellHeight),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }
                    }

                    // Vykreslení "Ducha" (Ghost Piece)
                    val ghost = engine.getGhostPiece()
                    if (ghost != null) {
                        val color = getColorForType(ghost.type).copy(alpha = 0.4f)
                        for (pos in ghost.getAbsolutePositions()) {
                            if (pos.y >= 0) {
                                drawBlock(pos.x, pos.y, cellWidth, cellHeight, color, isGhost = true)
                            }
                        }
                    }

                    // Vykreslení aktuálně padající kostky
                    val current = gameState.currentPiece
                    if (current != null) {
                        val color = getColorForType(current.type)
                        for (pos in current.getAbsolutePositions()) {
                            if (pos.y >= 0) {
                                drawBlock(pos.x, pos.y, cellWidth, cellHeight, color)
                            }
                        }
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
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))
                            ) {
                                Text("Hrát znovu", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Následující kostka
            PiecePreviewBox("NEXT", gameState.nextPiece)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Spodní panel: Ovládací tlačítka
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlButton("HOLD") { engine.hold() }
                ControlButton("OTOČ") { engine.rotate() }
                ControlButton("PÁD") { engine.hardDrop() }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlButton("← VLEVO", modifier = Modifier.weight(1f)) { engine.moveLeft() }
                Spacer(modifier = Modifier.width(16.dp))
                ControlButton("VPRAVO →", modifier = Modifier.weight(1f)) { engine.moveRight() }
            }
        }
    }
}

@Composable
fun ScoreBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PiecePreviewBox(label: String, type: TetrominoType?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .background(Color(0xFF111111)),
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

@Composable
fun ControlButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF222222)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(60.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        // Vykreslení obrysu pro "ducha"
        drawRoundRect(
            color = color,
            topLeft = Offset(posX + 2f, posY + 2f),
            size = Size(width - 4f, height - 4f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )
    } else {
        // Vykreslení pevného bloku
        drawRoundRect(
            color = color,
            topLeft = Offset(posX + 1f, posY + 1f),
            size = Size(width - 2f, height - 2f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Efekt neonu - vnitřní záře/zvýraznění
        drawRoundRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset(posX + 3f, posY + 3f),
            size = Size(width - 6f, height - 6f),
            cornerRadius = CornerRadius(2f, 2f),
            style = Stroke(width = 1f)
        )
    }
}