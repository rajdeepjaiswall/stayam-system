package `in`.getdownfoundation.sahusales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import `in`.getdownfoundation.sahusales.alarm.ReminderSyncer
import `in`.getdownfoundation.sahusales.ui.MainViewModel
import `in`.getdownfoundation.sahusales.ui.MainViewModelFactory
import `in`.getdownfoundation.sahusales.ui.auth.LoginScreen
import `in`.getdownfoundation.sahusales.ui.auth.RegisterScreen
import `in`.getdownfoundation.sahusales.ui.contacts.ContactsScreen
import `in`.getdownfoundation.sahusales.ui.dashboard.DashboardScreen
import `in`.getdownfoundation.sahusales.ui.debug.DebugScreen
import `in`.getdownfoundation.sahusales.ui.events.EventsScreen
import `in`.getdownfoundation.sahusales.ui.invoices.InvoicesScreen
import `in`.getdownfoundation.sahusales.ui.settings.SettingsScreen
import `in`.getdownfoundation.sahusales.ui.team.TeamScreen
import `in`.getdownfoundation.sahusales.ui.theme.Primary
import `in`.getdownfoundation.sahusales.ui.theme.SahuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SahuTheme {
                SahuApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync on every foreground
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            ReminderSyncer.sync(this@MainActivity)
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.Person)
    object Events : Screen("events", "Events", Icons.Default.DateRange)
    object Invoices : Screen("invoices", "Invoices", Icons.Default.Receipt)
    object Team : Screen("team", "Team", Icons.Default.Group)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Debug : Screen("debug", "Debug", Icons.Default.BugReport)
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Register : Screen("register", "Register", Icons.Default.PersonAdd)
}

@Composable
fun SahuApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(androidx.compose.ui.platform.LocalContext.current))
    val token by viewModel.token.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Logo tap counter for debug screen
    var logoTaps by remember { mutableStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }

    LaunchedEffect(error) {
        error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    val bottomTabs = remember(currentUser) {
        buildList {
            add(Screen.Dashboard)
            add(Screen.Contacts)
            add(Screen.Events)
            add(Screen.Invoices)
            if (currentUser?.role == "admin") add(Screen.Team)
            add(Screen.Settings)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = token != null && currentRoute !in listOf(Screen.Login.route, Screen.Register.route, Screen.Debug.route)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    bottomTabs.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label, fontSize = 10.sp) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (screen.route == Screen.Dashboard.route) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTap < 400) {
                                        logoTaps++
                                        if (logoTaps >= 7) {
                                            logoTaps = 0
                                            navController.navigate(Screen.Debug.route)
                                            return@NavigationBarItem
                                        }
                                    } else {
                                        logoTaps = 1
                                    }
                                    lastTap = now
                                }
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                indicatorColor = Primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (token != null) Screen.Dashboard.route else Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(viewModel,
                    onLoginSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                    onGoRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(viewModel,
                    onRegisterSuccess = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Register.route) { inclusive = true } } },
                    onGoLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
            composable(Screen.Contacts.route) { ContactsScreen(viewModel) { /* TODO: contact detail */ } }
            composable(Screen.Events.route) { EventsScreen(viewModel) }
            composable(Screen.Invoices.route) { InvoicesScreen(viewModel) }
            composable(Screen.Team.route) { TeamScreen(viewModel) }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel) {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            }
            composable(Screen.Debug.route) { DebugScreen(viewModel) }
        }
    }
}
