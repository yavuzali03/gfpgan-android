package com.gfpgan_android.presentation.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gfpgan_android.data.ml.CodeFormerOnnxManager
import com.gfpgan_android.data.ml.FaceDetectionManager
import com.gfpgan_android.data.ml.RealESRGANOnnxManager
import com.gfpgan_android.util.MemoryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simple test screen for CodeFormer only (no ESRGAN, no blending).
 */
@Composable
fun CodeFormerOnlyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var inputBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var outputBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Görsel seçin") }
    
    // Face detection option
    var useFaceDetection by remember { mutableStateOf(true) }
    var applyUpscaling by remember { mutableStateOf(false) }
    
    // Image picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                inputBitmap = BitmapFactory.decodeStream(inputStream)
                outputBitmap = null
                statusMessage = "Görsel yüklendi: ${inputBitmap?.width}x${inputBitmap?.height}"
            } catch (e: Exception) {
                statusMessage = "Hata: ${e.message}"
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🎭 CodeFormer Only",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        // Face Detection Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = useFaceDetection,
                onCheckedChange = { useFaceDetection = it }
            )
            Text("🔍 Yüz Tespiti Kullan (ML Kit)")
        }
        
        // Upscaling Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = applyUpscaling,
                onCheckedChange = { applyUpscaling = it }
            )
            Text("📈 2x Upscaling (RealESRGAN)")
        }
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { launcher.launch("image/*") },
                enabled = !isProcessing
            ) {
                Text("Görsel Seç")
            }
            
            Button(
                onClick = {
                    inputBitmap?.let { bitmap ->
                        isProcessing = true
                        statusMessage = "⚙️ CodeFormer işleniyor..."
                        
                        scope.launch {
                            try {
                                val codeformerManager = CodeFormerOnnxManager(context)
                                
                                val enhanced = if (useFaceDetection) {
                                    statusMessage = "🔍 Yüz tespiti yapılıyor..."
                                    val faceDetector = FaceDetectionManager(context)
                                    
                                    val result = withContext(Dispatchers.Default) {
                                        codeformerManager.enhanceWithFaceDetection(
                                            bitmap,
                                            faceDetector
                                        )
                                    }
                                    
                                    faceDetector.cleanup()
                                    result
                                } else {
                                    statusMessage = "⚙️ Tüm görsel işleniyor (tiling)..."
                                    withContext(Dispatchers.Default) {
                                        codeformerManager.enhance(bitmap)
                                    }
                                }
                                
                                // Apply RealESRGAN if enabled
                                val finalResult = if (applyUpscaling) {
                                    statusMessage = "📈 2x Upscaling..."
                                    val esrganManager = RealESRGANOnnxManager(context)
                                    val upscaled = esrganManager.upscaleImage(enhanced)
                                    esrganManager.cleanup()
                                    enhanced.recycle()
                                    upscaled
                                } else {
                                    enhanced
                                }
                                
                                outputBitmap = finalResult
                                statusMessage = "✅ Tamamlandı! (${finalResult.width}x${finalResult.height})"
                                
                                codeformerManager.cleanup()
                                MemoryUtils.logMemoryInfo()
                                
                            } catch (e: Exception) {
                                statusMessage = "❌ Hata: ${e.message}"
                                e.printStackTrace()
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                },
                enabled = inputBitmap != null && !isProcessing
            ) {
                Text("İşle")
            }
        }
        
        if (isProcessing) {
            CircularProgressIndicator()
        }
        
        // Image display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Input
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Orijinal", style = MaterialTheme.typography.labelMedium)
                inputBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Input",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Output
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CodeFormer", style = MaterialTheme.typography.labelMedium)
                outputBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Output",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        
        // Download button
        if (outputBitmap != null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    outputBitmap?.let { bitmap ->
                        scope.launch {
                            try {
                                val saved = saveImageToGallery(context, bitmap)
                                if (saved) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "✅ Görsel kaydedildi!",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "❌ Kaydetme başarısız",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "❌ Hata: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📥 İndir")
            }
        }
    }
}

/**
 * Save bitmap to gallery using MediaStore (Android 10+).
 */
private suspend fun saveImageToGallery(
    context: Context,
    bitmap: Bitmap
): Boolean = withContext(Dispatchers.IO) {
    try {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "codeformer_only_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PhotoEnhancer")
        }
        
        val uri = context.contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return@withContext false
        
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
        
        return@withContext true
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}
