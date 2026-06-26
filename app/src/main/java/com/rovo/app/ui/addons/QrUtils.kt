package com.rovo.app.ui.addons

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Generates a QR code bitmap for the given URL.
 */
internal fun generateQrCode(url: String, size: Int = 512): Bitmap? {
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
        Log.w("QrUtils", "QR generation error", e)
        null
    }
}
