package com.example.minlish.logic.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleIdTokenProvider(
    private val webClientId: String,
) {
    suspend fun getIdToken(context: Context): Result<String> {
        val trimmedWebClientId = webClientId.trim()
        if (trimmedWebClientId.isEmpty()) {
            return Result.failure(
                IllegalStateException("Thiếu cấu hình Google. Vui lòng kiểm tra `google-services.json` và Web Client ID."),
            )
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(trimmedWebClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            val tokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            Result.success(tokenCredential.idToken)
        } catch (t: Throwable) {
            val message = t.message?.takeIf { it.isNotBlank() } ?: "Đăng nhập Google thất bại."
            Result.failure(IllegalStateException(message))
        }
    }
}

