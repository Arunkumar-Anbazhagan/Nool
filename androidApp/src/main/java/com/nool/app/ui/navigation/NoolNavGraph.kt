package com.nool.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nool.app.ui.git.GitConflictScreen
import com.nool.app.ui.git.GitHistoryScreen
import com.nool.app.ui.git.GitSettingsScreen
import com.nool.app.ui.git.GitSetupScreen
import com.nool.app.ui.git.GitStatusScreen
import com.nool.app.ui.git.GitViewModel
import com.nool.app.ui.graph.GraphScreen
import com.nool.app.ui.graph.GraphViewModel
import com.nool.app.ui.notes.NoteDetailScreen
import com.nool.app.ui.notes.NoteEditViewModel
import com.nool.app.ui.notes.NotesListScreen
import com.nool.app.ui.notes.NotesViewModel
import com.nool.app.ui.search.SearchScreen
import com.nool.app.ui.search.SearchViewModel
import com.nool.app.ui.settings.SettingsScreen
import com.nool.app.ui.settings.SettingsViewModel
import com.nool.app.ui.trash.TrashScreen
import com.nool.app.ui.trash.TrashViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NoolNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.NotesList.createRoute("")
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(200)) }
    ) {
        composable(
            route = Screen.NotesList.route,
            arguments = listOf(
                navArgument("folder") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val folder = backStackEntry.arguments?.getString("folder") ?: ""
            val decodedFolder = Screen.NoteDetail.decodePath(folder)
            val viewModel: NotesViewModel = koinViewModel(
                key = "notes_$decodedFolder",
                parameters = { parametersOf(decodedFolder) }
            )

            NotesListScreen(
                viewModel = viewModel,
                onNavigateToNote = { notePath ->
                    navController.navigate(Screen.NoteDetail.createRoute(notePath))
                },
                onNavigateToFolder = { targetFolder ->
                    navController.navigate(Screen.NotesList.createRoute(targetFolder))
                },
                onNavigateUp = {
                    navController.navigateUp()
                },
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onNavigateToSearch = { query ->
                    navController.navigate(Screen.Search.createRoute(initialQuery = query))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToGraph = {
                    navController.navigate(Screen.Graph.createRoute(""))
                },
                onNavigateToGitSettings = {
                    navController.navigate(Screen.GitSettings.route)
                },
                onNavigateToGitStatus = {
                    navController.navigate(Screen.GitStatus.route)
                },
                onNavigateToGitConflict = {
                    navController.navigate(Screen.GitConflict.route)
                },
                onNavigateToGitSetup = {
                    navController.navigate(Screen.GitSetup.route)
                }
            )
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val decodedPath = Screen.NoteDetail.decodePath(rawPath)
            val viewModel: NoteEditViewModel = koinViewModel(
                key = "note_$decodedPath",
                parameters = { parametersOf(decodedPath) }
            )

            NoteDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNote = { notePath ->
                    navController.navigate(Screen.NoteDetail.createRoute(notePath))
                },
                onNavigateToGraph = { notePath ->
                    navController.navigate(Screen.Graph.createRoute(notePath ?: ""))
                }
            )
        }

        composable(route = Screen.Trash.route) {
            val viewModel: TrashViewModel = koinViewModel()
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Graph.route,
            arguments = listOf(
                navArgument("notePath") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val notePath = backStackEntry.arguments?.getString("notePath") ?: ""
            val decodedNotePath = if (notePath.isNotBlank()) URLDecoder.decode(notePath, StandardCharsets.UTF_8.toString()).ifEmpty { null } else null
            val viewModel: GraphViewModel = koinViewModel(
                parameters = { parametersOf(decodedNotePath) }
            )

            GraphScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNote = { targetNotePath ->
                    navController.navigate(Screen.NoteDetail.createRoute(targetNotePath))
                }
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("tag") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val tag = backStackEntry.arguments?.getString("tag") ?: ""
            val decodedTag = if (tag.isNotEmpty()) Screen.NoteDetail.decodePath(tag) else null
            val query = backStackEntry.arguments?.getString("query") ?: ""
            val decodedQuery = if (query.isNotEmpty()) Screen.NoteDetail.decodePath(query) else null
            val viewModel: SearchViewModel = koinViewModel(
                parameters = { parametersOf(decodedTag, decodedQuery) }
            )

            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNote = { notePath ->
                    navController.navigate(Screen.NoteDetail.createRoute(notePath))
                }
            )
        }

        composable(route = Screen.Settings.route) {
            val viewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGitSettings = {
                    navController.navigate(Screen.GitSettings.route)
                },
                onNavigateToGitStatus = {
                    navController.navigate(Screen.GitStatus.route)
                },
                onNavigateToGitSetup = {
                    navController.navigate(Screen.GitSetup.route)
                }
            )
        }

        // Git Screens
        composable(route = Screen.GitSetup.route) {
            val viewModel: GitViewModel = koinViewModel()
            GitSetupScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(route = Screen.GitStatus.route) {
            val viewModel: GitViewModel = koinViewModel()
            GitStatusScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.GitHistory.route) },
                onNavigateToConflicts = { navController.navigate(Screen.GitConflict.route) }
            )
        }

        composable(route = Screen.GitHistory.route) {
            val viewModel: GitViewModel = koinViewModel()
            GitHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.GitConflict.route) {
            val viewModel: GitViewModel = koinViewModel()
            GitConflictScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.GitSettings.route) {
            val viewModel: GitViewModel = koinViewModel()
            GitSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
