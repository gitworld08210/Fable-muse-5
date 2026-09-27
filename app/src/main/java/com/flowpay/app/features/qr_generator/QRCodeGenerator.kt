// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.flowpay.app.features.qr_generator

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.flowpay.app.data.UPIData
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.EnumMap

/**
 * QR code generation utility for Flowpay using ZXing.
 *
 * Generates standard NPCI-compliant UPI payment QR codes (`upi://pay?...`)
 * as high-resolution Bitmaps and BitMatrix objects for offline scanning
 * and device-to-device sharing.
 */
object QRCodeGenerator {

    private const val DEFAULT_QR_SIZE = 512
    private const val DEFAULT_MARGIN = 2
    private const val QR_CACHE_DIR = "qr_codes"
    private const val SHARED_QR_FILENAME = "flowpay_payment_qr.png"

    /**
     * Constructs a standard NPCI UPI URI string from the payment fields.
     *
     * Example output:
     * `upi://pay?pa=merchant@upi&pn=Merchant%20Name&am=150.00&cu=INR&tn=Offline%20Payment`
     */
    fun buildUpiUri(
        vpa: String,
        payeeName: String,
        amount: String? = null,
        transactionNote: String? = null,
        currency: String = "INR"
    ): String {
        val trimmedVpa = vpa.trim()
        require(trimmedVpa.isNotEmpty()) { "VPA cannot be empty" }

        val builder = StringBuilder("upi://pay?")
        builder.append("pa=").append(URLEncoder.encode(trimmedVpa, "UTF-8"))

        val trimmedName = payeeName.trim()
        if (trimmedName.isNotEmpty()) {
            builder.append("&pn=").append(URLEncoder.encode(trimmedName, "UTF-8"))
        }

        val trimmedAmount = amount?.trim()
        if (!trimmedAmount.isNullOrEmpty()) {
            val numericAmount = trimmedAmount.toDoubleOrNull()
            if (numericAmount != null && numericAmount > 0.0) {
                val formattedAmount = if (numericAmount % 1.0 == 0.0) {
                    numericAmount.toInt().toString()
                } else {
                    String.format(java.util.Locale.US, "%.2f", numericAmount)
                }
                builder.append("&am=").append(formattedAmount)
            }
        }

        val trimmedCurrency = currency.trim()
        if (trimmedCurrency.isNotEmpty()) {
            builder.append("&cu=").append(URLEncoder.encode(trimmedCurrency, "UTF-8"))
        }

        val trimmedNote = transactionNote?.trim()
        if (!trimmedNote.isNullOrEmpty()) {
            builder.append("&tn=").append(URLEncoder.encode(trimmedNote, "UTF-8"))
        }

        return builder.toString()
    }

    /**
     * Builds a UPI URI from a [UPIData] model.
     */
    fun buildUpiUri(upiData: UPIData): String {
        return buildUpiUri(
            vpa = upiData.vpa,
            payeeName = upiData.payeeName,
            amount = upiData.amount.takeIf { it.isNotEmpty() },
            transactionNote = upiData.transactionNote.takeIf { it.isNotEmpty() },
            currency = upiData.currency.ifEmpty { "INR" }
        )
    }

    /**
     * Encodes arbitrary text content into a ZXing [BitMatrix].
     */
    fun generateBitMatrix(
        content: String,
        size: Int = DEFAULT_QR_SIZE,
        margin: Int = DEFAULT_MARGIN,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ): BitMatrix {
        require(content.isNotEmpty()) { "Content to encode cannot be empty" }
        require(size > 0) { "QR Code size must be greater than zero" }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, errorCorrection)
            put(EncodeHintType.MARGIN, margin)
        }

        return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    }

    /**
     * Encodes arbitrary text content into an Android [Bitmap] using ZXing.
     *
     * @param content The text/URI to encode into QR code.
     * @param size Width and height of the generated bitmap in pixels.
     * @param foregroundColor Color of the QR dark modules (default Black).
     * @param backgroundColor Color of the QR light background (default White).
     */
    fun generateQrCodeBitmap(
        content: String,
        size: Int = DEFAULT_QR_SIZE,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val bitMatrix = generateBitMatrix(content, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Generates a QR Code bitmap directly from a [UPIData] payment model.
     */
    fun generatePaymentQrBitmap(
        upiData: UPIData,
        size: Int = DEFAULT_QR_SIZE,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val upiUri = buildUpiUri(upiData)
        return generateQrCodeBitmap(upiUri, size, foregroundColor, backgroundColor)
    }

    /**
     * Generates a QR Code bitmap from individual payment details.
     */
    fun generatePaymentQrBitmap(
        vpa: String,
        payeeName: String,
        amount: String? = null,
        transactionNote: String? = null,
        size: Int = DEFAULT_QR_SIZE,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val upiUri = buildUpiUri(vpa, payeeName, amount, transactionNote)
        return generateQrCodeBitmap(upiUri, size, foregroundColor, backgroundColor)
    }

    /**
     * Saves a generated QR Bitmap into the app's cache directory and returns a content Uri
     * backed by [FileProvider] for sharing.
     */
    fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        fileName: String = SHARED_QR_FILENAME
    ): Uri {
        val cacheFolder = File(context.cacheDir, QR_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        val file = File(cacheFolder, fileName)
        FileOutputStream(file).use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
        }
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Creates an [Intent] to share the payment QR code image and payment details
     * via the Android system share sheet.
     */
    fun createShareIntent(
        context: Context,
        bitmap: Bitmap,
        upiData: UPIData
    ): Intent {
        val imageUri = saveBitmapToCache(context, bitmap)

        val shareMessage = buildString {
            append("Scan this QR code offline with Flowpay or any UPI app to pay:\n")
            append("UPI ID: ${upiData.vpa}\n")
            if (upiData.payeeName.isNotBlank()) {
                append("Name: ${upiData.payeeName}\n")
            }
            if (upiData.amount.isNotBlank()) {
                append("Amount: ₹${upiData.amount}\n")
            }
            if (upiData.transactionNote.isNotBlank()) {
                append("Note: ${upiData.transactionNote}\n")
            }
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            putExtra(Intent.EXTRA_SUBJECT, "Flowpay Payment QR Code")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
