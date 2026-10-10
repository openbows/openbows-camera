package com.openbows.camera.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openbows.camera.OpenbowsApplication
import com.openbows.camera.ui.album.AlbumScreen
import com.openbows.camera.ui.album.PhotoDetailScreen
import com.openbows.camera.ui.camera.CameraScreen
import com.openbows.camera.ui.permission.PermissionGate
import com.openbows.camera.ui.template.TemplateEditorScreen
import com.openbows.camera.ui.template.TemplateListScreen

@Composable
fun OpenbowsRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as OpenbowsApplication
    LaunchedEffect(Unit) {
        app.container.templateRepository.ensureDefault()
    }
    PermissionGate {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = "camera") {
            composable("camera") {
                CameraScreen(
                    onOpenAlbum = { nav.navigate("album") },
                    onOpenTemplates = { nav.navigate("templates") },
                )
            }
            composable("album") {
                AlbumScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPhoto = { id -> nav.navigate("photo/$id") },
                )
            }
            composable(
                "photo/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PhotoDetailScreen(
                    photoId = id,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("templates") {
                TemplateListScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate("template/$id") },
                )
            }
            composable(
                "template/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                TemplateEditorScreen(
                    templateId = id,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
