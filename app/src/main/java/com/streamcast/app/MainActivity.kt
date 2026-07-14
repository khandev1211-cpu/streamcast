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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.streamcast.core.player.MediaSource
import com.streamcast.core.player.SourceType
import com.streamcast.feature.library.ui.LocalScreen
import com.streamcast.feature.library.ui.MusicScreen
import com.streamcast.feature.iptv.ui.IptvScreen
import com.streamcast.feature.iptv.ui.IptvPlayerScreen
import com.streamcast.feature.library.ui.MeScreen
import com.streamcast.feature.library.ui.player.AudioPlayerScreen
import com.streamcast.feature.library.ui.player.PlayerScreen
import com.streamcast.ui.theme.StreamCastTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun dispatchGenericMotionEvent(ev: android.view.MotionEvent?): Boolean {
        // PERMANENT FIX for ACTION_HOVER_EXIT crash on Android 10/TECNO devices
        // This blocks the system from sending hover signals to the UI, which prevents the internal Compose crash
        if (ev != null && (ev.actionMasked == android.view.MotionEvent.ACTION_HOVER_ENTER ||
            ev.actionMasked == android.view.MotionEvent.ACTION_HOVER_MOVE ||
            ev.actionMasked == android.view.MotionEvent.ACTION_HOVER_EXIT)) {
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

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
                val currentRoute = currentDestination?.route ?: "local"
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
                            val playerViewModel: com.streamcast.feature.library.viewmodel.PlayerViewModel = hiltViewModel()
                            LocalScreen(
                                onMediaClick = { media: MediaSource, playlist: List<MediaSource> ->
                                    val encodedUri = Uri.encode(media.uri.toString())
                                    val encodedName = Uri.encode(media.displayName)
                                    val index = playlist.indexOf(media)
                                    playerViewModel.playPlaylist(playlist, if (index != -1) index else 0)
                                    navController.navigate("player/$encodedUri/$encodedName")
                                }
                            )
                        }
                        composable(MainScreen.Music.route) {
                            val playerViewModel: com.streamcast.feature.library.viewmodel.PlayerViewModel = hiltViewModel()
                            MusicScreen(
                                onMediaClick = { media: MediaSource, playlist: List<MediaSource> ->
                                    val encodedUri = Uri.encode(media.uri.toString())
                                    val encodedName = Uri.encode(media.displayName)
                                    val index = playlist.indexOf(media)
                                    playerViewModel.playPlaylist(playlist, if (index != -1) index else 0)
                                    navController.navigate("audio-player/$encodedUri/$encodedName")
                                }
                            )
                        }
                        composable(MainScreen.Iptv.route) {
                            IptvScreen(
                                onChannelClick = { media: MediaSource, playlist: List<MediaSource> ->
                                    val encodedUri = Uri.encode(media.uri.toString())
                                    val encodedName = Uri.encode(media.displayName)
                                    navController.navigate("iptv-player/$encodedUri/$encodedName")
                                }
                            )
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
                        composable(
                            route = "audio-player/{uri}/{name}",
                            arguments = listOf(
                                navArgument("uri") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val uri = Uri.parse(Uri.decode(backStackEntry.arguments?.getString("uri")))
                            val name = backStackEntry.arguments?.getString("name") ?: "Audio"
                            
                            val mediaSource = MediaSource(
                                id = uri.toString(),
                                uri = uri,
                                type = SourceType.LOCAL,
                                displayName = name,
                                isCacheable = true
                            )
                            AudioPlayerScreen(
                                mediaSource = mediaSource,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "iptv-player/{uri}/{name}",
                            arguments = listOf(
                                navArgument("uri") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val uri = Uri.parse(Uri.decode(backStackEntry.arguments?.getString("uri")))
                            val name = backStackEntry.arguments?.getString("name") ?: "Channel"
                            
                            val mediaSource = MediaSource(
                                id = uri.toString(),
                                uri = uri,
                                type = SourceType.IPTV,
                                displayName = name,
                                isCacheable = false
                            )
                            IptvPlayerScreen(
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

sealed class MainScreen(val route: String, val label: String, val icon: ImageVector) {
    object Local : MainScreen("local", "Local", Icons.Default.Folder)
    object Music : MainScreen("music", "Music", Icons.Default.MusicNote)
    object Iptv : MainScreen("iptv", "IPTV", Icons.Default.Tv)
    object Me : MainScreen("me", "Me", Icons.Default.Person)
}

@Composable
fun StreamCastBottomBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(MainScreen.Local, MainScreen.Music, MainScreen.Iptv, MainScreen.Me)
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
