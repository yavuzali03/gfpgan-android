package com.gfpgan_android.presentation.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gfpgan_android.presentation.components.ModelSelectionCard
import com.gfpgan_android.presentation.viewmodels.EditorViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: EditorViewModel = koinViewModel()
    
    // Load image from URI
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(imageUri) {
        originalBitmap = loadBitmapFromUri(context, Uri.parse(imageUri))
    }
    
    // Collect state
    val selectedModel by viewModel.selectedModel.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val resultBitmap by viewModel.resultBitmap.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // UI State
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var showFullScreen by remember { mutableStateOf(false) }

    // Parameters
    var applyUpscaling by remember { mutableStateOf(false) }
    var aiStrength by remember { mutableFloatStateOf(0.55f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    com.gfpgan_android.presentation.components.FontAwesomeIcon(
                        iconCode = "\uf053", // Chevron Left
                        size = 20.sp,
                        color = Color.White
                    )
                }
                
                // Title
                Text(
                    text = "GFPGAN Android",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.width(40.dp)) // Balance
            }

            // --- IMAGE AREA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (originalBitmap != null) {
                    if (resultBitmap != null) {
                        // Before/After Slider
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFullScreen = true },
                            contentAlignment = Alignment.TopCenter
                        ) {
                            com.gfpgan_android.presentation.components.BeforeAfterSlider(
                                beforeBitmap = originalBitmap!!,
                                afterBitmap = resultBitmap!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f)
                            )
                        }
                    } else {
                        // Original Image
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                bitmap = originalBitmap!!.asImageBitmap(),
                                contentDescription = "Original",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    // Loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // --- LAYER 3: BOTTOM FLOATING CARD ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Removed padding(16.dp) to make it full width
                .background(
                    color = Color.Black.copy(alpha = 0.5f), // Glassy dark
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(24.dp)
                .navigationBarsPadding() 
        ) {
            // Eliminated Drag Handle (Bottom Sheet Indicator)
            
            // Model Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "İyileştirme Modu",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    com.gfpgan_android.presentation.components.ModelSelectionCard(
                        title = "Gerçekçi (GFPGAN)",
                        subtitle = "Doğal Yüz Onarımı",
                        iconCode = "\uf007", // User icon
                        isSelected = true,
                        onClick = { /* Only one model available */ },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        enabled = !isProcessing,
                        selectedGradientColors = listOf(
                            Color(0xFFf4d444),
                            Color(0xFFf3696e)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Options (Upscaling)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2x Upscaling",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                com.gfpgan_android.presentation.components.MinimalSwitch(
                    checked = applyUpscaling,
                    onCheckedChange = { applyUpscaling = it },
                    enabled = !isProcessing
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // AI Strength Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Gücü (Blend)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${(aiStrength * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Slider(
                    value = aiStrength,
                    onValueChange = { aiStrength = it },
                    valueRange = 0f..1f,
                    enabled = !isProcessing,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFf4d444),
                        activeTrackColor = Color(0xFFf4d444),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            val currentGradient = listOf(
                Color(0xFFf4d444),
                Color(0xFFf3696e)
            )

            if (resultBitmap == null) {
                com.gfpgan_android.presentation.components.PrimaryButton(
                    text = if (isProcessing) "İşleniyor..." else "Geliştir (Enhance)",
                    onClick = {
                        viewModel.processImage(
                            originalBitmap = originalBitmap!!,
                            useFaceDetection = true, 
                            applyUpscaling = applyUpscaling, 
                            blendRatio = aiStrength
                        )
                    },
                    enabled = !isProcessing && originalBitmap != null,
                    gradientColors = currentGradient
                )
            } else {
                // Save
                com.gfpgan_android.presentation.components.PrimaryButton(
                    text = "Kaydet",
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                val uri = com.gfpgan_android.util.ImageSaveUtils.saveBitmapToGallery(context, resultBitmap!!)
                                isSaving = false
                                saveMessage = if (uri != null) "✓ Galeriye Kaydedildi" else "✗ Kayıt Başarısız"
                                kotlinx.coroutines.delay(3000)
                                saveMessage = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    gradientColors = currentGradient
                )
            }
            
            // Messages
            saveMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = if (it.startsWith("✓")) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
             errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // --- FULL SCREEN OVERLAY ---
    if (showFullScreen && resultBitmap != null && originalBitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            com.gfpgan_android.presentation.components.BeforeAfterSlider(
                beforeBitmap = originalBitmap!!,
                afterBitmap = resultBitmap!!,
                modifier = Modifier.fillMaxSize()
            )

            // Close Button
            IconButton(
                onClick = { showFullScreen = false },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                com.gfpgan_android.presentation.components.FontAwesomeIcon(
                    iconCode = "\uf053", // Chevron Left
                    size = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        android.util.Log.e("EditorScreen", "Error loading bitmap: ${e.message}")
        null
    }
}
