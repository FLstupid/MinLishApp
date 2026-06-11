package com.example.minlish.data.model

import com.example.minlish.logic.CefrLevels

/**
 * User profile document: Firestore `users/{uid}` (merge). DataStore mirrors fields for offline use.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val goal: String = CefrLevels.DEFAULT_GOAL,
    val level: String = CefrLevels.DEFAULT_LEVEL,
    val dailyGoal: Int = 10,
)

