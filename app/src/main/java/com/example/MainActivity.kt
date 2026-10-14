package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AirQualityRepository
import com.example.data.local.AppDatabase
import com.example.ui.AirQualityViewModel
import com.example.ui.AirQualityViewModelFactory
import com.example.ui.navigation.NavScreen
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DatasetScreen
import com.example.ui.screens.ModelEvaluationScreen
import com.example.ui.screens.ModelTrainingScreen
import com.example.ui.screens.PredictionScreen
import com.example.ui.screens.PreprocessingScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RiskAQIScreen
import com.example.ui.screens.SensorMonitoringScreen
import com.example.ui.theme.AeroCastTheme
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {

    private val viewModel: AirQualityViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repo = AirQualityRepository(database.airQualityDao())
        AirQualityViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AeroCastTheme {
                MainAppContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: AirQualityViewModel) {
    var currentScreen by remember { mutableStateOf(NavScreen.DASHBOARD) }
    var showMenu by remember { mutableStateOf(false) }

    val notification by viewModel.notification.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()

    // Handle back button to pop to DASHBOARD
    BackHandler(enabled = currentScreen != NavScreen.DASHBOARD) {
        currentScreen = NavScreen.DASHBOARD
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceElevated,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "AeroCast Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AeroCast AI",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = currentScreen.title,
                                color = CyanSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // Alerts indicator shortcut
                    if (activeAlerts.isNotEmpty()) {
                        IconButton(onClick = { currentScreen = NavScreen.ALERTS }) {
                            Box {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = "Alerts",
                                    tint = RedDanger
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(RedDanger, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Overflow Menu for additional academic modules
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = TextSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Evaluation & Leaderboard", color = TextPrimary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    currentScreen = NavScreen.EVALUATION
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("CPCB / EPA AQI Standards", color = TextPrimary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.CloudQueue, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    currentScreen = NavScreen.RISK_AQI
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("IoT Sensors Integration", color = TextPrimary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Sensors, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    currentScreen = NavScreen.SENSOR
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Alerts Management", color = TextPrimary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = RedDanger, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    currentScreen = NavScreen.ALERTS
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Academic Project & Docs", color = TextPrimary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    currentScreen = NavScreen.REPORTS
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 6.dp
            ) {
                val primaryNavItems = listOf(
                    NavScreen.DASHBOARD,
                    NavScreen.DATASET,
                    NavScreen.PREPROCESSING,
                    NavScreen.MODELS,
                    NavScreen.PREDICTION
                )

                primaryNavItems.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.18f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Routing
            when (currentScreen) {
                NavScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.DATASET -> DatasetScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.PREPROCESSING -> PreprocessingScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.MODELS -> ModelTrainingScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.EVALUATION -> ModelEvaluationScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.PREDICTION -> PredictionScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.RISK_AQI -> RiskAQIScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.SENSOR -> SensorMonitoringScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.ALERTS -> AlertsScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
                NavScreen.REPORTS -> ReportsScreen(viewModel = viewModel, onNavigate = { currentScreen = it })
            }

            // In-App Notification Toast
            AnimatedVisibility(
                visible = notification != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                if (notification != null) {
                    val notif = notification!!
                    Surface(
                        color = if (notif.isError) RedDanger else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (notif.isError) RedDanger else EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearNotification() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = notif.message,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "DISMISS",
                                color = if (notif.isError) TextPrimary else CyanSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
