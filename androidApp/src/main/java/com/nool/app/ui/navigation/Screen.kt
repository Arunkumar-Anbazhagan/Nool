package com.nool.app.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    data object NotesList : Screen("notes?folder={folder}") {
        fun createRoute(folder: String = ""): String {
            val encoded = URLEncoder.encode(folder, StandardCharsets.UTF_8.toString())
            return "notes?folder=$encoded"
        }
    }

    data object NoteDetail : Screen("note/{path}") {
        fun createRoute(relativePath: String): String {
            val encoded = URLEncoder.encode(relativePath, StandardCharsets.UTF_8.toString())
            return "note/$encoded"
        }

        fun decodePath(encodedPath: String): String {
            return URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
        }
    }

    data object Trash : Screen("trash")

    data object Search : Screen("search?tag={tag}&query={query}") {
        fun createRoute(initialTag: String = "", initialQuery: String = ""): String {
            val encodedTag = URLEncoder.encode(initialTag, StandardCharsets.UTF_8.toString())
            val encodedQuery = URLEncoder.encode(initialQuery, StandardCharsets.UTF_8.toString())
            return "search?tag=$encodedTag&query=$encodedQuery"
        }
    }

    data object Settings : Screen("settings")

    data object Calendar : Screen("calendar")

    data object Graph : Screen("graph?notePath={notePath}") {
        fun createRoute(notePath: String = ""): String {
            val encoded = URLEncoder.encode(notePath, StandardCharsets.UTF_8.toString())
            return "graph?notePath=$encoded"
        }
    }

    // Git Screens
    data object GitSetup : Screen("git_setup")
    data object GitStatus : Screen("git_status")
    data object GitHistory : Screen("git_history")
    data object GitConflict : Screen("git_conflict")
    data object GitSettings : Screen("git_settings")
}
