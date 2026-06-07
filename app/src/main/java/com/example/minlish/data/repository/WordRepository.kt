package com.example.minlish.data.repository

import android.util.Log
import com.example.minlish.data.dao.WordDao
import com.example.minlish.data.model.Word
import com.example.minlish.logic.CefrLevels
import com.example.minlish.logic.WordNorm
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WordRepository(
    private val wordDao: WordDao,
    private val firebaseAuth: FirebaseAuth? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun getWordsBySet(setId: Long): Flow<List<Word>> = wordDao.getWordsBySet(setId)

    fun getWordCountForSet(setId: Long): Flow<Int> = wordDao.getWordCountForSet(setId)

    suspend fun countAllWords(): Int = wordDao.countAllWords()

    fun getLearnedCountForSet(setId: Long): Flow<Int> = wordDao.getLearnedCountForSet(setId)

    fun getDueReviewWordsAnySet(currentTime: Long): Flow<List<Word>> =
        wordDao.getDueReviewWordsAnySet(currentTime = currentTime)

    fun getDueReviewWords(setId: Long, currentTime: Long): Flow<List<Word>> =
        wordDao.getDueReviewWords(setId = setId, currentTime = currentTime)

    suspend fun getIntroduceWordsForLevel(
        setId: Long,
        userLevel: String,
        limit: Int,
    ): List<Word> {
        if (limit <= 0) return emptyList()
        val allowed = CefrLevels.allowedUpTo(userLevel)
        return wordDao.getIntroduceWords(setId = setId, allowedLevels = allowed, limit = limit)
    }

    suspend fun countIntroducePoolForLevel(setId: Long, userLevel: String): Int {
        val allowed = CefrLevels.allowedUpTo(userLevel)
        return wordDao.countIntroducePool(setId = setId, allowedLevels = allowed)
    }

    suspend fun getPendingIntroductionRetries(
        setId: Long,
        userLevel: String,
        now: Long = System.currentTimeMillis(),
    ): List<Word> {
        val allowed = CefrLevels.allowedUpTo(userLevel)
        return wordDao.getPendingIntroductionRetries(setId = setId, allowedLevels = allowed, now = now)
    }

    fun getPracticeWords(setId: Long, now: Long = System.currentTimeMillis()): Flow<List<Word>> =
        wordDao.getPracticeWords(setId = setId, now = now)

    suspend fun existsInSet(setId: Long, word: String): Boolean =
        wordDao.existsInSet(setId = setId, word = word)

    suspend fun findInSet(setId: Long, word: String): Word? =
        wordDao.findInSet(setId = setId, word = word)

    suspend fun insertWord(word: Word): Long {
        val prepared = WordNorm.withNorm(word)
        val existing = wordDao.findInSet(prepared.setId, prepared.word)
        if (existing != null) {
            val merged = prepared.copy(id = existing.id)
            wordDao.updateWord(merged)
            syncWordToCloud(merged)
            return existing.id
        }
        val insertedId = wordDao.insertWord(prepared)
        val uid = firebaseAuth?.currentUser?.uid ?: return insertedId
        pushWordToFirestore(uid, prepared.copy(id = insertedId))
        return insertedId
    }

    suspend fun insertWords(words: List<Word>): List<Long> {
        if (words.isEmpty()) return emptyList()
        val insertedIds = mutableListOf<Long>()
        val pendingKeys = mutableSetOf<String>()
        for (raw in words) {
            val prepared = WordNorm.withNorm(raw)
            val key = "${prepared.setId}:${prepared.wordNorm}"
            if (!pendingKeys.add(key)) continue

            val existing = wordDao.findInSet(prepared.setId, prepared.word)
            if (existing != null) {
                insertedIds.add(existing.id)
                continue
            }

            val id = wordDao.insertWord(prepared)
            insertedIds.add(id)
            val uid = firebaseAuth?.currentUser?.uid
            if (uid != null) {
                pushWordToFirestore(uid, prepared.copy(id = id))
            }
        }
        return insertedIds
    }

    suspend fun updateWordLocal(word: Word) {
        wordDao.updateWord(WordNorm.withNorm(word))
    }

    /** Best-effort cloud sync; failures are logged and do not propagate. */
    suspend fun syncWordToCloud(word: Word) {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        try {
            pushWordToFirestore(uid, WordNorm.withNorm(word))
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed for word id=${word.id}", e)
        }
    }

    private companion object {
        const val TAG = "WordRepository"
    }

    suspend fun deleteWord(word: Word) {
        wordDao.deleteWord(word)
        val uid = firebaseAuth?.currentUser?.uid ?: return
        withContext(Dispatchers.IO) {
            firestore.collection("users").document(uid)
                .collection("words").document(word.id.toString())
                .delete()
                .await()
        }
    }

    private suspend fun pushWordToFirestore(uid: String, word: Word) = withContext(Dispatchers.IO) {
        if (word.id <= 0L) return@withContext
        val docId = word.id.toString()
        firestore.collection("users").document(uid)
            .collection("words").document(docId)
            .set(WordNorm.withNorm(word))
            .await()
    }

    /**
     * Pull words from Firestore and upsert them into Room.
     */
    suspend fun pullWordsFromFirestore(uid: String) = withContext(Dispatchers.IO) {
        val snapshot = firestore.collection("users").document(uid)
            .collection("words").get().await()

        val wordsToInsert = snapshot.documents.mapNotNull { doc ->
            val remote = doc.toObject<Word>() ?: return@mapNotNull null
            val remoteId = doc.id.toLongOrNull() ?: remote.id
            WordNorm.withNorm(remote.copy(id = remoteId))
        }

        val toInsert = mutableListOf<Word>()
        val pendingKeys = mutableSetOf<String>()
        for (w in wordsToInsert) {
            if (w.setId <= 0L || w.word.isBlank()) continue
            val key = "${w.setId}:${w.wordNorm}"
            val existing = wordDao.findInSet(w.setId, w.word)
            if (existing == null) {
                if (pendingKeys.add(key)) {
                    toInsert.add(w)
                }
                continue
            }
            pendingKeys.add(key)
            val remoteReviewed = w.lastReviewed ?: 0L
            val localReviewed = existing.lastReviewed ?: 0L
            if (remoteReviewed >= localReviewed) {
                wordDao.updateWord(WordNorm.withNorm(w.copy(id = existing.id)))
            }
        }

        if (toInsert.isNotEmpty()) {
            wordDao.insertWords(toInsert)
        }
    }

    suspend fun syncFromCurrentUser() {
        val uid = firebaseAuth?.currentUser?.uid ?: return
        pullWordsFromFirestore(uid)
    }
}
