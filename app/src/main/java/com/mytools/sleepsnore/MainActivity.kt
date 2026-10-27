package com.mytools.sleepsnore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mytools.sleepsnore.audio.AlarmManagerHelper
import com.mytools.sleepsnore.audio.SoundscapeManager
import com.mytools.sleepsnore.data.db.SleepDatabase
import com.mytools.sleepsnore.service.SleepTrackingService
import com.mytools.sleepsnore.ui.screens.*
import com.mytools.sleepsnore.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("snore_settings", android.content.Context.MODE_PRIVATE)

        setContent {
            var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "dark") ?: "dark") }

            DisposableEffect(Unit) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                    if (key == "theme_mode") {
                        themeMode = sp.getString("theme_mode", "dark") ?: "dark"
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            SleepSnoreTheme(themeMode = themeMode) {
                MainAppScaffold()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainAppScaffold() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val sleepDao = remember { SleepDatabase.getInstance(context).sleepDao() }
    val soundscapeManager = remember { SoundscapeManager(context) }
    val alarmHelper = remember { AlarmManagerHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            soundscapeManager.release()
            alarmHelper.release()
        }
    }

    var selectedFactors by remember { mutableStateOf(listOf("alcohol")) }
    var selectedRemedies by remember { mutableStateOf(listOf("side_sleeping", "nasal_strip")) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val liveState by SleepTrackingService.liveState.collectAsState()

    val navItems = listOf(
        BottomNavItem("record", "监测", Icons.Default.Bedtime),
        BottomNavItem("results", "结果", Icons.Default.Speed),
        BottomNavItem("trends", "趋势", Icons.Default.ShowChart),
        BottomNavItem("recordings", "录音", Icons.Default.GraphicEq),
        BottomNavItem("settings", "设置", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = StandardBackground,
        bottomBar = {
            // 在夜视监测中隐藏底部导航，给予纯黑沉浸防刺眼体验
            if (!liveState.isTracking) {
                NavigationBar(
                    containerColor = LighterBackground,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route ||
                                (item.route == "results" && currentRoute?.startsWith("result") == true)

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryAccent,
                                indicatorColor = PrimaryAccent.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                selectedTextColor = PrimaryAccent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "record",
            modifier = Modifier
                .fillMaxSize()
                .padding(if (liveState.isTracking) PaddingValues(0.dp) else innerPadding)
        ) {
            // Tab 1: 监测
            composable("record") {
                RecordScreen(
                    soundscapeManager = soundscapeManager,
                    alarmHelper = alarmHelper,
                    selectedFactors = selectedFactors,
                    onFactorsChanged = { selectedFactors = it },
                    selectedRemedies = selectedRemedies,
                    onRemediesChanged = { selectedRemedies = it },
                    onNavigateToResult = { id ->
                        navController.navigate("result/$id")
                    }
                )
            }

            // Tab 2: 结果详情 (默认显示最新夜晚报告)
            composable("results") {
                ResultScreen(
                    sessionId = -1L,
                    onNavigateToRecord = { navController.navigate("record") }
                )
            }

            // 具体某夜晚结果页
            composable(
                route = "result/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val id = backStack.arguments?.getLong("sessionId") ?: -1L
                ResultScreen(
                    sessionId = id,
                    onBack = { navController.popBackStack() },
                    onNavigateToRecord = { navController.navigate("record") }
                )
            }

            // Tab 3: 趋势与对比 (日历 / 走势 / 对策对比)
            composable("trends") {
                TrendsScreen(
                    sleepDao = sleepDao,
                    onNavigateToResult = { id ->
                        navController.navigate("result/$id")
                    }
                )
            }

            // Tab 4: 录音片库
            composable("recordings") {
                RecordingsScreen(
                    sleepDao = sleepDao
                )
            }

            // Tab 5: 洞察与设置 (身体档案 BMI / 存储 / 麦克风 / FAQ)
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}
