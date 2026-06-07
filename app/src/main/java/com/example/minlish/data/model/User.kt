package com.example.minlish.data.model

/**
 * User profile stored in Firestore under: /users/{uid}
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val goal: String = "IELTS",
    val level: String = "B1",
    val dailyGoal: Int = 10,
)

