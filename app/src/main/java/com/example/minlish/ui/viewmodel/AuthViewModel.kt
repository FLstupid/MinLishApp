package com.example.minlish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.minlish.data.model.User
import com.example.minlish.data.preferences.UserPreferencesRepository
import com.example.minlish.data.repository.UserRepository
import com.example.minlish.data.repository.VocabSetRepository
import com.example.minlish.logic.CefrLevels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class AuthOneTimeEvent {
    data object ProfileSaved : AuthOneTimeEvent()
    data class ProfileSaveError(val message: String) : AuthOneTimeEvent()
}

class AuthViewModel(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val vocabSetRepository: VocabSetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _oneTimeEvents = Channel<AuthOneTimeEvent>(Channel.BUFFERED)
    val oneTimeEvents = _oneTimeEvents.receiveAsFlow()

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = firebaseAuth.currentUser
    )

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    loadProfile(user)
                    viewModelScope.launch {
                        vocabSetRepository.deleteEmptyDefaultSets()
                    }
                } else {
                    _profile.value = null
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty() || !trimmedEmail.contains('@') || password.isBlank()) {
                _uiState.value = AuthUiState.Error("Vui lòng nhập email và mật khẩu hợp lệ.")
                return@launch
            }

            _uiState.value = AuthUiState.Loading
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState.Idle
            } catch (t: Throwable) {
                val message = when {
                    t.message?.contains("password", ignoreCase = true) == true -> "Sai mật khẩu."
                    t.message?.contains("user", ignoreCase = true) == true -> "Không tìm thấy tài khoản."
                    else -> (t.message?.takeIf { it.isNotBlank() } ?: "Đăng nhập thất bại.")
                }
                _uiState.value = AuthUiState.Error(message)
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty() || !trimmedEmail.contains('@')) {
                _uiState.value = AuthUiState.Error("Email không hợp lệ.")
                return@launch
            }
            if (password.length < 6) {
                _uiState.value = AuthUiState.Error("Mật khẩu phải có ít nhất 6 ký tự.")
                return@launch
            }

            _uiState.value = AuthUiState.Loading
            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    ensureFirestoreProfile(
                        User(
                            uid = user.uid,
                            email = user.email.orEmpty(),
                        ),
                    )
                }
                _uiState.value = AuthUiState.Idle
            } catch (t: Throwable) {
                val message = when {
                    t.message?.contains("exists", ignoreCase = true) == true ||
                        t.message?.contains("in use", ignoreCase = true) == true -> "Email đã được sử dụng."
                    t.message?.contains("weak", ignoreCase = true) == true -> "Mật khẩu quá yếu."
                    else -> (t.message?.takeIf { it.isNotBlank() } ?: "Đăng ký thất bại.")
                }
                _uiState.value = AuthUiState.Error(message)
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                exchangeGoogleToken(idToken)
            } catch (t: Throwable) {
                _uiState.value = AuthUiState.Error(
                    t.message?.takeIf { it.isNotBlank() } ?: "Đăng nhập Google thất bại.",
                )
            }
        }
    }

    fun onGoogleSignInFailed(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun signOut() {
        firebaseAuth.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun updateProfile(updated: User) {
        viewModelScope.launch {
            val firebaseUser = firebaseAuth.currentUser
            val uid = updated.uid.takeIf { it.isNotBlank() } ?: firebaseUser?.uid
            if (uid.isNullOrBlank()) {
                val msg = "Chưa đăng nhập — không thể lưu hồ sơ."
                _oneTimeEvents.send(AuthOneTimeEvent.ProfileSaveError(msg))
                return@launch
            }
            val toSave = updated.copy(
                uid = uid,
                email = updated.email.ifBlank { firebaseUser?.email.orEmpty() },
                level = CefrLevels.normalize(updated.level),
            )
            _uiState.value = AuthUiState.Loading
            try {
                userPreferencesRepository.saveProfile(toSave)
                userRepository.upsertUserProfile(toSave)
                _profile.value = toSave
                _uiState.value = AuthUiState.Idle
                _oneTimeEvents.send(AuthOneTimeEvent.ProfileSaved)
            } catch (t: Throwable) {
                val msg = t.message?.takeIf { it.isNotBlank() } ?: "Cập nhật hồ sơ thất bại."
                _uiState.value = AuthUiState.Error(msg)
                _oneTimeEvents.send(AuthOneTimeEvent.ProfileSaveError(msg))
            }
        }
    }

    private suspend fun exchangeGoogleToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user
        if (user != null && userRepository.getUserProfile(user.uid) == null) {
            ensureFirestoreProfile(
                User(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    name = user.displayName.orEmpty(),
                ),
            )
        }
        _uiState.value = AuthUiState.Idle
    }

    private suspend fun ensureFirestoreProfile(profile: User) {
        try {
            userRepository.upsertUserProfile(profile)
            userPreferencesRepository.saveProfile(profile)
        } catch (_: Throwable) {
            // Best-effort on first sign-in; user can save again from Profile tab.
        }
    }

    private fun loadProfile(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            val email = firebaseUser.email.orEmpty()
            val displayName = firebaseUser.displayName.orEmpty()
            val uid = firebaseUser.uid
            try {
                val local = userPreferencesRepository.loadProfileForUser(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                )
                val remote = userRepository.getUserProfile(uid)
                val merged = when {
                    remote != null -> remote.copy(
                        uid = uid,
                        email = remote.email.ifBlank { email },
                        level = CefrLevels.normalize(remote.level),
                    )
                    else -> local
                }
                userPreferencesRepository.saveProfile(merged)
                _profile.value = merged
            } catch (_: Throwable) {
                _profile.value = User(
                    uid = uid,
                    email = email,
                    name = displayName,
                )
            }
        }
    }
}

class AuthViewModelFactory(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val vocabSetRepository: VocabSetRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                firebaseAuth = firebaseAuth,
                userRepository = userRepository,
                userPreferencesRepository = userPreferencesRepository,
                vocabSetRepository = vocabSetRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
