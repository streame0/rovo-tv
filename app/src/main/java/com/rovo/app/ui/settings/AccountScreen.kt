package com.rovo.app.ui.settings

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.rovo.app.data.model.ProfileEntity
import com.rovo.app.data.supabase.CloudAuthState
import com.rovo.app.data.supabase.CompanionSessionManager
import com.rovo.app.data.supabase.SyncResult
import com.rovo.app.remote_input.CloudServerManager
import com.rovo.app.remote_input.ServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    currentProfile: ProfileEntity?,
    viewModel: IntegrationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showQrDialog by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var showSignUpDialog by remember { mutableStateOf(false) }

    val cloudConnected = state.cloudAuthState !is CloudAuthState.Unauthenticated
    val cloudUser = when (val cs = state.cloudAuthState) {
        is CloudAuthState.Authenticated -> cs.email
        is CloudAuthState.Anonymous -> "Anonymous"
        is CloudAuthState.Unauthenticated -> null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is IntegrationsEvent.CloudAuthResult -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is IntegrationsEvent.CloudSyncComplete -> {
                    val r = event.result
                    Toast.makeText(
                        context,
                        "Synced: ${r.pushed} pushed, ${r.pulled} pulled${if (r.errors > 0) ", ${r.errors} errors" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    val goBackModifier = Modifier.onPreviewKeyEvent {
        if (it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown) {
            onBack()
            true
        } else false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Account",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            color = Color.White
        )
        Text(
            if (cloudConnected) "Connected to cloud sync"
            else "Sign in to sync across devices",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = Color.White.copy(0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(32.dp))

        // ── Connection status card ──
        AccountStatusCard(
            isConnected = cloudConnected,
            userLabel = cloudUser,
            profileName = currentProfile?.name ?: "Default",
            modifier = goBackModifier
        )

        Spacer(Modifier.height(24.dp))

        if (cloudConnected) {
            // ── Sync info ──
            if (state.cloudLastSyncResult != null) {
                SyncInfoCard(state.cloudLastSyncResult!!)
                Spacer(Modifier.height(16.dp))
            }

            // ── Sync Now ──
            AccountActionButton(
                icon = Icons.Default.Sync,
                title = "Sync Now",
                subtitle = if (state.cloudIsSyncing) "Syncing..." else "Push & pull all data",
                onClick = { viewModel.runCloudSync() },
                enabled = !state.cloudIsSyncing
            )

            Spacer(Modifier.height(12.dp))

            // ── Logout ──
            AccountActionButton(
                icon = Icons.Default.Logout,
                title = "Disconnect & Sign Out",
                subtitle = "Sign out of this profile's cloud account",
                onClick = { viewModel.disableCloudSync() },
                isDestructive = true
            )
        } else {
            // ── QR Code Login ──
            AccountActionButton(
                icon = Icons.Default.Cloud,
                title = "Scan QR Code with Phone",
                subtitle = "Open the link on your phone to sign in or create an account",
                onClick = { showQrDialog = true }
            )

            Spacer(Modifier.height(12.dp))

            // ── Manual Sign In ──
            AccountActionButton(
                icon = Icons.Default.Cloud,
                title = "Sign In Manually",
                subtitle = "Enter email and password on TV",
                onClick = { showSignInDialog = true }
            )

            Spacer(Modifier.height(12.dp))

            // ── Sign Up ──
            AccountActionButton(
                icon = Icons.Default.Cloud,
                title = "Create Account",
                subtitle = "Don't have an account? Create one",
                onClick = { showSignUpDialog = true }
            )
        }
    }

    // ── Dialogs ──

    if (showQrDialog) {
        CloudQrDialog(
            onDismiss = { showQrDialog = false },
            onSignIn = { email, password ->
                viewModel.signInToCloud(email, password)
                showQrDialog = false
            },
            onSignUp = { email, password ->
                viewModel.signUpForCloud(email, password)
                showQrDialog = false
            }
        )
    }

    if (showSignInDialog) {
        CloudAuthDialog(
            title = "Sign In",
            isLoading = state.isLoading,
            onDismiss = { showSignInDialog = false },
            onSubmit = { email, password ->
                viewModel.signInToCloud(email, password)
                showSignInDialog = false
            }
        )
    }

    if (showSignUpDialog) {
        CloudAuthDialog(
            title = "Create Account",
            isLoading = state.isLoading,
            onDismiss = { showSignUpDialog = false },
            isSignUp = true,
            onSubmit = { email, password ->
                viewModel.signUpForCloud(email, password)
                showSignUpDialog = false
            }
        )
    }
}

// ── Components ──

@Composable
private fun AccountStatusCard(
    isConnected: Boolean,
    userLabel: String?,
    profileName: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isConnected) Color(0xFF1A3A1A) else Color.White.copy(0.05f)
    val borderColor = if (isConnected) Color(0xFF2ECC71).copy(0.3f) else Color.White.copy(0.1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF2ECC71).copy(0.2f) else Color.White.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Check else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isConnected) Color(0xFF2ECC71) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profileName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = if (isConnected) "Cloud: $userLabel" else "Not connected to cloud",
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected) Color(0xFF2ECC71) else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SyncInfoCard(result: SyncResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.05f))
            .padding(16.dp)
    ) {
        Text(
            "Last Sync",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "↑ ${result.pushed} items pushed  ·  ↓ ${result.pulled} items pulled${if (result.errors > 0) "  ·  ⚠ ${result.errors} errors" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun AccountActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(if (isFocused && enabled) 1.02f else 1f)
    val borderColor by animateColorAsState(
        if (isFocused && enabled) MaterialTheme.colorScheme.primary
        else if (isDestructive) Color.Red.copy(0.3f)
        else Color.White.copy(0.1f)
    )
    val bgColor = Color.White.copy(0.05f)

    val clickableMod = if (enabled) {
        Modifier.clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
    } else Modifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isFocused && enabled) 2.dp else 0.dp, borderColor, RoundedCornerShape(12.dp))
            .then(clickableMod)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color.Red else if (isFocused && enabled) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(28.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDestructive) Color.Red else Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── QR Dialog ──

@Composable
private fun CloudQrDialog(
    onDismiss: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String) -> Unit
) {
    var sessionCode by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val result = CompanionSessionManager.createSession("auth")
        if (result.isFailure) {
            error = result.exceptionOrNull()?.message
            return@LaunchedEffect
        }
        val code = result.getOrThrow()
        sessionCode = code

        val url = "${CompanionSessionManager.BASE_URL}?code=$code"
        qrBitmap = withContext(Dispatchers.IO) { generateQrCode(url) }

        CompanionSessionManager.observeSession(code).collect { session ->
            val data = session.data
            val accessToken = data?.get("access_token")?.toString() ?: ""
            val refreshToken = data?.get("refresh_token")?.toString() ?: ""

            if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                val email = data?.get("email")?.toString() ?: ""
                onSignIn(email, "companion_session_token")
                onDismiss()
            } else if (session.status == "expired") {
                error = "Session expired"
            }
        }
    }

    Dialog(
        onDismissRequest = {
            sessionCode?.let { scope.launch { CompanionSessionManager.deleteSession(it) } }
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Connect Your Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Scan the QR code with your phone to sign in or create an account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(24.dp))

                when {
                    error != null -> {
                        Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                    qrBitmap != null && sessionCode != null -> {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "${CompanionSessionManager.BASE_URL}?code=$sessionCode",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(24.dp))

                        Text(
                            "☁️ Sign in or create an account on your phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                IntegrationButton(
                    text = "Cancel",
                    onClick = {
                        sessionCode?.let { scope.launch { CompanionSessionManager.deleteSession(it) } }
                        onDismiss()
                    },
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}

private fun generateQrCode(url: String, size: Int = 400): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
