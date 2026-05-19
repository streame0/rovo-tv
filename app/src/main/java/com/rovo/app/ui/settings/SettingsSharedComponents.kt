package com.rovo.app.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * Shared UI components used by IntegrationsScreen and AccountScreen.
 */

@Composable
internal fun IntegrationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(if (isFocused && enabled) 1.05f else 1f)

    val activeColor = when {
        isDestructive -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }

    val bgColor = if (!enabled) Color.White.copy(0.05f) else Color.White.copy(0.08f)

    val textColor = when {
        !enabled -> Color.White.copy(0.3f)
        isFocused -> activeColor
        isDestructive || isPrimary -> activeColor.copy(alpha = 0.95f)
        else -> Color.White
    }

    val borderColor = when {
        !enabled -> Color.White.copy(0.1f)
        isFocused -> activeColor
        isDestructive || isPrimary -> activeColor.copy(alpha = 0.75f)
        else -> Color.White.copy(0.2f)
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(interactionSource = interactionSource, indication = null) { onClick() } else Modifier)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .then(if (enabled) Modifier.focusable(interactionSource = interactionSource) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
internal fun IntegrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    onDone: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderBrush = if (isFocused) {
        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
    } else {
        SolidColor(Color.White.copy(0.1f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(0.5f))
            .border(if (isFocused) 2.dp else 1.dp, borderBrush, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = Color.Gray)
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}

@Composable
internal fun CloudAuthDialog(
    title: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
    isSignUp: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val emailFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        runCatching { emailFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(Modifier.height(20.dp))

                IntegrationTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                    focusRequester = emailFocusRequester,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                IntegrationTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    placeholder = "Password",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSignUp) {
                    Spacer(Modifier.height(12.dp))
                    IntegrationTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = null },
                        placeholder = "Confirm Password",
                        isPassword = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Color.Red.copy(0.8f), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    IntegrationButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.width(100.dp))
                    IntegrationButton(
                        text = if (isLoading) "Please wait..." else if (isSignUp) "Create" else "Sign In",
                        onClick = {
                            when {
                                email.isBlank() || password.isBlank() -> error = "Please fill in all fields"
                                isSignUp && password != confirmPassword -> error = "Passwords do not match"
                                password.length < 6 -> error = "Password must be at least 6 characters"
                                else -> onSubmit(email, password)
                            }
                        },
                        isPrimary = true,
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }
    }
}

/**
 * Block Up navigation to prevent focus escaping.
 */
@Composable
internal fun upBlockModifier(): Modifier = Modifier.onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
        true
    } else false
}
