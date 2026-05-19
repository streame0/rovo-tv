package com.rovo.app.ui.profiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.rovo.app.BuildConfig
import com.rovo.app.data.supabase.CompanionSessionManager
import com.rovo.app.data.supabase.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun AvatarUploadDialog(
    onDismissRequest: () -> Unit,
    onAvatarReceived: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var sessionCode by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Starting...") }
    var sessionData by remember { mutableStateOf<SessionStatus?>(null) }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()

        val result = CompanionSessionManager.createSession("avatar")
        if (result.isFailure) {
            error = "Failed to create session: ${result.exceptionOrNull()?.message}"
            return@LaunchedEffect
        }

        val code = result.getOrThrow()
        sessionCode = code
        statusText = "Code: $code"

        val url = "${CompanionSessionManager.BASE_URL}?code=$code"
        qrBitmap = withContext(Dispatchers.IO) {
            generateQrCode(url)
        }

        CompanionSessionManager.observeSession(code).collect { session ->
            sessionData = session
            when (session.status) {
                "completed" -> {
                    val data = session.data
                    val imageUrl = data?.get("image_url")?.toString()
                    if (imageUrl != null) {
                        scope.launch {
                            downloadAndSaveImage(context, imageUrl)?.let { path ->
                                onAvatarReceived(path)
                            }
                            onDismissRequest()
                        }
                    } else {
                        onDismissRequest()
                    }
                }
                "expired" -> {
                    error = "Session expired. Please try again."
                }
            }
        }
    }

    Dialog(onDismissRequest = {
        sessionCode?.let { scope.launch { CompanionSessionManager.deleteSession(it) } }
        onDismissRequest()
    }) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                .padding(32.dp)
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Upload Avatar",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    "Scan with your phone to upload a picture",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(Modifier.height(32.dp))

                when {
                    error != null -> {
                        Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                    qrBitmap != null && sessionCode != null -> {
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

                        Spacer(Modifier.height(24.dp))

                        Text("Or visit:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            "${CompanionSessionManager.BASE_URL}?code=$sessionCode",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        val s = sessionData
                        if (s != null && s.status == "pending") {
                            Text(
                                "Waiting for phone...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    else -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(statusText, color = Color.Gray)
                    }
                }
            }
        }
    }
}

private suspend fun downloadAndSaveImage(context: Context, imageUrl: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder().url(imageUrl).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val bytes = response.body?.bytes() ?: return@withContext null

            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) avatarsDir.mkdirs()

            val fileName = "avatar_${UUID.randomUUID()}.png"
            val file = File(avatarsDir, fileName)
            FileOutputStream(file).use { fos -> fos.write(bytes) }

            "custom:${file.absolutePath}"
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("AvatarUpload", "Download error", e)
            null
        }
    }

private fun generateQrCode(url: String, size: Int = 512): Bitmap? {
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
        if (BuildConfig.DEBUG) android.util.Log.w("AvatarUploadDialog", "QR generation error", e)
        null
    }
}
