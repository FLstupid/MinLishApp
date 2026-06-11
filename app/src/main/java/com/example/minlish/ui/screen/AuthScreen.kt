package com.example.minlish.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.minlish.R
import com.example.minlish.logic.auth.GoogleIdTokenProvider
import com.example.minlish.ui.viewmodel.AuthUiState
import com.example.minlish.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    webClientId: String,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleIdTokenProvider = remember(webClientId) { GoogleIdTokenProvider(webClientId) }

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // systemBarsPadding() đảm bảo nội dung không đè lên camera/notch
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (isSignUp) stringResource(R.string.auth_create_account) else stringResource(R.string.auth_welcome_back),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_email_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = MaterialTheme.shapes.large
        )

        if (uiState is AuthUiState.Error) {
            val errorMessage = (uiState as AuthUiState.Error).message
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        if (uiState is AuthUiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState !is AuthUiState.Loading,
            onClick = {
                val trimmedEmail = email.trim()
                if (isSignUp) viewModel.signUpWithEmail(trimmedEmail, password)
                else viewModel.signInWithEmail(trimmedEmail, password)
            },
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                if (isSignUp) stringResource(R.string.auth_sign_up) else stringResource(R.string.auth_sign_in),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        TextButton(
            onClick = { isSignUp = !isSignUp },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                if (isSignUp) stringResource(R.string.auth_has_account) else stringResource(R.string.auth_no_account),
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.auth_or_divider),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = {
                scope.launch {
                    googleIdTokenProvider.getIdToken(context).fold(
                        onSuccess = { viewModel.signInWithGoogleIdToken(it) },
                        onFailure = { e ->
                            viewModel.onGoogleSignInFailed(
                                e.message?.takeIf { it.isNotBlank() }
                                    ?: context.getString(R.string.auth_google_sign_in_failed),
                            )
                        },
                    )
                }
            },
            enabled = uiState !is AuthUiState.Loading,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.auth_continue_with_google), style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(24.dp))
    }
}
