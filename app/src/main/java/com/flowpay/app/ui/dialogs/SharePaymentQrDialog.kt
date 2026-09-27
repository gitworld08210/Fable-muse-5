// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.flowpay.app.ui.dialogs

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flowpay.app.constants.AppConstants
import com.flowpay.app.data.UPIData
import com.flowpay.app.features.qr_generator.QRCodeGenerator
import com.flowpay.app.ui.theme.LocalFlowpayAccentTheme

private const val PREFS_KEY_SAVED_VPA = "user_saved_vpa"
private const val PREFS_KEY_SAVED_NAME = "user_saved_name"

/**
 * Material Design 3 Dialog allowing users to create, preview, and share their
 * personalized UPI QR code for offline scanning.
 */
@Composable
fun SharePaymentQrDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember {
        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var vpa by remember {
        mutableStateOf(prefs.getString(PREFS_KEY_SAVED_VPA, "") ?: "")
    }
    var payeeName by remember {
        mutableStateOf(prefs.getString(PREFS_KEY_SAVED_NAME, "") ?: "")
    }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generationError by remember { mutableStateOf<String?>(null) }

    // Recompute QR Code bitmap whenever VPA, name, amount, or note changes
    LaunchedEffect(vpa, payeeName, amount, note) {
        val trimmedVpa = vpa.trim()
        if (trimmedVpa.isEmpty() || !trimmedVpa.contains("@")) {
            qrBitmap = null
            generationError = if (trimmedVpa.isNotEmpty()) "Enter a valid UPI ID (e.g. name@bank)" else null
            return@LaunchedEffect
        }

        try {
            val upiData = UPIData(
                vpa = trimmedVpa,
                payeeName = payeeName.trim(),
                amount = amount.trim(),
                transactionNote = note.trim(),
                currency = "INR"
            )
            qrBitmap = QRCodeGenerator.generatePaymentQrBitmap(upiData, size = 512)
            generationError = null

            // Persist the user's VPA and name for next time
            prefs.edit()
                .putString(PREFS_KEY_SAVED_VPA, trimmedVpa)
                .putString(PREFS_KEY_SAVED_NAME, payeeName.trim())
                .apply()
        } catch (e: Exception) {
            qrBitmap = null
            generationError = e.localizedMessage ?: "Failed to generate QR code"
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .testTag("share_payment_qr_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LocalFlowpayAccentTheme.current.headerGradientStart.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = LocalFlowpayAccentTheme.current.headerGradientStart,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Receive Money",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("qr_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High-contrast QR Display Container
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Your UPI Payment QR Code",
                            modifier = Modifier
                                .size(196.dp)
                                .testTag("generated_qr_image")
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = generationError ?: "Enter UPI ID to generate QR",
                                fontSize = 12.sp,
                                color = if (generationError != null) Color(0xFFD32F2F) else Color.DarkGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan offline with Flowpay or any UPI app",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // UPI VPA Input
                OutlinedTextField(
                    value = vpa,
                    onValueChange = { vpa = it },
                    label = { Text("Your UPI ID (VPA) *") },
                    placeholder = { Text("e.g. mobile@upi, name@okaxis") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_vpa_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Payee Name Input
                OutlinedTextField(
                    value = payeeName,
                    onValueChange = { payeeName = it },
                    label = { Text("Payee Name") },
                    placeholder = { Text("e.g. Priya Sharma") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Optional Amount + Note Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { input ->
                            // Allow digits with up to 2 decimal places
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                amount = input
                            }
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_amount_input")
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_note_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons: Share & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val bitmap = qrBitmap
                            if (bitmap != null) {
                                val upiData = UPIData(
                                    vpa = vpa.trim(),
                                    payeeName = payeeName.trim(),
                                    amount = amount.trim(),
                                    transactionNote = note.trim(),
                                    currency = "INR"
                                )
                                try {
                                    val shareIntent = QRCodeGenerator.createShareIntent(
                                        context = context,
                                        bitmap = bitmap,
                                        upiData = upiData
                                    )
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share Payment QR Code")
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error sharing QR: ${e.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please provide a valid UPI ID first",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = qrBitmap != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalFlowpayAccentTheme.current.headerGradientStart,
                            disabledContainerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("qr_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share QR",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
