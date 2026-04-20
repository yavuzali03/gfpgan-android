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
import kotlinx.coroutines.launch

/**
 * RealESRGAN Test Screen - Pure 2x upscaling test
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealESRGANTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var upscaledBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                try {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(
                            context.contentResolver,
                            selectedUri
                        )
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = false
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        }
                    }
                    
                    bitmap?.let { loadedBitmap ->
                        val argbBitmap = if (loadedBitmap.config != Bitmap.Config.ARGB_8888) {
                            loadedBitmap.copy(Bitmap.Config.ARGB_8888, false).also {
                                loadedBitmap.recycle()
                            }
                        } else {
                            loadedBitmap
                        }
                        originalBitmap = argbBitmap
                        upscaledBitmap = null
                        errorMessage = null
                    }
                } catch (e: Exception) {
                    errorMessage = "Fotoğraf yüklenemedi: ${e.message}"
                }
            }
        }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RealESRGAN 2x Test") },
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
                            "Orijinal (${bitmap.width}x${bitmap.height})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Orijinal",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            originalBitmap?.let { bitmap ->
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isProcessing = true
                                errorMessage = null
                                
                                val manager = com.gfpgan_android.data.ml.RealESRGANOnnxManager(context)
                                val result = manager.upscaleImage(bitmap)
                                manager.cleanup()
                                
                                upscaledBitmap = result
                            } catch (e: Exception) {
                                errorMessage = "İşleme hatası: ${e.message}"
                                e.printStackTrace()
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing
                ) {
                    Text(if (isProcessing) "İşleniyor..." else "📈 2x Upscale (RealESRGAN)")
                }
            }
            
            if (isProcessing) {
                CircularProgressIndicator()
            }
            
            upscaledBitmap?.let { bitmap ->
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
                            "Upscaled (${bitmap.width}x${bitmap.height})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Upscaled",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
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
