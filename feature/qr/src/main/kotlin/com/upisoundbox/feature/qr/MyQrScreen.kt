package com.upisoundbox.feature.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.upisoundbox.core.data.preferences.SoundboxPreferences
import com.upisoundbox.core.design.component.PillButton
import com.upisoundbox.core.design.component.PillInputWithCta
import com.upisoundbox.core.design.theme.LocalSoundboxColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@Composable
fun MyQrScreen(
    preferences: SoundboxPreferences,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalSoundboxColors.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val savedUpiId by preferences.myUpiId.collectAsState(initial = "")
    val savedShopName by preferences.myShopName.collectAsState(initial = "")

    var upiIdInput by remember { mutableStateOf("") }
    var shopNameInput by remember { mutableStateOf("") }
    var optionalAmountInput by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(savedUpiId, savedShopName) {
        if (upiIdInput.isEmpty()) upiIdInput = savedUpiId
        if (shopNameInput.isEmpty()) shopNameInput = savedShopName
    }

    // Regenerate QR on change
    LaunchedEffect(upiIdInput, shopNameInput, optionalAmountInput) {
        if (upiIdInput.isNotBlank()) {
            val uri = buildUpiUri(upiIdInput, shopNameInput, optionalAmountInput)
            qrBitmap = generateQrCodeBitmap(uri, 512)
        } else {
            qrBitmap = null
        }
    }

    // PDF Export launcher
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && qrBitmap != null) {
            scope.launch(Dispatchers.IO) {
                exportStandeePdf(context, uri, shopNameInput, upiIdInput, qrBitmap!!)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My UPI QR & Standee",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.ink
                )
            }

            if (qrBitmap != null) {
                IconButton(
                    onClick = {
                        shareQrImage(context, qrBitmap!!, shopNameInput)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share QR",
                        tint = colors.accentOrange
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Pill Input with Embedded Black CTA (Reference 1 Pattern!)
            Text(
                text = "YOUR UPI ID (VPA)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            PillInputWithCta(
                value = upiIdInput,
                onValueChange = { upiIdInput = it.trim() },
                placeholder = "e.g. yourname@upi or 9876543210@paytm",
                ctaText = "Save",
                onCtaClick = {
                    scope.launch {
                        preferences.setMyQrDetails(upiIdInput, shopNameInput)
                        Toast.makeText(context, "UPI ID saved!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Shop Name input
            Text(
                text = "SHOP / BUSINESS NAME",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            PillInputWithCta(
                value = shopNameInput,
                onValueChange = { shopNameInput = it },
                placeholder = "e.g. Krishna General Store",
                ctaText = "Update",
                onCtaClick = {
                    scope.launch {
                        preferences.setMyQrDetails(upiIdInput, shopNameInput)
                        Toast.makeText(context, "Shop name updated!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QR Standee Card Preview
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (shopNameInput.isNotBlank()) shopNameInput else "Your Shop Name",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "SCAN & PAY WITH ANY UPI APP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentOrange,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier
                                .size(220.dp)
                                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(colors.bubbleGrey, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Enter UPI ID above\nto generate QR code",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (upiIdInput.isNotBlank()) upiIdInput else "your-vpa@bank",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Export Printable Standee as PDF button
            PillButton(
                text = "Export Printable Standee (PDF)",
                onClick = {
                    val filename = "UPI_Standee_${System.currentTimeMillis()}.pdf"
                    exportPdfLauncher.launch(filename)
                },
                enabled = qrBitmap != null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun buildUpiUri(upiId: String, shopName: String, amount: String): String {
    val cleanUpi = Uri.encode(upiId.trim())
    val cleanName = Uri.encode(shopName.trim())
    var uri = "upi://pay?pa=$cleanUpi&pn=$cleanName&cu=INR"
    if (amount.isNotBlank()) {
        uri += "&am=${amount.trim()}"
    }
    return uri
}

private fun generateQrCodeBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

private fun shareQrImage(context: Context, bitmap: Bitmap, shopName: String) {
    try {
        val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cacheDir, "upi_qr.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Scan to pay $shopName via any UPI app")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share UPI QR"))
    } catch (_: Exception) {
        Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun exportStandeePdf(
    context: Context,
    uri: Uri,
    shopName: String,
    upiId: String,
    qrBitmap: Bitmap
) {
    withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#141414")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(24f, 24f, 571f, 818f, 20f, 20f, borderPaint)

        // Header accent bar
        val accentPaint = Paint().apply { color = Color.parseColor("#F25C2E") }
        canvas.drawRoundRect(40f, 40f, 555f, 130f, 16f, 16f, accentPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val displayName = if (shopName.isNotBlank()) shopName else "MERCHANT STAND"
        canvas.drawText(displayName, 595f / 2f, 95f, headerTextPaint)

        val subHeaderPaint = Paint().apply {
            color = Color.parseColor("#141414")
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SCAN TO PAY WITH ANY UPI APP", 595f / 2f, 180f, subHeaderPaint)

        // Draw scaled QR Code
        val scaledQr = Bitmap.createScaledBitmap(qrBitmap, 380, 380, true)
        canvas.drawBitmap(scaledQr, (595f - 380f) / 2f, 220f, null)

        // VPA text
        val vpaPaint = Paint().apply {
            color = Color.parseColor("#141414")
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("UPI ID: $upiId", 595f / 2f, 650f, vpaPaint)

        // Supported apps text
        val appsPaint = Paint().apply {
            color = Color.parseColor("#8A8A8F")
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Google Pay • PhonePe • Paytm • BHIM • Amazon Pay • Any Bank UPI", 595f / 2f, 700f, appsPaint)

        // Footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#8A8A8F")
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Instant voice confirmation announced aloud by UPI Soundbox", 595f / 2f, 780f, footerPaint)

        doc.finishPage(page)

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                doc.writeTo(outputStream)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Printable Standee PDF saved!", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
            }
        } finally {
            doc.close()
        }
    }
}
