import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Automatické přepsání názvu v TetrisScreen.kt na Neonové kostky před kompilací
val screenFile = file("src/commonMain/kotlin/cz/mares/tetrisek/TetrisScreen.kt")
if (screenFile.exists()) {
    val originalText = screenFile.readText()
    val updatedText = originalText
        .replace("TETRÍSEK", "NEONOVÉ KOSTKY")
        .replace("Tetrísek", "Neonové kostky")
    if (originalText != updatedText) {
        screenFile.writeText(updatedText)
    }
}

// Automatické vykreslení 1024x1024 PNG ikony pro iOS v čistém Kotlinu (bez java.awt)
tasks.register("generateIosAppIcon") {
    group = "build"
    description = "Generuje iOS ikonu 1024x1024 px přímo při kompilaci bez závislosti na java.awt."
    val outputDir = file("../iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
    val outputFile = file("$outputDir/app-icon-1024.png")

    inputs.property("version", "1.2")
    outputs.file(outputFile)

    doLast {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val size = 1024
        val pixels = IntArray(size * size)

        fun blendPixel(x: Int, y: Int, r: Int, g: Int, b: Int, a: Int) {
            if (x !in 0 until size || y !in 0 until size || a <= 0) return
            val idx = y * size + x
            if (a >= 255) {
                pixels[idx] = (r shl 16) or (g shl 8) or b
                return
            }
            val bg = pixels[idx]
            val bgR = (bg shr 16) and 0xFF
            val bgG = (bg shr 8) and 0xFF
            val bgB = bg and 0xFF
            val outR = (r * a + bgR * (255 - a)) / 255
            val outG = (g * a + bgG * (255 - a)) / 255
            val outB = (b * a + bgB * (255 - a)) / 255
            pixels[idx] = (outR shl 16) or (outG shl 8) or outB
        }

        fun isInsideRoundRect(px: Int, py: Int, rx: Int, ry: Int, rw: Int, rh: Int, rad: Int): Boolean {
            if (px < rx || px >= rx + rw || py < ry || py >= ry + rh) return false
            val r = rad.coerceAtLeast(0)
            if (r == 0) return true
            val cx = when {
                px < rx + r -> rx + r
                px >= rx + rw - r -> rx + rw - r - 1
                else -> px
            }
            val cy = when {
                py < ry + r -> ry + r
                py >= ry + rh - r -> ry + rh - r - 1
                else -> py
            }
            val dx = px - cx
            val dy = py - cy
            return dx * dx + dy * dy <= r * r
        }

        fun fillRoundRect(rx: Int, ry: Int, rw: Int, rh: Int, rad: Int, r: Int, g: Int, b: Int, a: Int) {
            val x0 = rx.coerceAtLeast(0)
            val y0 = ry.coerceAtLeast(0)
            val x1 = (rx + rw).coerceAtMost(size)
            val y1 = (ry + rh).coerceAtMost(size)
            for (py in y0 until y1) {
                for (px in x0 until x1) {
                    if (isInsideRoundRect(px, py, rx, ry, rw, rh, rad)) {
                        blendPixel(px, py, r, g, b, a)
                    }
                }
            }
        }

        fun strokeRoundRect(rx: Int, ry: Int, rw: Int, rh: Int, rad: Int, stroke: Int, r: Int, g: Int, b: Int, a: Int) {
            val x0 = rx.coerceAtLeast(0)
            val y0 = ry.coerceAtLeast(0)
            val x1 = (rx + rw).coerceAtMost(size)
            val y1 = (ry + rh).coerceAtMost(size)
            val innerRad = (rad - stroke).coerceAtLeast(0)
            for (py in y0 until y1) {
                for (px in x0 until x1) {
                    val inOuter = isInsideRoundRect(px, py, rx, ry, rw, rh, rad)
                    val inInner = isInsideRoundRect(px, py, rx + stroke, ry + stroke, rw - stroke * 2, rh - stroke * 2, innerRad)
                    if (inOuter && !inInner) {
                        blendPixel(px, py, r, g, b, a)
                    }
                }
            }
        }

        // 1. Vertikální gradient pozadí (#0A0A1A -> #140024)
        for (y in 0 until size) {
            val r = 10 + (10 * y) / size
            val g = 10 - (10 * y) / size
            val b = 26 + (10 * y) / size
            val color = (r shl 16) or (g shl 8) or b
            val rowOffset = y * size
            for (x in 0 until size) {
                pixels[rowOffset + x] = color
            }
        }

        // 2. Mřížka na pozadí
        val cellCount = 16
        val gridStep = size / cellCount
        for (i in 1 until cellCount) {
            val pos = i * gridStep
            for (k in 0 until size) {
                blendPixel(pos, k, 255, 255, 255, 22)
                blendPixel(pos + 1, k, 255, 255, 255, 22)
                blendPixel(k, pos, 255, 255, 255, 22)
                blendPixel(k, pos + 1, 255, 255, 255, 22)
            }
        }

        // 3. Vnější prosvětlený rámeček herní plochy
        fillRoundRect(96, 96, size - 192, size - 192, 80, 30, 34, 56, 170)
        strokeRoundRect(90, 90, size - 180, size - 180, 86, 10, 74, 82, 122, 210)

        // Pomocná funkce pro vykreslení neonového bloku
        fun drawBlock(bx: Int, by: Int, r: Int, g: Int, b: Int) {
            val blockSize = 130
            val px = bx + 6
            val py = by + 6
            val w = blockSize - 12
            val h = blockSize - 12

            fillRoundRect(px - 10, py - 10, w + 20, h + 20, 26, r, g, b, 65)
            fillRoundRect(px, py, w, h, 18, r, g, b, 175)
            fillRoundRect(px + 20, py + 20, w - 40, h - 40, 10, r, g, b, 255)
            fillRoundRect(px + 10, py + 10, 34, 34, 8, 255, 255, 255, 145)
        }

        val cx = 512
        val cy = 512

        // 4. Obrys Ducha (Ghost T-piece)
        val gxOffset = cx - 195
        val gyOffset = cy - 65
        strokeRoundRect(gxOffset + 8, gyOffset + 138, 114, 114, 18, 5, 187, 0, 255, 110)
        strokeRoundRect(gxOffset + 138, gyOffset + 138, 114, 114, 18, 5, 187, 0, 255, 110)
        strokeRoundRect(gxOffset + 268, gyOffset + 138, 114, 114, 18, 5, 187, 0, 255, 110)

        // 5. Aktivní fialové T (#BB00FF)
        val txOffset = cx - 195
        val tyOffset = cy - 310
        drawBlock(txOffset + 130, tyOffset, 187, 0, 255)
        drawBlock(txOffset, tyOffset + 130, 187, 0, 255)
        drawBlock(txOffset + 130, tyOffset + 130, 187, 0, 255)
        drawBlock(txOffset + 260, tyOffset + 130, 187, 0, 255)

        // 6. Spodní usazené kostky
        val bxOffset = cx - 390
        val byOffset = cy + 110

        // Azurová I řada (#00FFFF)
        drawBlock(bxOffset, byOffset + 130, 0, 255, 255)
        drawBlock(bxOffset + 130, byOffset + 130, 0, 255, 255)
        drawBlock(bxOffset + 260, byOffset + 130, 0, 255, 255)
        drawBlock(bxOffset + 390, byOffset + 130, 0, 255, 255)

        // Červený Z blok (#FF0033)
        drawBlock(bxOffset, byOffset, 255, 0, 51)
        drawBlock(bxOffset + 130, byOffset, 255, 0, 51)
        drawBlock(bxOffset + 130, byOffset - 130, 255, 0, 51)
        drawBlock(bxOffset + 260, byOffset - 130, 255, 0, 51)

        // Zelený S blok (#00FF00)
        drawBlock(bxOffset + 260, byOffset, 0, 255, 0)
        drawBlock(bxOffset + 390, byOffset, 0, 255, 0)
        drawBlock(bxOffset + 520, byOffset, 0, 255, 0)

        // Oranžový L blok (#FFA500)
        drawBlock(bxOffset + 520, byOffset + 130, 255, 165, 0)
        drawBlock(bxOffset + 650, byOffset + 130, 255, 165, 0)
        drawBlock(bxOffset + 650, byOffset, 255, 165, 0)
        drawBlock(bxOffset + 650, byOffset - 130, 255, 165, 0)

        // 7. Zápis čistého RGB PNG souboru (bez alfa kanálu pro Apple App Store)
        fun writeChunk(out: DataOutputStream, type: String, data: ByteArray) {
            out.writeInt(data.size)
            val typeBytes = type.toByteArray(Charsets.US_ASCII)
            out.write(typeBytes)
            out.write(data)
            val crc = CRC32()
            crc.update(typeBytes)
            crc.update(data)
            out.writeInt(crc.value.toInt())
        }

        FileOutputStream(outputFile).use { fos ->
            val out = DataOutputStream(fos)
            out.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))

            val ihdr = ByteArrayOutputStream()
            DataOutputStream(ihdr).apply {
                writeInt(size)
                writeInt(size)
                writeByte(8)
                writeByte(2)
                writeByte(0)
                writeByte(0)
                writeByte(0)
            }
            writeChunk(out, "IHDR", ihdr.toByteArray())

            val idatRaw = ByteArrayOutputStream()
            DeflaterOutputStream(idatRaw).use { deflater ->
                val rowBuffer = ByteArray(1 + size * 3)
                for (y in 0 until size) {
                    rowBuffer[0] = 0
                    var bufIdx = 1
                    val rowStart = y * size
                    for (x in 0 until size) {
                        val rgb = pixels[rowStart + x]
                        rowBuffer[bufIdx++] = ((rgb shr 16) and 0xFF).toByte()
                        rowBuffer[bufIdx++] = ((rgb shr 8) and 0xFF).toByte()
                        rowBuffer[bufIdx++] = (rgb and 0xFF).toByte()
                    }
                    deflater.write(rowBuffer)
                }
            }
            writeChunk(out, "IDAT", idatRaw.toByteArray())

            writeChunk(out, "IEND", ByteArray(0))
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.0")
        }
        iosMain.dependencies {
        }
    }
}

android {
    namespace = "cz.mares.tetrisek"
    compileSdk = 36

    defaultConfig {
        applicationId = "cz.mares.tetrisek"
        minSdk = 24
        targetSdk = 36

        val rawBuild = System.getenv("PROJECT_BUILD_NUMBER")?.toIntOrNull()
            ?: System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
            ?: 1
        val ciBuildNumber = if (rawBuild < 100) rawBuild + 100 else rawBuild
        versionCode = ciBuildNumber
        versionName = "1.0.$ciBuildNumber"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("CM_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("CM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CM_KEY_ALIAS")
                keyPassword = System.getenv("CM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            val keystorePath = System.getenv("CM_KEYSTORE_PATH")
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.matching { it.name.startsWith("link") && it.name.contains("FrameworkIos") }.configureEach {
    dependsOn("generateIosAppIcon")
}
