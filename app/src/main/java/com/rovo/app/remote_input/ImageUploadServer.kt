package com.rovo.app.remote_input

import android.os.Handler
import android.os.Looper
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * HTTP server that serves a mobile-friendly image upload form
 * and receives the uploaded image file.
 */
class ImageUploadServer(
    port: Int,
    private val tempFolder: File,
    private val onImageUploaded: (File) -> Unit
) : NanoHTTPD(port) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val csrfToken = UUID.randomUUID().toString()

    companion object {
        private const val TAG = "ImageUploadServer"
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.uri == "/ping") return DisconnectBanner.pingResponse()
        return when (session.method) {
            Method.GET -> serveForm()
            Method.POST -> handleUpload(session)
            else -> newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }
    }

    private fun serveForm(): Response {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Upload Image</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background-color: #121212;
                        color: #ffffff;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .container {
                        background-color: #1e1e1e;
                        border-radius: 16px;
                        padding: 32px 24px;
                        width: 100%;
                        max-width: 400px;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.3);
                        text-align: center;
                    }
                    h1 {
                        color: #fff;
                        font-size: 1.5rem;
                        font-weight: 600;
                        margin-bottom: 0.5rem;
                    }
                    p {
                        color: #aaaaaa;
                        font-size: 14px;
                        margin-bottom: 24px;
                    }
                    .upload-area {
                        border: 2px dashed #333;
                        border-radius: 12px;
                        padding: 40px 20px;
                        margin-bottom: 24px;
                        cursor: pointer;
                        transition: border-color 0.2s;
                    }
                    .upload-area:hover {
                        border-color: #555;
                    }
                    #fileInput {
                        display: none;
                    }
                    .preview-container {
                        display: none;
                        margin-bottom: 24px;
                    }
                    #preview {
                        max-width: 100%;
                        max-height: 300px;
                        border-radius: 8px;
                    }
                    button {
                        width: 100%;
                        padding: 14px 24px;
                        font-size: 1rem;
                        font-weight: 600;
                        border: none;
                        border-radius: 24px;
                        background-color: #ffffff;
                        color: #000000;
                        cursor: pointer;
                        transition: transform 0.1s, opacity 0.2s;
                    }
                    button:active {
                        transform: scale(0.98);
                    }
                    button:disabled {
                        opacity: 0.6;
                        cursor: not-allowed;
                    }
                    .success {
                        display: none;
                        color: #10b981;
                    }
                    .success svg {
                        width: 64px;
                        height: 64px;
                        margin-bottom: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="container" id="main-container">
                    <div id="upload-form">
                        <h1>🖼️ Upload Image</h1>
                        <p>Choose an image from your phone to send to the TV</p>
                        
                        <div class="upload-area" onclick="document.getElementById('fileInput').click()">
                            <div id="upload-prompt">
                                <span style="font-size: 40px;">Tap to select</span>
                            </div>
                            <div class="preview-container" id="preview-container">
                                <img id="preview" src="" alt="Preview">
                            </div>
                        </div>

                        <form id="imageForm">
                            <input type="hidden" name="csrf_token" value="$csrfToken">
                            <input type="file" id="fileInput" name="image" accept="image/*" required>
                            <button type="submit" id="submitBtn" disabled>Upload to TV</button>
                        </form>
                    </div>

                    <div class="success" id="success-message">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                            <polyline points="22 4 12 14.01 9 11.01"/>
                        </svg>
                        <h1>Uploaded!</h1>
                        <p>Check your TV to see the new image.</p>
                    </div>
                </div>

                <script>
                    const fileInput = document.getElementById('fileInput');
                    const preview = document.getElementById('preview');
                    const previewContainer = document.getElementById('preview-container');
                    const uploadPrompt = document.getElementById('upload-prompt');
                    const submitBtn = document.getElementById('submitBtn');

                    fileInput.addEventListener('change', function() {
                        const file = this.files[0];
                        if (file) {
                            const reader = new FileReader();
                            reader.onload = function(e) {
                                preview.src = e.target.result;
                                previewContainer.style.display = 'block';
                                uploadPrompt.style.display = 'none';
                                submitBtn.disabled = false;
                            }
                            reader.readAsDataURL(file);
                        }
                    });

                    document.getElementById('imageForm').addEventListener('submit', async (e) => {
                        e.preventDefault();
                        submitBtn.disabled = true;
                        submitBtn.textContent = 'Uploading...';

                        try {
                            const formData = new FormData();
                            formData.append('csrf_token', '$csrfToken');
                            formData.append('image', fileInput.files[0]);

                            const response = await fetch('/', {
                                method: 'POST',
                                body: formData
                            });

                            if (response.ok) {
                                document.getElementById('upload-form').style.display = 'none';
                                document.getElementById('success-message').style.display = 'block';
                            } else {
                                throw new Error('Upload failed');
                            }
                        } catch (err) {
                            alert('Failed to upload image. Please try again.');
                            submitBtn.disabled = false;
                            submitBtn.textContent = 'Upload to TV';
                        }
                    });
                </script>
                ${DisconnectBanner.htmlSnippet}
            </body>
            </html>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun handleUpload(session: IHTTPSession): Response {
        return try {
            val files = mutableMapOf<String, String>()
            session.parseBody(files)

            val token = session.parms["csrf_token"]
            if (token != csrfToken) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Invalid request")
            }

            // NanoHTTPD stores uploaded files in a map where the key is the field name
            // and the value is the path to a temporary file.
            val tempFilePath = files["image"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No image found")
            val tempFile = File(tempFilePath)
            
            // Create a more permanent temporary file in our app's cache
            val targetFile = File(tempFolder, "upload_${System.currentTimeMillis()}.jpg")
            tempFile.copyTo(targetFile, overwrite = true)

            mainHandler.post {
                onImageUploaded(targetFile)
            }

            newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "OK")
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error")
        }
    }
}
