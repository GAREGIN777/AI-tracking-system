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
            MyApplicationTheme(darkTheme = false) {
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
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600
    val selectionMode = remember { mutableStateOf("pickup") }
 
    val localizedContext = remember(currentLang) {
        val locale = java.util.Locale(currentLang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
 
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Welcoming top branding bar
        TopActionRow(
            currentLanguage = currentLang,
            onLanguageCodeChanged = { viewModel.setLanguage(it) },
            currentRole = currentRole,
            onRoleChanged = { role -> viewModel.currentRole.value = role },
            onClearHistory = { viewModel.clearHistory() },
            currentUser = currentUser,
            onLogout = { viewModel.logout() },
            localizedContext = localizedContext
        )
 
        if (currentUser == null) {
            RegistrationScreen(viewModel = viewModel, localizedContext = localizedContext, currentLang = currentLang)
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
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

            val traffic by viewModel.currentTrafficLevel.collectAsStateWithLifecycle()
            SandboxConsole(
                currentUser = currentUser,
                currentRole = currentRole,
                onRoleChanged = { role -> viewModel.currentRole.value = role },
                traffic = traffic,
                onTrafficChanged = { lvl -> viewModel.currentTrafficLevel.value = lvl },
                localizedContext = localizedContext
            )
        }
    }
}

@Composable
fun TopActionRow(
    currentLanguage: String,
    onLanguageCodeChanged: (String) -> Unit,
    currentRole: UserRole,
    onRoleChanged: (UserRole) -> Unit,
    onClearHistory: () -> Unit,
    currentUser: com.example.data.UserProfile?,
    onLogout: () -> Unit,
    localizedContext: android.content.Context
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
                    text = if (currentUser != null) "${currentUser.role}: ${currentUser.phone}" else localizedContext.getString(R.string.welcome_back),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = currentUser?.name ?: "Yerevan Navigator",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            var expanded by remember { mutableStateOf(false) }
            val langLabel = when (currentLanguage) {
                "hy" -> "🇦🇲 HY"
                "ru" -> "🇷🇺 RU"
                else -> "🇺🇸 EN"
            }

            Box(modifier = Modifier.wrapContentSize()) {
                Button(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(text = langLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Language",
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("English 🇺🇸", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            onLanguageCodeChanged("en")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Русский 🇷🇺", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            onLanguageCodeChanged("ru")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Հայերեն 🇦🇲", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            onLanguageCodeChanged("hy")
                            expanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onClearHistory,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Logs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (currentUser != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectorBar(
    currentRole: UserRole,
    onRoleChanged: (UserRole) -> Unit,
    localizedContext: android.content.Context,
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
                Text(localizedContext.getString(R.string.user_mode), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                Text(localizedContext.getString(R.string.driver_mode), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val localizedContext = remember(currentLang) {
        val locale = java.util.Locale(currentLang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    val pickupLat by viewModel.pickupLat.collectAsStateWithLifecycle()
    val pickupLng by viewModel.pickupLng.collectAsStateWithLifecycle()
    val dropoffLat by viewModel.dropoffLat.collectAsStateWithLifecycle()
    val dropoffLng by viewModel.dropoffLng.collectAsStateWithLifecycle()

    val driverOnline by viewModel.driverOnline.collectAsStateWithLifecycle()
    val driverLat by viewModel.driverLat.collectAsStateWithLifecycle()
    val driverLng by viewModel.driverLng.collectAsStateWithLifecycle()

    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()
    val routePoints by viewModel.simulationRoute.collectAsStateWithLifecycle()
    val selectedCarType by viewModel.selectedCarType.collectAsStateWithLifecycle()

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
        val groundColor = if (isLightTheme) Color(0xFFF1F3F4) else Color(0xFF1B1A21)
        val streetBaseColor = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF2B2E3C)
        val gridColor = if (isLightTheme) Color(0xFFE2E4E7).copy(alpha = 0.4f) else Color(0xFF24222C)
        val riverColor = if (isLightTheme) Color(0xFFADD8E6) else Color(0xFF263C60).copy(alpha = 0.6f)
        val parkColor = if (isLightTheme) Color(0xFFC8E6C9).copy(alpha = 0.6f) else Color(0xFF1E3322).copy(alpha = 0.4f)

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

            // Draw English Park
            val englishParkPos = toCanvasOffset(40.1741, 44.5074, size.width, size.height)
            drawCircle(parkColor, radius = 40f * density, center = englishParkPos)

            // Draw Victory Park (includes lake and greenery)
            val victoryParkPos = toCanvasOffset(40.1960, 44.5230, size.width, size.height)
            drawCircle(parkColor, radius = 55f * density, center = victoryParkPos)

            // Draw Lovers' Park
            val loversParkPos = toCanvasOffset(40.1920, 44.5035, size.width, size.height)
            drawCircle(parkColor, radius = 30f * density, center = loversParkPos)

            // Draw Hrazdan River representing geographic curve of Yerevan
            val riverLinePoints = listOf(
                40.2180 to 44.4550,
                40.2030 to 44.4680,
                40.1880 to 44.4750,
                40.1780 to 44.4720,
                40.1690 to 44.4710,
                40.1550 to 44.4820
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
            val localizedName = when (currentLang) {
                "hy" -> l.nameHy
                "ru" -> l.nameRu
                else -> l.nameEn
            }
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
                    contentDescription = localizedName,
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
                        text = localizedName.substringBefore(" "),
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

        // Driver moving vehicle (Class-specific custom visual tracking indicator)
        if (driverOnline) {
            val dPos = toCanvasOffset(driverLat, driverLng, widthPx, heightPx)
            val (carBg, iconHex, carClassLabel) = when (selectedCarType) {
                "Economy" -> Triple(Color(0xFF10B981), Color.White, "Eco 🍀")
                "Comfort" -> Triple(Color(0xFF3B82F6), Color.White, "Comfort 🚗")
                "Business" -> Triple(Color(0xFF8B5CF6), Color.White, "Biz 💎")
                "Cargo" -> Triple(Color(0xFFF59E0B), Color.Black, "Cargo 🚚")
                else -> Triple(Color(0xFFFBBF24), Color.Black, "Car")
            }

            Box(
                modifier = Modifier
                    .offset(x = (dPos.x / density - 35).dp, y = (dPos.y / density - 16).dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(carBg, RoundedCornerShape(12.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (selectedCarType) {
                            "Economy" -> Icons.Default.ThumbUp
                            "Comfort" -> Icons.Default.Star
                            "Business" -> Icons.Default.Favorite
                            "Cargo" -> Icons.Default.ShoppingCart
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = "Car Class Icon",
                        tint = iconHex,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = carClassLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconHex
                    )
                }
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
                    text = localizedContext.getString(R.string.live_traffic, traffic.name.lowercase().replaceFirstChar { it.uppercase() }),
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
                    // Center onto city center (Red/Republic Square)
                    viewModel.setPickup(MapHelper.CENTER_LAT, MapHelper.CENTER_LNG)
                    Toast.makeText(localizedContext, localizedContext.getString(R.string.centered_to_red_square), Toast.LENGTH_SHORT).show()
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

    var pickupQuery by remember { mutableStateOf(pickupName) }
    var dropoffQuery by remember { mutableStateOf(dropoffName) }

    LaunchedEffect(pickupName) {
        pickupQuery = pickupName
    }
    LaunchedEffect(dropoffName) {
        dropoffQuery = dropoffName
    }

    val carType by viewModel.selectedCarType.collectAsStateWithLifecycle()
    val traffic by viewModel.currentTrafficLevel.collectAsStateWithLifecycle()
    val activeOrder by viewModel.activeOrder.collectAsStateWithLifecycle()
    val loggedOrders by viewModel.orders.collectAsStateWithLifecycle()
    val navigationText by viewModel.navigationText.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val localizedContext = remember(currentLang) {
        val locale = java.util.Locale(currentLang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    val currencySymbol = if (currentLang == "hy") "֏" else "₽"
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
                    text = localizedContext.getString(R.string.convenient_spots),
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
                        val localizedName = when (currentLang) {
                            "hy" -> l.nameHy
                            "ru" -> l.nameRu
                            else -> l.nameEn
                        }
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
                                text = localizedName.substringBefore(" "),
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
                                    text = localizedContext.getString(R.string.active_order),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = localizedContext.getString(R.string.order_status, order.status),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${order.fare.toInt()} $currencySymbol",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = localizedContext.getString(R.string.driver_info, navigationText),
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
                                Text(localizedContext.getString(R.string.map_route), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { MapHelper.launchYandexNavigator(context, order.dropoffLat, order.dropoffLng) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(localizedContext.getString(R.string.yandex_navi), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                text = localizedContext.getString(R.string.avg_wait),
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
                                text = localizedContext.getString(R.string.est_fare),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                            val farePrice = MapHelper.calculateFare(dist, carType, traffic)
                            Text(
                                text = "${farePrice.toInt()} $currencySymbol",
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
                        // Pickup Address typing field
                        OutlinedTextField(
                            value = pickupQuery,
                            onValueChange = {
                                pickupQuery = it
                                selectionMode.value = "pickup"
                            },
                            label = { Text(localizedContext.getString(R.string.from_pickup), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                            }
                        )

                        // Autocomplete suggestions dynamic block
                        val pickupSuggestions = MapHelper.searchYerevanAddress(pickupQuery)
                        if (selectionMode.value == "pickup" && pickupSuggestions.isNotEmpty() && pickupQuery != pickupName) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    pickupSuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setPickup(suggestion.lat, suggestion.lng)
                                                    pickupQuery = suggestion.getLabel(currentLang)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = suggestion.getLabel(currentLang), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Destination Address typing field
                        OutlinedTextField(
                            value = dropoffQuery,
                            onValueChange = {
                                dropoffQuery = it
                                selectionMode.value = "dropoff"
                            },
                            label = { Text(localizedContext.getString(R.string.to_destination), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                            }
                        )

                        // Autocomplete suggestions dynamic block
                        val dropoffSuggestions = MapHelper.searchYerevanAddress(dropoffQuery)
                        if (selectionMode.value == "dropoff" && dropoffSuggestions.isNotEmpty() && dropoffQuery != dropoffName) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    dropoffSuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setDropoff(suggestion.lat, suggestion.lng)
                                                    dropoffQuery = suggestion.getLabel(currentLang)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(text = suggestion.getLabel(currentLang), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
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
                                text = localizedContext.getString(R.string.book_ride, totalFare.toInt().toString()),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }



            // CAR TIERS SELECTOR ROW
            item {
                Text(
                    text = localizedContext.getString(R.string.vehicle_comfort),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Economy", "Comfort", "Business", "Cargo").forEach { tier ->
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tier,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${estF.toInt()} $currencySymbol",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    maxLines = 1
                                )
                                
                                val badgeText = when (tier) {
                                    "Economy" -> "Low Cost"
                                    "Comfort" -> "Popular"
                                    "Business" -> "Premium"
                                    "Cargo" -> "Cargo 🚚"
                                    else -> ""
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .background(
                                            color = when (tier) {
                                                "Economy" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                "Comfort" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                "Business" -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                                else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (tier) {
                                            "Economy" -> Color(0xFF059669)
                                            "Comfort" -> Color(0xFF2563EB)
                                            "Business" -> Color(0xFF7C3AED)
                                            else -> Color(0xFFD97706)
                                        }
                                    )
                                }
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
                    text = localizedContext.getString(R.string.stored_logs),
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
                            text = "${log.fare.toInt()} $currencySymbol",
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
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val localizedContext = remember(currentLang) {
        val locale = java.util.Locale(currentLang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    val currencySymbol = if (currentLang == "hy") "֏" else "₽"

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
                            text = localizedContext.getString(R.string.system_engine),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (driverOnline) Color(0xFF1B5E20) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (driverOnline) localizedContext.getString(R.string.driver_system_active) else localizedContext.getString(R.string.driver_system_inactive),
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
                            text = localizedContext.getString(R.string.offline_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = localizedContext.getString(R.string.offline_desc),
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
                                text = localizedContext.getString(R.string.active_dispatch), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${order.fare.toInt()} $currencySymbol", 
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
                                    Text(localizedContext.getString(R.string.arrived_at_pickup), fontWeight = FontWeight.Bold)
                                }
                            }
                            "ARRIVED" -> {
                                Button(
                                    onClick = { viewModel.startJourney() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("driver_start_journey_btn")
                                ) {
                                    Text(localizedContext.getString(R.string.start_journey), fontWeight = FontWeight.Bold)
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
                            Text(localizedContext.getString(R.string.navigator_quick_link), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { viewModel.cancelOrder() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(localizedContext.getString(R.string.reject_dispatch), color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = localizedContext.getString(R.string.incoming_assignment), 
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
                            text = localizedContext.getString(R.string.no_live_tasks),
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
                                        text = localizedContext.getString(R.string.class_label, pending.carType),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${pending.fare.toInt()} $currencySymbol",
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
                                    Text(localizedContext.getString(R.string.accept_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    viewModel: TrackerViewModel,
    localizedContext: android.content.Context,
    currentLang: String
) {
    var selectedTab by remember { mutableStateOf("PASSENGER") }
    
    // Form fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    // Driver fields
    var vehicleModel by remember { mutableStateOf("") }
    var vehicleColor by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var vehicleClass by remember { mutableStateOf("Economy") }
    
    var errorText by remember { mutableStateOf("") }
    var classMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = if (currentLang == "hy") "Միացեք Navigator Ցանցին" else if (currentLang == "ru") "Вступить в Сеть Навигатор" else "Join Navigator Network",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == "PASSENGER") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = "PASSENGER" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLang == "hy") "Ուղևոր" else if (currentLang == "ru") "Пассажир" else "Passenger",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "PASSENGER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == "DRIVER") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = "DRIVER" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLang == "hy") "Վարորդ" else if (currentLang == "ru") "Водитель" else "Driver",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "DRIVER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = "" },
                    label = { Text(if (currentLang == "hy") "Անուն" else if (currentLang == "ru") "Имя" else "Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorText = "" },
                    label = { Text(if (currentLang == "hy") "Հեռախոսահամար" else if (currentLang == "ru") "Номер телефона" else "Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = "" },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (selectedTab == "DRIVER") {
                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it; errorText = "" },
                        label = { Text(if (currentLang == "hy") "Մեքենայի մոդել" else if (currentLang == "ru") "Модель машины" else "Car Model") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = licensePlate,
                            onValueChange = { licensePlate = it; errorText = "" },
                            label = { Text(if (currentLang == "hy") "Պետհամարանիշ" else if (currentLang == "ru") "Госномер" else "License Plate") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = vehicleColor,
                            onValueChange = { vehicleColor = it; errorText = "" },
                            label = { Text(if (currentLang == "hy") "Գույն" else if (currentLang == "ru") "Цвет" else "Color") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = vehicleClass,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (currentLang == "hy") "Սակագին" else if (currentLang == "ru") "Класс" else "Service Class") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { classMenuExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = classMenuExpanded,
                            onDismissRequest = { classMenuExpanded = false }
                        ) {
                            listOf("Economy", "Comfort", "Business", "Cargo").forEach { tier ->
                                DropdownMenuItem(
                                    text = { Text(tier) },
                                    onClick = {
                                        vehicleClass = tier
                                        classMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank() || email.isBlank()) {
                            errorText = if (currentLang == "hy") "Խնդրում ենք լրացնել բոլոր դաշտերը" else if (currentLang == "ru") "Пожалуйста, заполните все поля" else "Please fill in all common fields"
                        } else if (selectedTab == "DRIVER" && (vehicleModel.isBlank() || licensePlate.isBlank() || vehicleColor.isBlank())) {
                            errorText = if (currentLang == "hy") "Մեքենայի տվյալները պարտադիր են" else if (currentLang == "ru") "Данные автомобиля обязательны" else "Vehicle details are required for drivers"
                        } else {
                            val profile = com.example.data.UserProfile(
                                name = name,
                                phone = phone,
                                email = email,
                                role = selectedTab,
                                vehicleModel = if (selectedTab == "DRIVER") vehicleModel else null,
                                vehicleColor = if (selectedTab == "DRIVER") vehicleColor else null,
                                licensePlate = if (selectedTab == "DRIVER") licensePlate else null,
                                vehicleClass = if (selectedTab == "DRIVER") vehicleClass else null
                            )
                            viewModel.registerUser(profile)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (currentLang == "hy") "Գրանցվել և Մուտք Գործել" else if (currentLang == "ru") "Зарегистрироваться и войти" else "Register & Enter",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SandboxConsole(
    currentUser: com.example.data.UserProfile?,
    currentRole: UserRole,
    onRoleChanged: (UserRole) -> Unit,
    traffic: TrafficLevel,
    onTrafficChanged: (TrafficLevel) -> Unit,
    localizedContext: android.content.Context
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🛠️ Yandex Sandbox Simulator Controls",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (expanded) "HIDE" else "SHOW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "In production, these tools are removed. Traffic congestion metrics, map routing, and driver dispatch calculations are synchronized in real-time under-the-hood with Yandex MapKit and Yandex Traffic APIs.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Active Registered Role read-only indicator
                Text(
                    text = localizedContext.getString(R.string.active_workspace),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (currentRole == UserRole.PASSENGER) Icons.Default.Person else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentRole == UserRole.PASSENGER) 
                                localizedContext.getString(R.string.user_mode) 
                            else 
                                localizedContext.getString(R.string.driver_mode),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = localizedContext.getString(R.string.locked_mode_desc),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Traffic Jam custom switcher controls
                Text(
                    text = "Simulate Live Traffic/Jams (Alters fares and ETA instantly):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { onTrafficChanged(lvl) }
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
}
