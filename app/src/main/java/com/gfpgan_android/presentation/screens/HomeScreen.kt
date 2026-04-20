package com.gfpgan_android.presentation.screens

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gfpgan_android.util.GalleryUtils
import com.gfpgan_android.util.PermissionUtils
import kotlinx.coroutines.launch

/**
 * HomeScreen - Ana ekran
 * 
 * Uygulama açıldığında izin kontrolü yapar
 * İzin varsa direkt galeriyi yükler, yoksa izin ister
 * 
 * @param isDarkMode Dark mode aktif mi?
 * @param onThemeToggle Theme toggle callback
 * @param onImageSelected Fotoğraf seçildiğinde callback
 * @param onFaceRestoreClick Face restore ekranına git
 */
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onImageSelected: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var galleryImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }
    
    // Galeriden fotoğrafları yükle
    val loadGallery: () -> Unit = {
        isLoading = true
        scope.launch {
            galleryImages = GalleryUtils.getGalleryImages(context)
            isLoading = false
        }
    }
    
    // İzin kontrolü yap
    val checkPermissions: () -> Boolean = {
        PermissionUtils.getStoragePermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED
        }
    }
    
    // İzin launcher - Galeri izinleri
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionGranted = allGranted
        
        if (allGranted) {
            loadGallery()
        }
    }
    
    // Uygulama açıldığında: izin varsa galeriyi yükle, yoksa izin iste
    LaunchedEffect(Unit) {
        if (checkPermissions()) {
            // İzin zaten var, direkt galeriyi yükle
            permissionGranted = true
            loadGallery()
        } else {
            // İzin yok, iste
            storagePermissionLauncher.launch(
                PermissionUtils.getStoragePermissions()
            )
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Header - Başlık ve Dark Mode Switch
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "gradient")
                val offset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 3000f, // Match gradient width for seamless loop
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "offset"
                )

                Text(
                    text = "AI Photo Enhancer",
                    style = MaterialTheme.typography.headlineLarge.merge(
                        androidx.compose.ui.text.TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFf4d444), // Yellow
                                    Color(0xFFf3696e), // Pink
                                    Color(0xFF8752a3), // Purple
                                    Color(0xFF6274e7), // Blue
                                    Color(0xFF8752a3), // Purple
                                    Color(0xFFf3696e), // Pink
                                    Color(0xFFf4d444)  // Yellow
                                ),
                                start = Offset(offset, 0f),
                                end = Offset(offset + 3000f, 0f),
                                tileMode = TileMode.Repeated // Repeated creates smooth loop when start/end colors match
                            ),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                )
                
            }
            
            // Content - Galeriden fotoğraflar veya loading/empty state
            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                !permissionGranted -> {
                    // İzin verilmedi
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Galeri izni gerekli\nLütfen ayarlardan izin verin",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                
                galleryImages.isEmpty() -> {
                    // Galeri boş
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Galeride fotoğraf bulunamadı",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                else -> {
                    // Fotoğraf grid'i
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = galleryImages,
                            key = { it.toString() }
                        ) { imageUri ->
                            GalleryImageItem(
                                imageUri = imageUri,
                                onClick = { onImageSelected(imageUri) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * GalleryImageItem - Grid'deki tek bir fotoğraf item'ı
 */
@Composable
private fun GalleryImageItem(
    imageUri: Uri,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .crossfade(true)
            .build(),
        contentDescription = "Gallery Image",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    )
}
