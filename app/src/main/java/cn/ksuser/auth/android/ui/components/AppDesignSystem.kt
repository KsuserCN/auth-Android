package cn.ksuser.auth.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.ksuser.auth.android.ui.theme.BrandButtonGradientEnd
import cn.ksuser.auth.android.ui.theme.BrandButtonGradientStart
import kotlinx.coroutines.launch

object AppSpacing {
    val S8: Dp = 6.dp
    val S12: Dp = 10.dp
    val S16: Dp = 12.dp
    val S20: Dp = 16.dp
    val S24: Dp = 20.dp
    val S32: Dp = 24.dp
}

object AppRadius {
    val R8: Dp = 6.dp
    val R12: Dp = 10.dp
    val R16: Dp = 14.dp
    val R20: Dp = 16.dp
    val R24: Dp = 20.dp
}

val AppPagePadding = AppSpacing.S12

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.S12),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.R16),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f),
                ),
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.S8),
        ) {
            content()
        }
    }
}

@Composable
fun BrandHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.R20),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        ),
                    ),
                )
                .padding(AppSpacing.S20),
        ) {
            content()
        }
    }
}

@Composable
fun AppOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        label = label,
        singleLine = singleLine,
        shape = RoundedCornerShape(AppRadius.R12),
        textStyle = textStyle,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun LoadingButtonContent(
    text: String,
    isLoading: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(AppSpacing.S8))
        }
        Text(text)
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val disabledColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(AppRadius.R12)),
        shape = RoundedCornerShape(AppRadius.R12),
        contentPadding = PaddingValues(0.dp),
        colors = disabledColors,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(BrandButtonGradientStart, BrandButtonGradientEnd))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            ),
                        )
                    },
                )
                .padding(vertical = AppSpacing.S8),
            contentAlignment = Alignment.Center,
        ) {
            Text(text)
        }
    }
}

@Composable
fun LoadingButton(
    text: String,
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    Button(
        onClick = {
            if (isLoading) return@Button
            scope.launch {
                isLoading = true
                try {
                    onClick()
                } finally {
                    isLoading = false
                }
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier.heightIn(min = 42.dp),
        shape = RoundedCornerShape(AppRadius.R12),
    ) {
        LoadingButtonContent(text = text, isLoading = isLoading)
    }
}

@Composable
fun LoadingOutlinedButton(
    text: String,
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = {
            if (isLoading) return@OutlinedButton
            scope.launch {
                isLoading = true
                try {
                    onClick()
                } finally {
                    isLoading = false
                }
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier.heightIn(min = 42.dp),
        shape = RoundedCornerShape(AppRadius.R12),
    ) {
        LoadingButtonContent(text = text, isLoading = isLoading)
    }
}

@Composable
fun LoadingTextButton(
    text: String,
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    TextButton(
        onClick = {
            if (isLoading) return@TextButton
            scope.launch {
                isLoading = true
                try {
                    onClick()
                } finally {
                    isLoading = false
                }
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        LoadingButtonContent(text = text, isLoading = isLoading)
    }
}
