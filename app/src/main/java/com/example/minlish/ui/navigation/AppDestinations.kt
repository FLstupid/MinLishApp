package com.example.minlish.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.minlish.R

/** String route constants used with Jetpack Navigation Compose. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val LIBRARY = "library"
    const val LEARN = "learn"
    const val PRACTICE = "practice"
    const val PROFILE = "profile"
    const val ANALYTICS = "analytics"
    const val CHECKPOINT = "checkpoint"
    const val AUTH = "auth"
    const val SET_DETAIL = "library/{setId}"

    fun setDetail(setId: Long) = "library/$setId"
}

/** Bottom-nav destinations; [route] maps to the corresponding [Routes] entry. */
enum class AppDestinations(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, R.string.nav_home, Icons.Outlined.Home),
    VOCABULARY(Routes.LIBRARY, R.string.nav_library, Icons.AutoMirrored.Outlined.MenuBook),
    LEARN(Routes.LEARN, R.string.nav_learn, Icons.Outlined.AutoStories),
    PRACTICE(Routes.PRACTICE, R.string.nav_practice, Icons.Outlined.FitnessCenter),
    PROFILE(Routes.PROFILE, R.string.nav_profile, Icons.Outlined.Person),
}
