package com.gfpgan_android.presentation.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gfpgan_android.domain.usecase.RestoreFaceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * FaceRestoreViewModel - Yüz restorasyonu ekranı için ViewModel
 * 
 * MVVM Pattern - Clean Architecture Presentation Layer
 * 
 * State Management:
 * - isProcessing: İşlem devam ediyor mu?
 * - restoredBitmap: İşlenmiş fotoğraf
 * - errorMessage: Hata mesajı
 * 
 * Threading:
 * - viewModelScope kullanır (Main-safe)
 * - UseCase içinde Dispatchers.Default'a geçilir
 */
class FaceRestoreViewModel(
    private val restoreFaceUseCase: RestoreFaceUseCase
) : ViewModel() {
    
    // ==================== UI State ====================
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _restoredBitmap = MutableStateFlow<Bitmap?>(null)
    val restoredBitmap: StateFlow<Bitmap?> = _restoredBitmap.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()
    
    // ==================== Public API ====================
    
    /**
     * Yüzü restore et
     * 
     * Kotlin Rules:
     * - Null safety: bitmap?.let kullanımı
     * - No `!!` operator
     * - Flow ile reactive state
     * 
     * @param originalBitmap Orijinal fotoğraf
     * @param keepOriginalSize Orijinal boyutu koru
     */
    fun restoreFace(
        originalBitmap: Bitmap?,
        keepOriginalSize: Boolean = true,
        useFaceDetection: Boolean = true,
        applyUpscaling: Boolean = false,
        blendRatio: Float = 0.55f
    ) {
        // Null safety check
        originalBitmap?.let { bitmap ->
            viewModelScope.launch {
                try {
                    _isProcessing.value = true
                    _errorMessage.value = null
                    _processingProgress.value = 0.3f
                    
                    android.util.Log.d("FaceRestoreVM", "Starting face restoration...")
                    
                    // UseCase çağrısı
                    val result = restoreFaceUseCase.invoke(
                        originalBitmap = bitmap,
                        keepOriginalSize = keepOriginalSize,
                        useFaceDetection = useFaceDetection,
                        applyUpscaling = applyUpscaling,
                        blendRatio = blendRatio
                    )
                    
                    _processingProgress.value = 1.0f
                    _restoredBitmap.value = result
                    
                    android.util.Log.i("FaceRestoreVM", "✓ Face restoration completed successfully")
                    
                } catch (e: Exception) {
                    _errorMessage.value = "Yüz restorasyonu hatası: ${e.message}"
                    _restoredBitmap.value = null
                    e.printStackTrace()
                    android.util.Log.e("FaceRestoreVM", "✗ Restoration error: ${e.message}")
                } finally {
                    _isProcessing.value = false
                    _processingProgress.value = 0f
                }
            }
        } ?: run {
            _errorMessage.value = "Bitmap null - Lütfen geçerli bir fotoğraf seçin"
            android.util.Log.w("FaceRestoreVM", "⚠ Null bitmap provided")
        }
    }
    
    /**
     * Batch işleme (Gelecekte grup fotoğrafları için)
     */
    fun restoreFacesBatch(bitmaps: List<Bitmap>) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _errorMessage.value = null
                
                val results = restoreFaceUseCase.invokeBatch(bitmaps)
                
                // İlk sonucu göster (veya başka bir strateji)
                _restoredBitmap.value = results.firstOrNull()
                
            } catch (e: Exception) {
                _errorMessage.value = "Batch işleme hatası: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * İşlenmiş fotoğrafı temizle
     */
    fun clearRestoredImage() {
        _restoredBitmap.value?.let { bitmap ->
            // Memory cleanup
            bitmap.recycle()
        }
        _restoredBitmap.value = null
        android.util.Log.d("FaceRestoreVM", "Cleared restored image")
    }
    
    /**
     * Hata mesajını temizle
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * ViewModel temizliği
     */
    override fun onCleared() {
        super.onCleared()
        clearRestoredImage()
        android.util.Log.d("FaceRestoreVM", "ViewModel cleared")
    }
}
