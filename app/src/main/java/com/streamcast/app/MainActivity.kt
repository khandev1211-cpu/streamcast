package com.streamcast.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.SourceType
import com.streamcast.feature.library.ui.LibraryScreen
import com.streamcast.feature.library.ui.player.PlayerScreen
import com.streamcast.ui.theme.StreamCastTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamCastTheme {
                val navController = rememberNavController()
                
                // Permission Handling
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_VIDEO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(permission)
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "library"
                val isPlayerScreen = currentRoute.startsWith("player")

                Scaffold(
                    topBar = {
                        if (!isPlayerScreen) {
                            StreamCastTopBar(navController, currentRoute)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "library",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("library") {
                            LibraryScreen(
                                onVideoClick = { video ->
                                    val encodedUri = Uri.encode(video.uri.toString())
                                    navController.navigate("player/$encodedUri/${video.displayName}")
                                }
                            )
                        }
                        composable("iptv") { 
                            Surface(Modifier.fillMaxSize()) { Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Text("IPTV Screen") } }
                        }
                        composable("live") { 
                            Surface(Modifier.fillMaxSize()) { Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Live Screen") } }
                        }
                        composable("settings") { 
                            Surface(Modifier.fillMaxSize()) { Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Settings Screen") } }
                        }
                        composable(
                            route = "player/{uri}/{name}",
                            arguments = listOf(
                                navArgument("uri") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val uri = Uri.parse(Uri.decode(backStackEntry.arguments?.getString("uri")))
                            val name = backStackEntry.arguments?.getString("name") ?: "Video"
                            
                            val mediaSource = MediaSource(
                                id = uri.toString(),
                                uri = uri,
                                type = SourceType.LOCAL,
                                displayName = name,
                                isCacheable = true
                            )
                            PlayerScreen(
                                mediaSource = mediaSource,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamCastTopBar(navController: androidx.navigation.NavHostController, currentRoute: String) {
    var showMenu by remember { mutableStateOf(false) }
    val tabs = listOf("library" to "Library", "iptv" to "IPTV", "live" to "Live")
    
    Column {
        TopAppBar(
            title = { Text("StreamCast", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            actions = {
                IconButton(onClick = { /* Search */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { 
                            showMenu = false
                            navController.navigate("settings")
                        },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                }
            }
        )
        
        // MX Player style top tabs
        if (currentRoute in listOf("library", "iptv", "live")) {
            TabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabs.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0)])
                    )
                }
            ) {
                tabs.forEach { (route, label) ->
                    Tab(
                        selected = currentRoute == route,
                        onClick = { navController.navigate(route) {
                            popUpTo("library") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }},
                        text = { Text(label, style = MaterialTheme.typography.bodyMedium) }
                    )
                }
            }
        }
    }
}
