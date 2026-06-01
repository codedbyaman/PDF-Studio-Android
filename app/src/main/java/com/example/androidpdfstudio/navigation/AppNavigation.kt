package com.example.androidpdfstudio.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.androidpdfstudio.screens.CompressPDFScreen
import com.example.androidpdfstudio.screens.EditPDFScreen
import com.example.androidpdfstudio.screens.HomeScreen
import com.example.androidpdfstudio.screens.MergePDFScreen
import com.example.androidpdfstudio.screens.PDFToImagesScreen
import com.example.androidpdfstudio.screens.PhotosToPDFScreen
import com.example.androidpdfstudio.screens.SplitPDFScreen
import com.example.androidpdfstudio.ui.theme.NavyDark
import com.example.androidpdfstudio.ui.theme.StudioAccent

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object EditPDF : Screen("edit_pdf")
    object MergePDF : Screen("merge_pdf")
    object SplitPDF : Screen("split_pdf")
    object PhotosToPDF : Screen("photos_to_pdf")
    object CompressPDF : Screen("compress_pdf")
    object PDFToImages : Screen("pdf_to_images")
}

private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)

private val navItems = listOf(
    NavItem(Screen.Home, Icons.Default.Home, "Home"),
    NavItem(Screen.EditPDF, Icons.Default.Edit, "Edit"),
    NavItem(Screen.MergePDF, Icons.AutoMirrored.Filled.CallMerge, "Merge"),
    NavItem(Screen.SplitPDF, Icons.Default.ContentCut, "Split"),
    NavItem(Screen.PhotosToPDF, Icons.Default.Collections, "Photos"),
)

@Composable
fun AppNavHost(navController: NavHostController) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NavyDark,
                tonalElevation = 0.dp,
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = StudioAccent,
                            selectedTextColor   = StudioAccent,
                            indicatorColor      = StudioAccent.copy(alpha = 0.15f),
                            unselectedIconColor = Color.White.copy(alpha = 0.50f),
                            unselectedTextColor = Color.White.copy(alpha = 0.50f),
                        ),
                    )
                }
            }
        },
        // Let each screen manage its own system window insets
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            // Only consume the bottom padding (= NavigationBar height)
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition        = { scaleIn(tween(240), initialScale = 0.94f) + fadeIn(tween(240)) },
            exitTransition         = { scaleOut(tween(180), targetScale = 0.94f) + fadeOut(tween(180)) },
            popEnterTransition     = { scaleIn(tween(240), initialScale = 0.94f) + fadeIn(tween(240)) },
            popExitTransition      = { scaleOut(tween(180), targetScale = 0.94f) + fadeOut(tween(180)) },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.EditPDF.route) {
                EditPDFScreen(navController = navController)
            }
            composable(Screen.MergePDF.route) {
                MergePDFScreen(navController = navController)
            }
            composable(Screen.SplitPDF.route) {
                SplitPDFScreen(navController = navController)
            }
            composable(Screen.PhotosToPDF.route) {
                PhotosToPDFScreen(navController = navController)
            }
            composable(Screen.CompressPDF.route) {
                CompressPDFScreen(navController = navController)
            }
            composable(Screen.PDFToImages.route) {
                PDFToImagesScreen(navController = navController)
            }
        }
    }
}
