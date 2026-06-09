package com.example.minlish.data.model

/**
 * User profile document: Firestore `users/{uid}` (merge). DataStore mirrors fields for offline use.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val goal: String = "IELTS",
    val level: String = "B1",
    val dailyGoal: Int = 10,
)

