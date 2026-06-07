package com.example.minlish.data.repository

import com.example.minlish.data.dao.VocabularySetDao
import com.example.minlish.data.dao.WordDao
import com.example.minlish.data.model.VocabularySet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class VocabSetRepository(
    private val vocabularySetDao: VocabularySetDao,
    private val wordDao: WordDao,
    private val firebaseAuth: FirebaseAuth? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    companion object {
        // Prevent multiple ViewModels creating the default set concurrently.
        private val defaultSetMutex = Mutex()
        const val DEFAULT_SET_TITLE = "Bộ từ của tôi"
    }

    fun getAllSets(): Flow<List<VocabularySet>> = vocabularySetDao.getAllSets()

    suspend fun getOrCreateDefaultSetId(): Long {
        return defaultSetMutex.withLock {
            val sets = vocabularySetDao.getAllSets().first()
            if (sets.isNotEmpty()) return@withLock sets.first().id

            // Create a single default set for MVP flows.
            val insertedId = vocabularySetDao.insertSet(
                VocabularySet(
                    title = DEFAULT_SET_TITLE,
                    description = null,
                    tags = "",
                    wordCount = 0,
                    createdAt = System.currentTimeMillis(),
                    userId = ""
                )
            )

            // Re-query to return the actual row id (defensive against race / REPLACE semantics).
            val afterInsert = vocabularySetDao.getAllSets().first()
            afterInsert.firstOrNull()?.id ?: insertedId
        }
    }

    /** Removes empty placeholder sets created before starter content is installed. */
    suspend fun deleteEmptyDefaultSets() {
        val legacyTitles = setOf(DEFAULT_SET_TITLE, "My vocabulary")
        val sets = vocabularySetDao.getAllSets().first()
        for (set in sets) {
            if (set.title !in legacyTitles) continue
            val count = wordDao.countWordsInSet(set.id)
            if (count == 0) {
                vocabularySetDao.deleteSet(set)
            }
        }
    }

    suspend fun insertSet(
        title: String,
        description: String?,
        tags: String,
    ): Long {
        val uid = firebaseAuth?.currentUser?.uid
        val createdAt = System.currentTimeMillis()
        val toInsert = VocabularySet(
            title = title.trim(),
            description = description?.takeIf { it.isNotBlank() },
            tags = tags.trim(),
            wordCount = 0,
            createdAt = createdAt,
            userId = uid.orEmpty(),
        )

        val insertedId = vocabularySetDao.insertSet(toInsert)
        if (uid != null) {
            pushSetToFirestore(uid, toInsert.copy(id = insertedId, userId = uid))
        }
        return insertedId
    }

    suspend fun updateSet(set: VocabularySet) {
        vocabularySetDao.updateSet(set)
        val uid = firebaseAuth?.currentUser?.uid ?: return
        pushSetToFirestore(uid, set.copy(userId = uid))
    }

    suspend fun deleteSet(set: VocabularySet) {
        // Always delete locally (offline-safe).
        wordDao.deleteWordsBySetId(set.id)
        vocabularySetDao.deleteSet(set)

        val uid = firebaseAuth?.currentUser?.uid ?: return

        val docId = set.id.toString()
        withContext(Dispatchers.IO) {
            // Best-effort cloud cleanup: delete words whose setId matches.
            val wordsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("words")
                .whereEqualTo("setId", set.id)
                .get()
                .await()

            val docs = wordsSnapshot.documents
            if (docs.isNotEmpty()) {
                // Firestore limits batch size; chunk defensively.
                docs.chunked(500).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            }

            firestore.collection("users")
                .document(uid)
                .collection("sets")
                .document(docId)
                .delete()
                .await()
        }
    }

    suspend fun syncFromCurrentUser() = withContext(Dispatchers.IO) {
        val uid = firebaseAuth?.currentUser?.uid ?: return@withContext
        val snapshot = firestore.collection("users").document(uid).collection("sets").get().await()

        val remoteSets = snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val remoteId = doc.id.toLongOrNull() ?: 0L

            val title = data["title"] as? String ?: return@mapNotNull null
            val description = data["description"] as? String
            val tags = data["tags"] as? String ?: ""
            val wordCount = (data["wordCount"] as? Number)?.toInt() ?: 0
            val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

            VocabularySet(
                id = remoteId,
                title = title,
                description = description,
                tags = tags,
                wordCount = wordCount,
                createdAt = createdAt,
                userId = uid,
            )
        }

        for (set in remoteSets) {
            vocabularySetDao.insertSet(set)
        }
    }

    suspend fun refreshWordCount(setId: Long) {
        val set = vocabularySetDao.getSetById(setId) ?: return
        val count = wordDao.countWordsInSet(setId)
        if (set.wordCount != count) {
            vocabularySetDao.updateSet(set.copy(wordCount = count))
        }
    }

    private suspend fun pushSetToFirestore(uid: String, set: VocabularySet) = withContext(Dispatchers.IO) {
        firestore.collection("users").document(uid)
            .collection("sets")
            .document(set.id.toString())
            .set(set)
            .await()
    }
}

