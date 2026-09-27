// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.flowpay.app.features.qr_generator

import android.graphics.Bitmap
import com.flowpay.app.data.UPIData
import com.flowpay.app.features.qr_scanner.domain.QRCodeParser
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QRCodeGeneratorTest {

    @Test
    fun `buildUpiUri formats complete parameters properly`() {
        val uri = QRCodeGenerator.buildUpiUri(
            vpa = "merchant@okhdfcbank",
            payeeName = "Ravi Kumar",
            amount = "250.50",
            transactionNote = "Dinner bill",
            currency = "INR"
        )

        assertTrue(uri.startsWith("upi://pay?"))
        assertTrue(uri.contains("pa=merchant%40okhdfcbank"))
        assertTrue(uri.contains("pn=Ravi+Kumar"))
        assertTrue(uri.contains("am=250.50"))
        assertTrue(uri.contains("cu=INR"))
        assertTrue(uri.contains("tn=Dinner+bill"))
    }

    @Test
    fun `buildUpiUri formats integer amounts cleanly`() {
        val uri = QRCodeGenerator.buildUpiUri(
            vpa = "store@sbi",
            payeeName = "Store",
            amount = "500.00"
        )

        assertTrue(uri.contains("am=500"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buildUpiUri throws on empty VPA`() {
        QRCodeGenerator.buildUpiUri(vpa = "   ", payeeName = "Test")
    }

    @Test
    fun `generateBitMatrix creates expected dimensions`() {
        val matrix = QRCodeGenerator.generateBitMatrix("upi://pay?pa=test@bank", size = 256)
        assertNotNull(matrix)
        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)
    }

    @Test
    fun `roundtrip QR generation and decoding matches QRCodeParser`() {
        val inputUpi = UPIData(
            vpa = "vendor@icici",
            payeeName = "Chai Stall",
            amount = "40",
            transactionNote = "Morning tea",
            currency = "INR"
        )

        // 1. Generate QR bitmap
        val bitmap = QRCodeGenerator.generatePaymentQrBitmap(inputUpi, size = 300)
        assertNotNull(bitmap)
        assertEquals(300, bitmap.width)
        assertEquals(300, bitmap.height)

        // 2. Decode pixels using ZXing QRCodeReader
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = QRCodeReader().decode(binaryBitmap)

        assertNotNull(result)
        val decodedText = result.text
        assertTrue(decodedText.startsWith("upi://pay?"))

        // 3. Verify Flowpay QRCodeParser parses it identically
        val parseResult = QRCodeParser.parse(decodedText)
        assertTrue("Parsed result should be Valid", parseResult is QRCodeParser.ParseResult.Valid)

        val parsedData = (parseResult as QRCodeParser.ParseResult.Valid).data
        assertEquals("vendor@icici", parsedData.vpa)
        assertEquals("Chai Stall", parsedData.payeeName)
        assertEquals("40", parsedData.amount)
        assertEquals("INR", parsedData.currency)
    }
}
