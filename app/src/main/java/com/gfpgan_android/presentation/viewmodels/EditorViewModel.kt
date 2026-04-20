package com.gfpgan_android.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gfpgan_android.data.ai.GfpganManager
import com.gfpgan_android.data.ml.FaceDetectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Editor ViewModel - Manages image processing state
 */
class EditorViewModel(
    private val context: Context,
    private val gfpganManager: GfpganManager
) : ViewModel() {
    
    // UI State
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    
    private val _resultBitmap = MutableStateFlow<Bitmap?>(null)
    val resultBitmap: StateFlow<Bitmap?> = _resultBitmap
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress
    
    // Model selection
    enum class AIModel {
        GFPGAN
    }
    
    private val _selectedModel = MutableStateFlow(AIModel.GFPGAN)
    val selectedModel: StateFlow<AIModel> = _selectedModel
    
    fun processImage(
        originalBitmap: Bitmap,
        useFaceDetection: Boolean,
        applyUpscaling: Boolean,
        blendRatio: Float
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _errorMessage.value = null
                _processingProgress.value = 0.3f
                
                val result = gfpganManager.restoreFace(
                    originalBitmap = originalBitmap,
                    scaleBackToOriginal = true,
                    useFaceDetection = useFaceDetection,
                    applyUpscaling = applyUpscaling,
                    blendRatio = blendRatio
                )
                
                _processingProgress.value = 1.0f
                _resultBitmap.value = result
                
                android.util.Log.i("EditorVM", "✓ Processing completed")
                
            } catch (e: Exception) {
                _errorMessage.value = "İşleme hatası: ${e.message}"
                _resultBitmap.value = null
                e.printStackTrace()
                android.util.Log.e("EditorVM", "✗ Processing error: ${e.message}")
            } finally {
                _isProcessing.value = false
                _processingProgress.value = 0f
            }
        }
    }
    

    fun clearResult() {
        _resultBitmap.value?.recycle()
        _resultBitmap.value = null
        _errorMessage.value = null
    }
}
