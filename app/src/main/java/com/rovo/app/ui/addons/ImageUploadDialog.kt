package com.rovo.app.ui.addons

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rovo.app.remote_input.ImageUploadServerManager
import com.rovo.app.remote_input.ServerInfo
import kotlinx.coroutines.delay
import java.io.File

/**
 * Dialog that displays a QR code for local image upload.
 * Creates a local server and shows QR code pointing to the TV's local IP.
 */
@Composable
fun ImageUploadDialog(
    onDismissRequest: () -> Unit,
    onImageUploaded: (File) -> Unit
) {
    var serverInfo by remember { mutableStateOf<ServerInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val context = LocalContext.current
    val serverManager = remember { ImageUploadServerManager(context) }
    val focusRequester = remember { FocusRequester() }

    // Start server when dialog opens
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        
        val tempFolder = File(context.cacheDir, "remote_uploads").apply { if (!exists()) mkdirs() }
        
        val info = serverManager.startServer(tempFolder) { uploadedFile ->
            onImageUploaded(uploadedFile)
            onDismissRequest()
        }
        
        if (info != null) {
            serverInfo = info
            qrBitmap = generateQrCode(info.url)
        } else {
            error = "Could not start local server. Check your network."
        }
    }

    Dialog(onDismissRequest = {
        serverManager.stopServer()
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
                    "Upload Image",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    "Scan with your phone to upload an image",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(Modifier.height(32.dp))

                when {
                    error != null -> {
                        Text(
                            error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    qrBitmap != null && serverInfo != null -> {
                        // QR Code
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
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // PIN Display
                        serverInfo!!.pin?.let { pin ->
                            Text(
                                "Enter this PIN on your phone:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                pin,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 8.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Manual URL
                        Text(
                            "Or visit:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            serverInfo!!.url,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    else -> {
                        // Loading
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Starting local server...",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
