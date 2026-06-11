package com.example.minlish.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlish.R
import com.example.minlish.data.model.Word

@Composable
fun Flashcard(
    word: Word,
    modifier: Modifier = Modifier,
) {
    var rotated by remember(word.id) { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "CardRotation"
    )

    val tapHint = stringResource(R.string.learn_tap_flip)
    val frontContentDesc = word.word + word.pronunciation?.let { " ($it)" }.orEmpty()
    val backContentDesc = word.meaning + word.example?.let { ". $it" }.orEmpty()
    val cardContentDesc = if (rotation <= 90f) frontContentDesc else backContentDesc

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 360.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .semantics {
                contentDescription = "$cardContentDesc. $tapHint"
            }
            .clickable { rotated = !rotated },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front Side
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = word.word,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    word.pronunciation?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                // Back Side
                Column(
                    modifier = Modifier
                        .graphicsLayer { rotationY = 180f }
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = word.meaning,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    word.example?.let {
                        Text(
                            text = "\"$it\"",
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
