package com.example.minlish.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.minlish.R

enum class AppDestinations(
    val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(R.string.nav_home, Icons.Outlined.Home),
    VOCABULARY(R.string.nav_library, Icons.Outlined.MenuBook),
    LEARN(R.string.nav_learn, Icons.Outlined.AutoStories),
    PRACTICE(R.string.nav_practice, Icons.Outlined.FitnessCenter),
    PROFILE(R.string.nav_profile, Icons.Outlined.Person),
}
