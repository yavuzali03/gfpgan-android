package com.gfpgan_android.presentation.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gfpgan_android.presentation.viewmodels.FaceRestoreViewModel
import org.koin.androidx.compose.koinViewModel
import android.provider.MediaStore

/**
 * FaceRestoreScreen - GFPGAN yüz restorasyonu için örnek ekran
 * 
 * Jetpack Compose Best Practices:
 * - State hoisting (ViewModel ile)
 * - collectAsStateWithLifecycle() kullanımı
 * - koinViewModel() ile DI
 * - No side effects in composables
 * 
 * Özellikler:
 * 1. Galeriden fotoğraf seçme
 * 2. GFPGAN ile işleme
 * 3. Öncesi/Sonrası karşılaştırma
 * 4. İşlem durumu gösterimi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRestoreScreen(
    viewModel: FaceRestoreViewModel = koinViewModel()
) {
    val context = LocalContext.current
    
    // ==================== State ====================
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val restoredBitmap by viewModel.restoredBitmap.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val processingProgress by viewModel.processingProgress.collectAsStateWithLifecycle()
    
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // ==================== Photo Picker ====================
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                try {
                    // Modern API: ImageDecoder (API 28+) veya BitmapFactory
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        // API 28+ için ImageDecoder
                        val source = android.graphics.ImageDecoder.createSource(
                            context.contentResolver,
                            selectedUri
                        )
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = false
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        // API 27 ve altı için BitmapFactory
                        context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        }
                    }
                    
                    // Null check ve boyut kontrolü
                    bitmap?.let { loadedBitmap ->
                        if (loadedBitmap.width > 0 && loadedBitmap.height > 0) {
                            // ARGB_8888 formatına dönüştür (ONNX Runtime uyumluluğu için)
                            val argbBitmap = if (loadedBitmap.config != Bitmap.Config.ARGB_8888) {
                                loadedBitmap.copy(Bitmap.Config.ARGB_8888, false).also {
                                    loadedBitmap.recycle()
                                }
                            } else {
                                loadedBitmap
                            }
                            
                            originalBitmap = argbBitmap
                            viewModel.clearError()
                            
                            android.util.Log.d(
                                "FaceRestore",
                                "✓ Bitmap loaded: ${argbBitmap.width}x${argbBitmap.height}, " +
                                "Config: ${argbBitmap.config}"
                            )
                        } else {
                            android.util.Log.e("FaceRestore", "✗ Invalid bitmap dimensions")
                            viewModel.clearError()
                            // ViewModel'a hata set et
                            // viewModel.setError("Geçersiz resim boyutu")
                        }
                    } ?: run {
                        android.util.Log.e("FaceRestore", "✗ Bitmap decode failed (null)")
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("FaceRestore", "✗ Bitmap loading error: ${e.message}")
                    // viewModel.setError("Fotoğraf yüklenemedi: ${e.message}")
                }
            }
        }
    )
    
    // ==================== UI ====================
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GFPGAN Face Restore") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ==================== Fotoğraf Seç Butonu ====================
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !isProcessing
            ) {
                Text("Fotoğraf Seç")
            }
            
            // ==================== Orijinal Fotoğraf ====================
            originalBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Orijinal",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Orijinal Fotoğraf",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            // ==================== Face Detection Toggle ====================
            var useFaceDetection by remember { mutableStateOf(true) }
            var applyUpscaling by remember { mutableStateOf(false) }
            var aiStrength by remember { mutableFloatStateOf(0.55f) }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Face Detection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔍 Yüz Tespiti",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = useFaceDetection,
                            onCheckedChange = { useFaceDetection = it },
                            enabled = !isProcessing
                        )
                    }
                    
                    // 2x Upscaling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📈 2x Upscaling",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = applyUpscaling,
                            onCheckedChange = { applyUpscaling = it },
                            enabled = !isProcessing
                        )
                    }
                    
                    // AI Strength Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎨 AI Gücü",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${(aiStrength * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = aiStrength,
                            onValueChange = { aiStrength = it },
                            valueRange = 0f..1f,
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = when {
                                aiStrength < 0.3f -> "Çok Hafif"
                                aiStrength < 0.5f -> "Hafif"
                                aiStrength < 0.7f -> "Dengeli"
                                aiStrength < 0.9f -> "Güçlü"
                                else -> "Maksimum"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            // ==================== İşle Butonu ====================
            originalBitmap?.let {
                Button(
                    onClick = { 
                        viewModel.restoreFace(
                            originalBitmap = it,
                            keepOriginalSize = true,
                            useFaceDetection = useFaceDetection,
                            applyUpscaling = applyUpscaling,
                            blendRatio = aiStrength
                        )
                    },
                    enabled = !isProcessing
                ) {
                    Text(
                        if (isProcessing) "İşleniyor..." 
                        else "Yüzü Restore Et (GFPGAN)"
                    )
                }
            }
            
            // ==================== Progress Indicator ====================
            if (isProcessing) {
                LinearProgressIndicator(
                    progress = { processingProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "ONNX Runtime inference çalışıyor...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // ==================== Restore Edilmiş Fotoğraf ====================
            restoredBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Restore Edilmiş (GFPGAN)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "İşlenmiş Fotoğraf",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // ==================== Kaydet Butonu ====================
                Button(
                    onClick = {
                        restoredBitmap?.let { bitmap ->
                            // Galeriye kaydet
                            try {
                                val contentValues = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "GFPGAN_${System.currentTimeMillis()}.jpg")
                                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PhotoEnhancer")
                                }
                                
                                val uri = context.contentResolver.insert(
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    contentValues
                                )
                                
                                uri?.let { imageUri ->
                                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
                                    }
                                    android.widget.Toast.makeText(context, "✓ Galeriye kaydedildi!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "✗ Kaydetme hatası: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = restoredBitmap != null,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text("💾 Galeriye Kaydet")
                }
            }
            
            // ==================== Hata Mesajı ====================
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
