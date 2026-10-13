package com.example.tugas_sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tugas_sensor.ui.theme.BrightBall
import com.example.tugas_sensor.ui.theme.BrightBg
import com.example.tugas_sensor.ui.theme.BrightCardBg
import com.example.tugas_sensor.ui.theme.BrightText
import com.example.tugas_sensor.ui.theme.DarkBall
import com.example.tugas_sensor.ui.theme.DarkBg
import com.example.tugas_sensor.ui.theme.DarkCardBg
import com.example.tugas_sensor.ui.theme.DarkText
import com.example.tugas_sensor.ui.theme.NormalBall
import com.example.tugas_sensor.ui.theme.NormalBg
import com.example.tugas_sensor.ui.theme.NormalCardBg
import com.example.tugas_sensor.ui.theme.NormalText
import com.example.tugas_sensor.ui.theme.SensorActiveGreen
import com.example.tugas_sensor.ui.theme.SensorInactiveRed
import com.example.tugas_sensor.ui.theme.Tugas_SensorTheme
import java.util.Locale
import kotlin.math.exp

class MainActivity : ComponentActivity(), SensorEventListener {

    // Sensor Manager & Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lightSensor: Sensor? = null

    // State Variables for Sensor Availability
    private var isAccelerometerAvailable by mutableStateOf(value = false)
    private var isLightSensorAvailable by mutableStateOf(value = false)

    // State Variables for Real-Time Sensor Readings
    private var accelX by mutableFloatStateOf(0f)
    private var accelY by mutableFloatStateOf(0f)
    private var accelZ by mutableFloatStateOf(0f)

    private var lightLux by mutableFloatStateOf(0f)

    // Low-pass filter supaya gerakan bola halus (tidak jitter)
    private var hasAccelData = false
    private val filterAlpha = 0.15f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Inisialisasi SensorManager & Ambil Sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Cek ketersediaan sensor
        isAccelerometerAvailable = (accelerometer != null)
        isLightSensorAvailable = (lightSensor != null)

        setContent {
            Tugas_SensorTheme {
                SensorPlaygroundScreen(
                    isAccelerometerAvailable = isAccelerometerAvailable,
                    isLightSensorAvailable = isLightSensorAvailable,
                    accelX = accelX,
                    accelY = accelY,
                    accelZ = accelZ,
                    lightLux = lightLux
                )
            }
        }
    }

    // 2. Lifecycle: Daftarkan Listener saat Activity dalam kondisi Resumed
    override fun onResume() {
        super.onResume()

        // Accelerometer: SENSOR_DELAY_GAME untuk pembaruan cepat
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Light sensor
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // 3. Lifecycle: Lepaskan Listener saat Activity Pause
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // Callback pembaruan data sensor real-time
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val rx = it.values[0]
                    val ry = it.values[1]
                    val rz = it.values[2]
                    if (!hasAccelData) {
                        accelX = rx
                        accelY = ry
                        accelZ = rz
                        hasAccelData = true
                    } else {
                        accelX += filterAlpha * (rx - accelX)
                        accelY += filterAlpha * (ry - accelY)
                        accelZ += filterAlpha * (rz - accelZ)
                    }
                }
                Sensor.TYPE_LIGHT -> {
                    lightLux = it.values[0]
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Tidak digunakan pada aplikasi ini
    }
}

// Enum untuk Status Mode Cahaya
enum class LightCondition(val label: String, val icon: ImageVector) {
    DARK("Gelap", Icons.Default.Nightlight),
    NORMAL("Normal", Icons.Default.LightMode),
    BRIGHT("Terang", Icons.Default.WbSunny),
    UNAVAILABLE("Tidak Tersedia", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorPlaygroundScreen(
    isAccelerometerAvailable: Boolean,
    isLightSensorAvailable: Boolean,
    accelX: Float,
    accelY: Float,
    accelZ: Float,
    lightLux: Float
) {
    // Penentuan kategori mode cahaya berdasarkan tingkat lux
    val lightCondition = when {
        !isLightSensorAvailable -> LightCondition.UNAVAILABLE
        lightLux < 15f -> LightCondition.DARK
        lightLux <= 300f -> LightCondition.NORMAL
        else -> LightCondition.BRIGHT
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sensor Playground",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Eksplorasi Motion & Environment Sensor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Card 1: Petunjuk Penggunaan
            InstructionCard()

            // Section: Status Sensor & Pembacaan Real-Time
            Text(
                text = "Status & Pembacaan Sensor Real-Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccelerometerStatusCard(
                    modifier = Modifier.weight(1f),
                    isAvailable = isAccelerometerAvailable,
                    x = accelX,
                    y = accelY,
                    z = accelZ
                )

                LightSensorStatusCard(
                    modifier = Modifier.weight(1f),
                    isAvailable = isLightSensorAvailable,
                    lux = lightLux,
                    condition = lightCondition
                )
            }

            // Section: Area Permainan Interactive Canvas
            GameAreaCard(
                isAccelerometerAvailable = isAccelerometerAvailable,
                accelX = accelX,
                accelY = accelY,
                accelZ = accelZ,
                lightLux = lightLux,
                lightCondition = lightCondition
            )

            // Educational Info Card
            EducationalInfoCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InstructionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Petunjuk Penggunaan Interaktif 3-Sumbu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "• Sumbu X (Miring Kanan/Kiri): Geser bola horizontal.\n" +
                            "• Sumbu Y (Miring Depan/Belakang): Geser bola vertikal.\n" +
                            "• Sumbu Z (Dorong/Angkat HP): Ubah ukuran 3D bola.\n" +
                            "• Tekan Kalibrasi untuk menjadikan posisi pegangan saat ini sebagai titik netral.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun AccelerometerStatusCard(
    modifier: Modifier = Modifier,
    isAvailable: Boolean,
    x: Float,
    y: Float,
    z: Float
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Accelerometer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                StatusBadge(isAvailable = isAvailable)
            }

            Text(
                text = "Kategori: Motion",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isAvailable) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ReadingRow(label = "X (Kiri/Kanan):", value = String.format(Locale.getDefault(), "%.2f m/s²", x))
                    ReadingRow(label = "Y (Atas/Bawah):", value = String.format(Locale.getDefault(), "%.2f m/s²", y))
                    ReadingRow(label = "Z (Depan/Belakang):", value = String.format(Locale.getDefault(), "%.2f m/s²", z))
                }
            } else {
                Text(
                    text = "Sensor tidak ditemukan pada perangkat ini.",
                    fontSize = 11.sp,
                    color = SensorInactiveRed
                )
            }
        }
    }
}

@Composable
fun LightSensorStatusCard(
    modifier: Modifier = Modifier,
    isAvailable: Boolean,
    lux: Float,
    condition: LightCondition
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Light Sensor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                StatusBadge(isAvailable = isAvailable)
            }

            Text(
                text = "Kategori: Environment",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isAvailable) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ReadingRow(label = "Intensitas:", value = String.format(Locale.getDefault(), "%.1f lux", lux))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Kondisi:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = condition.icon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = condition.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Sensor cahaya tidak tersedia pada perangkat/emulator ini.",
                    fontSize = 11.sp,
                    color = SensorInactiveRed
                )
            }
        }
    }
}

@Composable
fun ReadingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusBadge(isAvailable: Boolean) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isAvailable) SensorActiveGreen.copy(alpha = 0.15f) else SensorInactiveRed.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isAvailable) "Tersedia" else "N/A",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAvailable) SensorActiveGreen else SensorInactiveRed
        )
    }
}

@Composable
fun GameAreaCard(
    isAccelerometerAvailable: Boolean,
    accelX: Float,
    accelY: Float,
    accelZ: Float,
    lightLux: Float,
    lightCondition: LightCondition
) {
    // Warna dinamis berdasarkan sensor cahaya
    val targetBgColor = when (lightCondition) {
        LightCondition.DARK -> DarkBg
        LightCondition.NORMAL, LightCondition.UNAVAILABLE -> NormalBg
        LightCondition.BRIGHT -> BrightBg
    }
    val targetCardBgColor = when (lightCondition) {
        LightCondition.DARK -> DarkCardBg
        LightCondition.NORMAL, LightCondition.UNAVAILABLE -> NormalCardBg
        LightCondition.BRIGHT -> BrightCardBg
    }
    val targetTextColor = when (lightCondition) {
        LightCondition.DARK -> DarkText
        LightCondition.NORMAL, LightCondition.UNAVAILABLE -> NormalText
        LightCondition.BRIGHT -> BrightText
    }
    val targetBallColor = when (lightCondition) {
        LightCondition.DARK -> DarkBall
        LightCondition.NORMAL, LightCondition.UNAVAILABLE -> NormalBall
        LightCondition.BRIGHT -> BrightBall
    }

    val animatedBgColor by animateColorAsState(targetBgColor, animationSpec = tween(500), label = "BgAnim")
    val animatedCardBgColor by animateColorAsState(targetCardBgColor, animationSpec = tween(500), label = "CardBgAnim")
    val animatedTextColor by animateColorAsState(targetTextColor, animationSpec = tween(500), label = "TextAnim")
    val animatedBallColor by animateColorAsState(targetBallColor, animationSpec = tween(500), label = "BallAnim")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardBgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Area Permainan Bola 3D",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = animatedTextColor
                    )
                    Text(
                        text = "Responsif Sumbu X, Y, Z HP",
                        fontSize = 11.sp,
                        color = animatedTextColor.copy(alpha = 0.7f)
                    )
                }

                // Indicator Badge Light Condition
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedTextColor.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = lightCondition.icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = animatedTextColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${lightCondition.label} (${if (lightCondition == LightCondition.UNAVAILABLE) "N/A" else String.format(Locale.getDefault(), "%.0f lx", lightLux)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedTextColor
                    )
                }
            }

            // Canvas Game Surface
            val density = LocalDensity.current
            val baseBallRadiusPx = with(density) { 22.dp.toPx() }

            var canvasWidthPx by remember { mutableFloatStateOf(0f) }
            var canvasHeightPx by remember { mutableFloatStateOf(0f) }

            var ballX by remember { mutableFloatStateOf(0f) }
            var ballY by remember { mutableFloatStateOf(0f) }
            var vx by remember { mutableFloatStateOf(0f) }
            var vy by remember { mutableFloatStateOf(0f) }

            // Titik netral (kalibrasi) = posisi pegangan HP yang dianggap "datar"
            var calX by remember { mutableFloatStateOf(0f) }
            var calY by remember { mutableFloatStateOf(0f) }
            var calZ by remember { mutableFloatStateOf(0f) }
            var isCalibrated by remember { mutableStateOf(false) }

            // Kalibrasi otomatis sekali saat data sensor pertama masuk
            LaunchedEffect(accelX, accelY, accelZ, isAccelerometerAvailable) {
                if (isAccelerometerAvailable && !isCalibrated &&
                    (accelX != 0f || accelY != 0f || accelZ != 0f)
                ) {
                    calX = accelX
                    calY = accelY
                    calZ = accelZ
                    isCalibrated = true
                }
            }

            // Efek 3D dari sumbu Z (relatif terhadap posisi kalibrasi)
            val zScale = if (isCalibrated) {
                (1f + ((accelZ - calZ) / 18f)).coerceIn(0.65f, 1.45f)
            } else {
                1f
            }
            val currentBallRadiusPx = baseBallRadiusPx * zScale

            // PENTING: nilai terbaru untuk dibaca di dalam coroutine loop fisika
            // (tanpa ini, loop hanya memakai nilai sensor pertama sehingga bola diam / menempel di pojok)
            val latestAccelX by rememberUpdatedState(accelX)
            val latestAccelY by rememberUpdatedState(accelY)
            val latestRadius by rememberUpdatedState(currentBallRadiusPx)

            val resetBall = {
                if ((canvasWidthPx > 0f) && (canvasHeightPx > 0f)) {
                    ballX = canvasWidthPx / 2f
                    ballY = canvasHeightPx / 2f
                    vx = 0f
                    vy = 0f
                }
            }

            val calibrate = {
                calX = accelX
                calY = accelY
                calZ = accelZ
                isCalibrated = true
                resetBall()
            }

            // Loop fisika: bola bergerak mengikuti kemiringan HP
            LaunchedEffect(canvasWidthPx, canvasHeightPx) {
                if (canvasWidthPx <= 0f || canvasHeightPx <= 0f) return@LaunchedEffect

                if (ballX == 0f && ballY == 0f) {
                    ballX = canvasWidthPx / 2f
                    ballY = canvasHeightPx / 2f
                }

                var lastTime = 0L
                while (true) {
                    withFrameNanos { frameTime ->
                        if (lastTime == 0L) {
                            lastTime = frameTime
                            return@withFrameNanos
                        }
                        val dt = ((frameTime - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                        lastTime = frameTime

                        // Percepatan bola (px/s²) per 1 m/s² kemiringan, disesuaikan densitas layar
                        val accelScale = density.density * 220f

                        // Gaya hanya dihitung setelah titik netral tersedia
                        var forceX = 0f
                        var forceY = 0f
                        if (isCalibrated) {
                            // Miring kanan -> accelX turun -> bola ke kanan
                            forceX = -(latestAccelX - calX) * accelScale
                            // Miring ke belakang (accelY naik) -> bola turun
                            forceY = (latestAccelY - calY) * accelScale
                        }

                        // Inersia + redaman (tidak bergantung frame rate)
                        val damping = exp(-2.5f * dt)
                        vx = (vx + forceX * dt) * damping
                        vy = (vy + forceY * dt) * damping

                        var nextX = ballX + vx * dt
                        var nextY = ballY + vy * dt

                        val r = latestRadius
                        val minX = r
                        val maxX = canvasWidthPx - r
                        val minY = r
                        val maxY = canvasHeightPx - r
                        val bounce = 0.4f

                        if (nextX < minX) {
                            nextX = minX
                            vx = -vx * bounce
                        } else if (nextX > maxX) {
                            nextX = maxX
                            vx = -vx * bounce
                        }
                        if (nextY < minY) {
                            nextY = minY
                            vy = -vy * bounce
                        } else if (nextY > maxY) {
                            nextY = maxY
                            vy = -vy * bounce
                        }

                        ballX = nextX
                        ballY = nextY
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(animatedBgColor)
                    .border(1.5.dp, animatedTextColor.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val r = latestRadius
                            ballX = (ballX + dragAmount.x).coerceIn(r, (canvasWidthPx - r).coerceAtLeast(r))
                            ballY = (ballY + dragAmount.y).coerceIn(r, (canvasHeightPx - r).coerceAtLeast(r))
                            vx = dragAmount.x * 15f
                            vy = dragAmount.y * 15f
                        }
                    }
            ) {
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }

                LaunchedEffect(widthPx, heightPx) {
                    canvasWidthPx = widthPx
                    canvasHeightPx = heightPx
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = animatedTextColor.copy(alpha = 0.08f)
                    val axisColor = animatedTextColor.copy(alpha = 0.2f)

                    // Crosshair Axes
                    drawLine(
                        color = axisColor,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                    drawLine(
                        color = axisColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    // Concentric Reference Ring
                    drawCircle(
                        color = gridColor,
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width / 3.5f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                    )

                    // Vektor arah gaya (X & Y)
                    if (ballX > 0f && ballY > 0f && isAccelerometerAvailable && isCalibrated) {
                        val forceVector = Offset(-(accelX - calX) * 12f, (accelY - calY) * 12f)
                        drawLine(
                            color = animatedBallColor.copy(alpha = 0.6f),
                            start = Offset(ballX, ballY),
                            end = Offset(ballX + forceVector.x, ballY + forceVector.y),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Bola 3D dengan glow & skala dari sumbu Z
                    if (ballX > 0f && ballY > 0f) {
                        drawCircle(
                            color = animatedBallColor.copy(alpha = 0.25f),
                            center = Offset(ballX, ballY),
                            radius = currentBallRadiusPx * 1.4f
                        )
                        drawCircle(
                            color = animatedBallColor,
                            center = Offset(ballX, ballY),
                            radius = currentBallRadiusPx
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            center = Offset(ballX - currentBallRadiusPx * 0.35f, ballY - currentBallRadiusPx * 0.35f),
                            radius = currentBallRadiusPx * 0.35f
                        )
                    }
                }

                if (!isAccelerometerAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Akselerometer Tidak Tersedia\nAnda tetap dapat menggeser bola dengan sentuhan jari.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Action Bar (Posisi, Skala Z, Tombol Kalibrasi & Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format(Locale.getDefault(), "Posisi: (%.0f, %.0f) px", ballX, ballY),
                        fontSize = 11.sp,
                        color = animatedTextColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "Skala Z: %.2fx", zScale),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedTextColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { calibrate() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = animatedTextColor.copy(alpha = 0.15f),
                            contentColor = animatedTextColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Kalibrasi",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Kalibrasi", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { resetBall() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = animatedTextColor.copy(alpha = 0.15f),
                            contentColor = animatedTextColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reset", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EducationalInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "💡 Konsep Pembelajaran Sensor",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "1. Accelerometer (Motion Category): Mengukur percepatan gravitasi & gerak fisik HP dalam 3 sumbu (X, Y, Z).\n" +
                        "2. Light Sensor (Environment Category): Mengukur derajat pencahayaan (lux). Nilai dan ambang respons dapat bervariasi tergantung hardware sensor perangkat.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SensorPlaygroundPreview() {
    Tugas_SensorTheme {
        SensorPlaygroundScreen(
            isAccelerometerAvailable = true,
            isLightSensorAvailable = true,
            accelX = 1.2f,
            accelY = -3.4f,
            accelZ = 9.8f,
            lightLux = 150f
        )
    }
}