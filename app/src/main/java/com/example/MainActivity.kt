package com.example

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TrackerViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MainContentScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainContentScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600
    val selectionMode = remember { mutableStateOf("pickup") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Welcoming top branding bar
        TopActionRow(
            currentRole = currentRole,
            onRoleChanged = { role -> viewModel.currentRole.value = role },
            onClearHistory = { viewModel.clearHistory() }
        )

        // Bento-themed Mode Switcher segment
        RoleSelectorBar(
            currentRole = currentRole,
            onRoleChanged = { role -> viewModel.currentRole.value = role }
        )

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left interactive panel (User options / Driver dashboard)
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .fillMaxHeight()
                ) {
                    if (currentRole == UserRole.PASSENGER) {
                        PassengerPanel(viewModel = viewModel, selectionMode = selectionMode)
                    } else {
                        DriverPanel(viewModel = viewModel)
                    }
                }
                
                // Right map panel
                Box(
                    modifier = Modifier
                        .weight(6f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    InteractiveMapView(viewModel = viewModel, selectionMode = selectionMode)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Map bento piece
                Box(
                    modifier = Modifier
                        .weight(4.5f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    InteractiveMapView(viewModel = viewModel, selectionMode = selectionMode)
                }

                // Interaction console panel
                Box(
                    modifier = Modifier
                        .weight(5.5f)
                        .fillMaxWidth()
                ) {
                    if (currentRole == UserRole.PASSENGER) {
                        PassengerPanel(viewModel = viewModel, selectionMode = selectionMode)
                    } else {
                        DriverPanel(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TopActionRow(
    currentRole: UserRole,
    onRoleChanged: (UserRole) -> Unit,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Welcome back, guest",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Yandex-Link",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        IconButton(
            onClick = onClearHistory,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Logs",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RoleSelectorBar(
    currentRole: UserRole,
    onRoleChanged: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val focusMode = currentRole == UserRole.PASSENGER

        Button(
            onClick = { onRoleChanged(UserRole.PASSENGER) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (focusMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (focusMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("user_mode_button"),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("User Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = { onRoleChanged(UserRole.DRIVER) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!focusMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (!focusMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("driver_mode_button"),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Driver Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InteractiveMapView(
    viewModel: TrackerViewModel,
    selectionMode: MutableState<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pickupLat by viewModel.pickupLat.collectAsStateWithLifecycle()
    val pickupLng by viewModel.pickupLng.collectAsStateWithLifecycle()
    val dropoffLat by viewModel.dropoffLat.collectAsStateWithLifecycle()
    val dropoffLng by viewModel.dropoffLng.collectAsStateWithLifecycle()

    val driverOnline by viewModel.driverOnline.collectAsStateWithLifecycle()
    val driverLat by viewModel.driverLat.collectAsStateWithLifecycle()
    val driverLng by viewModel.driverLng.collectAsStateWithLifecycle()

    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()
    val routePoints by viewModel.simulationRoute.collectAsStateWithLifecycle()

    val density = LocalDensity.current.density

    // Bounds setting
    val minLat = MapHelper.CENTER_LAT - MapHelper.LAT_SPAN * 0.5
    val maxLat = MapHelper.CENTER_LAT + MapHelper.LAT_SPAN * 0.5
    val minLng = MapHelper.CENTER_LNG - MapHelper.LNG_SPAN * 0.5
    val maxLng = MapHelper.CENTER_LNG + MapHelper.LNG_SPAN * 0.5

    fun toCanvasOffset(lat: Double, lng: Double, w: Float, h: Float): Offset {
        val xF = (lng - minLng) / (maxLng - minLng)
        val yF = 1.0 - (lat - minLat) / (maxLat - minLat)
        return Offset((xF * w).toFloat(), (yF * h).toFloat())
    }

    val traffic by viewModel.currentTrafficLevel.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEADDFF).copy(alpha = 0.05f)) // M3 lavender background backing
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Adaptive themed colors for the Map canvas
        val isLightTheme = MaterialTheme.colorScheme.primary == BentoPrimaryLight
        val groundColor = if (isLightTheme) Color(0xFFE6E0E9) else Color(0xFF1B1A21)
        val streetBaseColor = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF2B2E3C)
        val gridColor = if (isLightTheme) Color(0xFFFFFFFF).copy(alpha = 0.5f) else Color(0xFF24222C)
        val riverColor = if (isLightTheme) Color(0xFFC5D3ED) else Color(0xFF263C60).copy(alpha = 0.6f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(groundColor)
                .pointerInput(selectionMode.value) {
                    detectTapGestures { offset ->
                        val xF = offset.x / widthPx
                        val yF = 1f - (offset.y / heightPx)
                        val tapLat = minLat + yF * (maxLat - minLat)
                        val tapLng = minLng + xF * (maxLng - minLng)

                        if (selectionMode.value == "pickup") {
                            viewModel.setPickup(tapLat, tapLng)
                        } else {
                            viewModel.setDropoff(tapLat, tapLng)
                        }
                    }
                }
        ) {
            // Draw grid lines
            for (i in 0..8) {
                val x = (i.toFloat() / 8) * size.width
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.5f)
                val y = (i.toFloat() / 8) * size.height
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.5f)
            }

            // Draw Moskva River representing geographic curve
            val riverLinePoints = listOf(
                55.7031 to 37.5112,
                55.7158 to 37.5436,
                55.7294 to 37.5915,
                55.7414 to 37.6208,
                55.7539 to 37.6450,
                55.7600 to 37.6800
            )
            val riverPath = Path()
            val firstRiverOffset = toCanvasOffset(riverLinePoints[0].first, riverLinePoints[0].second, size.width, size.height)
            riverPath.moveTo(firstRiverOffset.x, firstRiverOffset.y)
            for (i in 1 until riverLinePoints.size) {
                val pt = toCanvasOffset(riverLinePoints[i].first, riverLinePoints[i].second, size.width, size.height)
                riverPath.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = riverPath,
                color = riverColor,
                style = Stroke(width = 36f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw Streets
            MapHelper.streets.forEach { street ->
                if (street.points.isNotEmpty()) {
                    val path = Path()
                    val pF = toCanvasOffset(street.points[0].first, street.points[0].second, size.width, size.height)
                    path.moveTo(pF.x, pF.y)
                    for (j in 1 until street.points.size) {
                        val p = toCanvasOffset(street.points[j].first, street.points[j].second, size.width, size.height)
                        path.lineTo(p.x, p.y)
                    }

                    val streetColor = when (street.trafficLevel) {
                        TrafficLevel.LOW -> Color(0xFF10B981)
                        TrafficLevel.MEDIUM -> Color(0xFFFBBF24)
                        TrafficLevel.HIGH -> Color(0xFFEF4444)
                    }
                    drawPath(path, streetBaseColor, style = Stroke(12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, streetColor.copy(alpha = 0.7f), style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // Draw active routing dashed line
            if (routePoints.isNotEmpty()) {
                val routePath = Path()
                val rF = toCanvasOffset(routePoints[0].first, routePoints[0].second, size.width, size.height)
                routePath.moveTo(rF.x, rF.y)
                for (idx in 1 until routePoints.size) {
                    val rp = toCanvasOffset(routePoints[idx].first, routePoints[idx].second, size.width, size.height)
                    routePath.lineTo(rp.x, rp.y)
                }
                drawPath(
                    path = routePath,
                    color = Color(0xFFA78BFA),
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                )
            }
        }

        // Standard landmarks mapping labels
        MapHelper.landmarks.forEach { l ->
            val pos = toCanvasOffset(l.lat, l.lng, widthPx, heightPx)
            Box(
                modifier = Modifier
                    .offset(
                        x = (pos.x / density - 12).dp,
                        y = (pos.y / density - 12).dp
                    )
                    .shadow(3.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                    .size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = l.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp)
                )
                // Label overlay
                Box(
                    modifier = Modifier
                        .offset(y = (-18).dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = l.name.substringBefore(" "),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Green Pickup Pin (A)
        val posA = toCanvasOffset(pickupLat, pickupLng, widthPx, heightPx)
        Box(
            modifier = Modifier
                .offset(x = (posA.x / density - 15).dp, y = (posA.y / density - 32).dp)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .background(Color(0xFF10B981), RoundedCornerShape(10.dp))
                .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("A", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }

        // Red Destination Pin (B)
        val posB = toCanvasOffset(dropoffLat, dropoffLng, widthPx, heightPx)
        Box(
            modifier = Modifier
                .offset(x = (posB.x / density - 15).dp, y = (posB.y / density - 32).dp)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .background(Color(0xFFEF4444), RoundedCornerShape(10.dp))
                .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("B", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }

        // Driver moving vehicle (Yellow arrow indicator)
        if (driverOnline) {
            val dPos = toCanvasOffset(driverLat, driverLng, widthPx, heightPx)
            Box(
                modifier = Modifier
                    .offset(x = (dPos.x / density - 14).dp, y = (dPos.y / density - 14).dp)
                    .shadow(6.dp, CircleShape)
                    .background(Color(0xFFFBBF24), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Driver",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // GLASS-ESQUE LIVE TRAFFIC BADGE (Top-Left, matching HTML bento aesthetics!)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when (traffic) {
                                TrafficLevel.LOW -> Color(0xFF10B981)
                                TrafficLevel.MEDIUM -> Color(0xFFFBBF24)
                                TrafficLevel.HIGH -> Color(0xFFEF4444)
                            },
                            CircleShape
                        )
                )
                Text(
                    text = "Live Traffic: ${traffic.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // FLOATING ACTION MY_LOCATION BUTTON (Bottom-Right, matching HTML bento aesthetics!)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .size(44.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    // Center onto city center (Red Square)
                    viewModel.setPickup(MapHelper.CENTER_LAT, MapHelper.CENTER_LNG)
                    Toast.makeText(context, "Centered to Red Square", Toast.LENGTH_SHORT).show()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Center Location",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // INTERACTIVE HUD SELECTOR OVERLAY (Bottom-Left)
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable { selectionMode.value = if (selectionMode.value == "pickup") "dropoff" else "pickup" }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (selectionMode.value == "pickup") Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectionMode.value == "pickup") "Set Pickup (A)" else "Set Destination (B)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PassengerPanel(
    viewModel: TrackerViewModel,
    selectionMode: MutableState<String>
) {
    val pickupName by viewModel.pickupName.collectAsStateWithLifecycle()
    val dropoffName by viewModel.dropoffName.collectAsStateWithLifecycle()
    val pickupLat by viewModel.pickupLat.collectAsStateWithLifecycle()
    val pickupLng by viewModel.pickupLng.collectAsStateWithLifecycle()
    val dropoffLat by viewModel.dropoffLat.collectAsStateWithLifecycle()
    val dropoffLng by viewModel.dropoffLng.collectAsStateWithLifecycle()

    val carType by viewModel.selectedCarType.collectAsStateWithLifecycle()
    val traffic by viewModel.currentTrafficLevel.collectAsStateWithLifecycle()
    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()
    val loggedOrders by viewModel.orders.collectAsStateWithLifecycle()
    val navigationText by viewModel.navigationText.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val dist = MapHelper.getDistanceKm(pickupLat, pickupLng, dropoffLat, dropoffLng)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // LANDMARKS QUICK-SELECT BENTO CAPSULES
        item {
            Column {
                Text(
                    text = "SELECT PIN CONVENIENT SPOTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(MapHelper.landmarks[0], MapHelper.landmarks[1], MapHelper.landmarks[2]).forEach { l ->
                        val selected = (selectionMode.value == "pickup" && pickupLat == l.lat && pickupLng == l.lng) ||
                                       (selectionMode.value == "dropoff" && dropoffLat == l.lat && dropoffLng == l.lng)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (selectionMode.value == "pickup") viewModel.setPickup(l.lat, l.lng)
                                    else viewModel.setDropoff(l.lat, l.lng)
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = l.name.substringBefore(" "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ACTIVE TASK RIDE INFO
        if (activeOrder != null) {
            val order = activeOrder!!
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "ACTIVE ORDER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "STATUS: ${order.status}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${order.fare.toInt()} ₽",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "Driver info: $navigationText",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { MapHelper.launchYandexMaps(context, order.pickupLat, order.pickupLng, order.dropoffLat, order.dropoffLng) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Map Route", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { MapHelper.launchYandexNavigator(context, order.dropoffLat, order.dropoffLng) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Yandex Navi", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { viewModel.cancelOrder() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        } else {
            // SQUARED AVG WAIT & EST COST SIDE-BY-SIDE BENTO CARDS
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val waitTime = when (traffic) {
                        TrafficLevel.LOW -> "3 min"
                        TrafficLevel.MEDIUM -> "6 min"
                        TrafficLevel.HIGH -> "12 min"
                    }

                    // Card 1: Avg Wait
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "AVG. WAIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = waitTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Card 2: Est Fare
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "EST. FARE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                            val farePrice = MapHelper.calculateFare(dist, carType, traffic)
                            Text(
                                text = "${farePrice.toInt()} ₽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ROUTE ADDRESS SELECTION PANEL & BOOKING TRIGGER
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Row A (Pickup Address)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectionMode.value = "pickup" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val labelColor = if (selectionMode.value == "pickup") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                Text(
                                    text = "FROM (A) - PICKUP LOCATION",
                                    fontSize = 8.5.sp,
                                    color = labelColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pickupName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Row B (Destination Address)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectionMode.value = "dropoff" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val labelColor = if (selectionMode.value == "dropoff") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                Text(
                                    text = "TO (B) - DESTINATION SPOTS",
                                    fontSize = 8.5.sp,
                                    color = labelColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dropoffName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Unified Yandex book button inside card
                        val totalFare = MapHelper.calculateFare(dist, carType, traffic)
                        Button(
                            onClick = { viewModel.createOrder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("create_order_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "BOOK RIDE • ${totalFare.toInt()} ₽",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // TRAFFIC SIMULATING CONTROLLER BENTO CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SIMULATE CITY TRAFFIC DENSITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TrafficLevel.values().forEach { lvl ->
                                val selected = traffic == lvl
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable { viewModel.currentTrafficLevel.value = lvl }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lvl.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CAR TIERS SELECTOR ROW
            item {
                Text(
                    text = "SELECT VEHICLE COMFORT TIER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Economy", "Comfort", "Business").forEach { tier ->
                        val selected = carType == tier
                        val estF = MapHelper.calculateFare(dist, tier, traffic)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectedCarType.value = tier }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tier,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${estF.toInt()} ₽",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // COHESIVE HISTORIC ROOM DB RUN CATALOG IN BENTO
        if (loggedOrders.isNotEmpty()) {
            item {
                Text(
                    text = "STORED LOGS IN LOCAL ARCHIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(loggedOrders) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "#${log.id}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${log.carType} • ${log.status}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${log.pickupAddress.take(16)}.. → ${log.dropoffAddress.take(16)}..",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${log.fare.toInt()} ₽",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverPanel(viewModel: TrackerViewModel) {
    val driverOnline by viewModel.driverOnline.collectAsStateWithLifecycle()
    val pendingList by viewModel.pendingOrders.collectAsStateWithLifecycle()
    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()
    val navigationText by viewModel.navigationText.collectAsStateWithLifecycle()
    val driverLat by viewModel.driverLat.collectAsStateWithLifecycle()
    val driverLng by viewModel.driverLng.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ONLINE SWITCH HIGH-RADII BENTO PAD
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (driverOnline) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.3.dp,
                        color = if (driverOnline) Color(0xFF81C784).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (driverOnline) Color(0xFF1B5E20) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (driverOnline) "DRIVER SYSTEM ACTIVE" else "DRIVER SYSTEM INACTIVE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (driverOnline) Color(0xFF2E7D32) else Color.DarkGray
                        )
                    }
                    Switch(
                        checked = driverOnline,
                        onCheckedChange = { viewModel.toggleDriverOnline() },
                        modifier = Modifier.testTag("driver_online_switch")
                    )
                }
            }
        }

        if (!driverOnline) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You are currently offline.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toggle your status on to discover incoming live simulated dispatch requests.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else if (activeOrder != null) {
            val order = activeOrder!!
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "ACTIVE DISPATCH TASK", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "${order.fare.toInt()} ₽", 
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "From: ${order.pickupAddress}\nTo: ${order.dropoffAddress}", 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "GPS: $navigationText", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        when(order.status) {
                            "ACCEPTED" -> {
                                Button(
                                    onClick = { viewModel.markArrivedAtPickup() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("driver_arrived_pickup_btn")
                                ) {
                                    Text("ARRIVED AT PICKUP", fontWeight = FontWeight.Bold)
                                }
                            }
                            "ARRIVED" -> {
                                Button(
                                    onClick = { viewModel.startJourney() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("driver_start_journey_btn")
                                ) {
                                    Text("PASSENGER BOARDED • START", fontWeight = FontWeight.Bold)
                                }
                            }
                            "IN_PROGRESS" -> {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { MapHelper.launchYandexNavigator(context, order.dropoffLat, order.dropoffLng) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Navigator Quick Link", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { viewModel.cancelOrder() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("REJECT DISPATCH TASK", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "INCOMING ASSIGNMENT QUEUE", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            if (pendingList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    ) {
                        Text(
                            text = "No live tasks active. Switch to User Mode, select spots and press 'BOOK RIDE' to see live distribution!",
                            fontSize = 11.sp, 
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp).fillMaxWidth()
                        )
                    }
                }
            } else {
                items(pendingList) { pending ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable { viewModel.acceptOrder(pending) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${pending.pickupAddress.take(24)}.. → ${pending.dropoffAddress.take(24)}..", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Class: ${pending.carType}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${pending.fare.toInt()} ₽",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = { viewModel.acceptOrder(pending) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("accept_order_${pending.id}_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
