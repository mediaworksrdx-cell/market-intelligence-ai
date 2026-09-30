package com.example.marketintelligence

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.marketintelligence.data.auth.AuthTokenManager
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.ui.academy.AcademyScreen
import com.example.marketintelligence.ui.analysis.AnalysisScreen
import com.example.marketintelligence.ui.fno.FnoScreen
import com.example.marketintelligence.ui.fno.BuildupDetailScreen
import com.example.marketintelligence.ui.main.MainViewModel
import com.example.marketintelligence.ui.market.MarketScreen
import com.example.marketintelligence.ui.market.InstrumentSearchScreen
import com.example.marketintelligence.ui.notifications.NotificationsScreen
import com.example.marketintelligence.ui.portfolio.HoldingDetailScreen
import com.example.marketintelligence.ui.portfolio.PortfolioScreen
import com.example.marketintelligence.ui.scanner.AIScannerScreen
import com.example.marketintelligence.ui.settings.SettingsScreen
import com.example.marketintelligence.ui.welcome.MarketIntelligenceWelcomeScreen
import com.example.marketintelligence.ui.theme.*
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

sealed class Screen(val route: String, val label: String, val icon: Int? = null) {
    object Market : Screen("market", "HUB", R.drawable.ic_market)
    object AiScan : Screen("ai_scan", "SCAN", R.drawable.ic_ai_scan)
    object Fno : Screen("fno", "F&O", R.drawable.ic_fno)
    object Portfolio : Screen("portfolio", "PF", R.drawable.ic_portfolio)
    object Academy : Screen("academy", "ACADEMY", R.drawable.ic_academy)
    object Notifications : Screen("notifications", "NOTIFICATIONS")
    object Settings : Screen("settings", "SETTINGS")
    object BuildupDetail : Screen("buildup_detail", "BUILDUP")
    object Search : Screen("search", "SEARCH")
    object Analysis : Screen("analysis/{symbol}/{type}", "ANALYSIS") { 
        fun createRoute(symbol: String, type: String) = "analysis/${Uri.encode(symbol)}/$type" 
    }
    object HoldingDetail : Screen("holding/{symbol}", "DETAIL") { fun createRoute(symbol: String) = "holding/$symbol" }
}

val navItems = listOf(Screen.Market, Screen.AiScan, Screen.Fno, Screen.Portfolio, Screen.Academy)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authTokenManager: AuthTokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            
            MarketIntelligenceAiAndroidTheme(darkTheme = isDarkMode) {
                var showSplash by remember { mutableStateOf(true) }
                Crossfade(targetState = showSplash, animationSpec = tween(400), label = "splashCrossfade") { isSplash ->
                    if (isSplash) {
                        MarketIntelligenceWelcomeScreen(
                            onAnimationComplete = { showSplash = false }
                        )
                    } else {
                        MainScreen(mainViewModel = mainViewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null && 
            (uri.scheme == AuthTokenManager.DEEP_LINK_SCHEME_AARKAAI || uri.scheme == AuthTokenManager.DEEP_LINK_SCHEME_MI) &&
            uri.host == AuthTokenManager.DEEP_LINK_HOST
        ) {
            val token = uri.getQueryParameter("token")
            val userId = uri.getQueryParameter("user_id") ?: "google_user"
            val name = uri.getQueryParameter("name") ?: "Institutional User"
            val email = uri.getQueryParameter("email")

            if (!token.isNullOrBlank()) {
                lifecycleScope.launch {
                    authTokenManager.saveAuth(
                        token = token,
                        userId = userId,
                        name = name,
                        email = email,
                        provider = "google"
                    )
                    Toast.makeText(
                        applicationContext,
                        "Google Authentication Active • Welcome, $name!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

@EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface EngineRouterEntryPoint {
    fun engineRouter(): EngineRouter
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val unreadCount by mainViewModel.unreadNotificationCount.collectAsState()
    val colors = MaterialTheme.colorScheme

    val isUtilityScreen = currentRoute == Screen.Settings.route || 
                          currentRoute == Screen.Notifications.route || 
                          currentRoute == Screen.BuildupDetail.route ||
                          currentRoute == Screen.Search.route ||
                          currentRoute?.startsWith("analysis/") == true ||
                          currentRoute?.startsWith("holding/") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isUtilityScreen) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_falcon_emblem),
                                contentDescription = "Market Intelligence Logo",
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "MARKET INTELLIGENCE", 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "-",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "MI 007", 
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Text(
                                (currentRoute?.split("/")?.first()?.uppercase() ?: "SYSTEM"), 
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isUtilityScreen) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    if (!isUtilityScreen || currentRoute != Screen.Notifications.route) {
                        IconButton(onClick = { 
                            navController.navigate(Screen.Notifications.route) {
                                launchSingleTop = true
                            }
                        }) {
                            BadgedBox(
                                badge = { 
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = AppRed,
                                            contentColor = Color.White,
                                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                        ) { Text("$unreadCount", fontSize = 7.sp) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = colors.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        IconButton(onClick = { 
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background, 
                    titleContentColor = colors.onBackground
                ),
                modifier = Modifier.drawWithContent {
                    drawContent()
                    drawLine(
                        color = colors.outline,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            InstitutionalBottomNav(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(colors.background)) {
            NavHost(
                navController,
                startDestination = Screen.Market.route,
                enterTransition = { fadeIn() + slideInHorizontally { it / 2 } },
                exitTransition = { fadeOut() + slideOutHorizontally { -it / 2 } },
                popEnterTransition = { fadeIn() + slideInHorizontally { -it / 2 } },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 2 } }
            ) {
                composable(Screen.Market.route) {
                    MarketScreen(
                        onNavigateToAnalysis = { symbol, type ->
                            navController.navigate(Screen.Analysis.createRoute(symbol, type)) { launchSingleTop = true }
                        },
                        onNavigateToSearch = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                }

                composable(Screen.Search.route) {
                    InstrumentSearchScreen(
                        onInstrumentSelected = { symbol, type ->
                            navController.navigate(Screen.Analysis.createRoute(symbol, type))
                        }
                    )
                }

                composable(Screen.AiScan.route) {
                    AIScannerScreen(
                        onViewChart = { symbol ->
                            navController.navigate(Screen.Analysis.createRoute(symbol, "STOCK")) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.Fno.route) { 
                    FnoScreen(onOpenBuildupScanner = {
                        navController.navigate(Screen.BuildupDetail.route) { launchSingleTop = true }
                    })
                }
                composable(Screen.Portfolio.route) {
                    PortfolioScreen(onHoldingClick = { symbol ->
                        navController.navigate(Screen.HoldingDetail.createRoute(symbol)) { launchSingleTop = true }
                    })
                }
                composable(Screen.Academy.route) { AcademyScreen() }
                composable(Screen.Notifications.route) {
                    NotificationsScreen(onNavigateToAnalysis = { symbol ->
                        navController.navigate(Screen.Analysis.createRoute(symbol, "STOCK")) {
                            popUpTo(Screen.Notifications.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    })
                 }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(Screen.BuildupDetail.route) { 
                    BuildupDetailScreen(onNavigateBack = { navController.popBackStack() }) 
                }
                composable(
                    route = Screen.Analysis.route,
                    arguments = listOf(
                        navArgument("symbol") { type = NavType.StringType },
                        navArgument("type") { type = NavType.StringType }
                    )
                ) {
                    val hiltEntryPoint = EntryPointAccessors.fromApplication(LocalContext.current, EngineRouterEntryPoint::class.java)
                    AnalysisScreen(engineRouter = hiltEntryPoint.engineRouter())
                }
                composable(
                    route = Screen.HoldingDetail.route,
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { backStackEntry ->
                    val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
                    HoldingDetailScreen(
                        symbol = symbol,
                        onNavigateToAnalysis = { sym ->
                            navController.navigate(Screen.Analysis.createRoute(sym, "STOCK")) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InstitutionalBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val colors = MaterialTheme.colorScheme
    
    Surface(
        modifier = Modifier.fillMaxWidth().drawWithContent {
            drawContent()
            drawLine(
                color = colors.outline,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        },
        color = colors.background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val contentColor = if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.6f)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!isSelected) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                ) {
                    screen.icon?.let {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = it),
                            contentDescription = screen.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = screen.label,
                        color = contentColor,
                        fontSize = 8.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    
                    if (isSelected) {
                        Box(modifier = Modifier.padding(top = 4.dp).size(2.dp).background(colors.primary, CircleShape))
                    } else {
                        Spacer(modifier = Modifier.padding(top = 4.dp).size(2.dp))
                    }
                }
            }
        }
    }
}
