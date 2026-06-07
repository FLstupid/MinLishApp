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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
            text = "MinLish",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (isSignUp) "Tạo tài khoản mới" else "Chào mừng trở lại",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mật khẩu") },
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
            Text(if (isSignUp) "Đăng ký" else "Đăng nhập", style = MaterialTheme.typography.titleMedium)
        }

        TextButton(
            onClick = { isSignUp = !isSignUp },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(if (isSignUp) "Đã có tài khoản? Đăng nhập ngay" else "Chưa có tài khoản? Đăng ký ngay")
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "HOẶC",
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
                                e.message?.takeIf { it.isNotBlank() } ?: "Đăng nhập Google thất bại.",
                            )
                        },
                    )
                }
            },
            enabled = uiState !is AuthUiState.Loading,
            shape = MaterialTheme.shapes.large,
        ) {
            Text("Tiếp tục với Google", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(24.dp))
    }
}
