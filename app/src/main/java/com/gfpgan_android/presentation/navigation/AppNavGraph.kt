package com.gfpgan_android.presentation.navigation

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * AppNavGraph - Ana navigasyon yapısı
 * 
 * Jetpack Compose Navigation graph tanımı
 * Tüm ekranlar ve aralarındaki geçişler burada tanımlanır
 * 
 * @param navController Navigation controller
 * @param modifier Compose modifier
 * @param startDestination Başlangıç ekranı (default: Home)
 * @param isDarkMode Dark mode aktif mi?
 * @param onThemeToggle Theme toggle callback
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        
        // Home Screen - Ana Ekran
        composable(route = Screen.Home.route) {
            com.gfpgan_android.presentation.screens.HomeScreen(
                isDarkMode = isDarkMode,
                onThemeToggle = onThemeToggle,
                onImageSelected = { imageUri ->
                    // URL encode and navigate to Editor
                    val encodedUri = java.net.URLEncoder.encode(
                        imageUri.toString(), 
                        "UTF-8"
                    )
                    navController.navigate(Screen.Editor.createRoute(encodedUri))
                },
                modifier = Modifier
            )
        }
        
        // Result Screen - Sonuç Ekranı
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("resultUri") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("resultUri")
            val resultUri = encodedUri?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            
            // TODO: ResultScreen composable'ı buraya gelecek
            // ResultScreen(
            //     resultUri = resultUri,
            //     onBackToHome = {
            //         navController.navigate(Screen.Home.route) {
            //             popUpTo(Screen.Home.route) { inclusive = true }
            //         }
            //     },
            //     onShareClick = { /* Share logic */ }
            // )
        }
        
        // Settings Screen - Ayarlar (Opsiyonel)
        composable(route = Screen.Settings.route) {
            // TODO: SettingsScreen composable'ı buraya gelecek
        }
        
        // Face Restore Screen - GFPGAN Yüz Restorasyonu
        composable(route = Screen.FaceRestore.route) {
            com.gfpgan_android.presentation.screens.FaceRestoreScreen()
        }
        
        
        // RealESRGAN Test Screen
        composable(route = Screen.RealESRGANTest.route) {
            com.gfpgan_android.presentation.screens.RealESRGANTestScreen()
        }
        
        // Editor Screen - Unified image editor
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
            
            com.gfpgan_android.presentation.screens.EditorScreen(
                imageUri = decodedUri,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}
