package com.example.minlish.data.repository

import com.example.minlish.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun getUserProfile(uid: String): User? = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.toObject<User>()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun upsertUserProfile(profile: User): Void? = withContext(Dispatchers.IO) {
        firestore.collection("users").document(profile.uid).set(profile).await()
    }

    suspend fun mergeFcmToken(uid: String, token: String) = withContext(Dispatchers.IO) {
        firestore.collection("users")
            .document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .await()
    }
}
