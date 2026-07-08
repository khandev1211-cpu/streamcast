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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.SourceType
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import com.streamcast.feature.library.ui.LocalScreen
import com.streamcast.feature.library.ui.MusicScreen
import com.streamcast.feature.library.ui.ScreenScreen
import com.streamcast.feature.library.ui.MeScreen
import com.streamcast.feature.library.ui.player.PlayerScreen
import com.streamcast.ui.theme.StreamCastTheme
import dagger.hilt.android.AndroidEntryPoint

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
                val currentDestination = navBackStackEntry?.destination
                val currentRoute = currentDestination?.route ?: "library"
                val isPlayerScreen = currentRoute.startsWith("player")

                Scaffold(
                    bottomBar = {
                        if (!isPlayerScreen) {
                            StreamCastBottomBar(navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = MainScreen.Local.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(MainScreen.Local.route) {
                            LocalScreen(
                                onMediaClick = { media ->
                                    val encodedUri = Uri.encode(media.uri.toString())
                                    navController.navigate("player/$encodedUri/${media.displayName}")
                                }
                            )
                        }
                        composable(MainScreen.Music.route) {
                            MusicScreen(
                                onMediaClick = { media ->
                                    val encodedUri = Uri.encode(media.uri.toString())
                                    navController.navigate("player/$encodedUri/${media.displayName}")
                                }
                            )
                        }
                        composable(MainScreen.Screen.route) { 
                            ScreenScreen()
                        }
                        composable(MainScreen.Me.route) { 
                            MeScreen()
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
fun StreamCastTopBar(navController: androidx.navigation.NavHostController) {
    TopAppBar(
        title = { Text("StreamCast", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        actions = {
            IconButton(onClick = { /* Search */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { /* More Menu */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        }
    )
}

sealed class MainScreen(val route: String, val label: String, val icon: ImageVector) {
    object Local : MainScreen("local", "Local", Icons.Default.Folder)
    object Music : MainScreen("music", "Music", Icons.Default.MusicNote)
    object Screen : MainScreen("screen", "Screen", Icons.Default.Cast)
    object Me : MainScreen("me", "Me", Icons.Default.Person)
}

@Composable
fun StreamCastBottomBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(MainScreen.Local, MainScreen.Music, MainScreen.Screen, MainScreen.Me)
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
