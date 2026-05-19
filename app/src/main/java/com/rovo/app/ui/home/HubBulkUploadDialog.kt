package com.rovo.app.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.rovo.app.data.model.HubRowItemEntity
import com.rovo.app.data.supabase.CompanionSessionManager
import com.rovo.app.domain.HubShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HubBulkUploadDialog(
    items: List<HubRowItemEntity>,
    shape: HubShape,
    onDismiss: () -> Unit,
    onImageReceived: (String, ByteArray) -> Unit,
    onImageUrlReceived: ((String, String) -> Unit)? = null,
    onImageDeleted: ((String) -> Unit)? = null
) {
    var sessionCode by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val initialItems = remember(items, shape) {
        items.map { item ->
            mapOf<String, Any>(
                "config_unique_id" to item.configUniqueId,
                "title" to item.title,
                "custom_image_url" to ""
            )
        }
    }

    LaunchedEffect(Unit) {
        val sessionData = mapOf<String, Any>(
            "items" to initialItems,
            "shape" to shape.name.lowercase()
        )
        val result = CompanionSessionManager.createSession("hub", sessionData)
        result.onSuccess { code ->
            sessionCode = code
            val url = "${CompanionSessionManager.BASE_URL}/hub.html?code=$code"

            withContext(Dispatchers.IO) {
                try {
                    val writer = QRCodeWriter()
                    val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 512, 512)
                    val width = bitMatrix.width
                    val height = bitMatrix.height
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                        }
                    }
                    qrBitmap = bmp
                } catch (_: Exception) {}
            }
            isLoading = false
        }.onFailure {
            statusMessage = "Failed to create session: ${it.message}"
            isLoading = false
        }
    }

    val knownUrls = remember { mutableStateMapOf<String, String?>() }

    LaunchedEffect(sessionCode) {
        val code = sessionCode ?: return@LaunchedEffect
        while (isActive) {
            val session = CompanionSessionManager.pollSession(code)
            if (session?.data != null) {
                val itemsData = session.data["items"]
                if (itemsData is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    val sessionItems = itemsData as List<Map<String, Any?>>
                    for (item in sessionItems) {
                        val configId = item["config_unique_id"] as? String ?: continue
                        val currentUrl = item["custom_image_url"] as? String
                        val lastKnownUrl = knownUrls[configId]

                        if (currentUrl != null && currentUrl != lastKnownUrl && currentUrl.isNotEmpty()) {
                            knownUrls[configId] = currentUrl
                            onImageUrlReceived?.invoke(configId, currentUrl)
                        } else if (currentUrl == null && lastKnownUrl != null) {
                            knownUrls[configId] = null
                            onImageDeleted?.invoke(configId)
                        } else if (currentUrl != null && lastKnownUrl == null) {
                            knownUrls[configId] = currentUrl
                        }
                    }
                }
            }
            delay(2000)
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                sessionCode?.let { CompanionSessionManager.deleteSession(it) }
            }
        }
    }

    Dialog(onDismissRequest = {
        scope.launch {
            sessionCode?.let { CompanionSessionManager.deleteSession(it) }
        }
        onDismiss()
    }) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                .padding(32.dp)
                .focusRequester(focusRequester)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bulk Manage Images",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    text = "Scan to open the web portal on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Creating session...", color = Color.Gray)
                } else if (statusMessage != null) {
                    Text(
                        statusMessage!!,
                        color = Color(0xFFef4444),
                        textAlign = TextAlign.Center
                    )
                } else if (qrBitmap != null && sessionCode != null) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .padding(3.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Or visit:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "${CompanionSessionManager.BASE_URL}/hub.html?code=${sessionCode}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Press Back to Finish",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
