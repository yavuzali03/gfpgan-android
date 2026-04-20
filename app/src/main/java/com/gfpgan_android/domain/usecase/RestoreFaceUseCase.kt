package com.gfpgan_android.domain.usecase

import android.graphics.Bitmap
import com.gfpgan_android.data.ai.GfpganManager

/**
 * RestoreFaceUseCase - Yüz restorasyonu iş mantığı
 * 
 * Clean Architecture Domain Layer
 * GfpganManager'ı wrap eder ve face restoration pipeline'ını kontrol eder
 * 
 * Use Case Pattern:
 * - operator fun invoke() ile çağrılır
 * - Tek sorumluluk: Yüz kalitesi iyileştirme
 * - Repository/Manager bağımlılığını soyutlar
 */
class RestoreFaceUseCase(
    private val gfpganManager: GfpganManager
) {
    /**
     * Yüzü restore et (Basit kullanım)
     * 
     * @param originalBitmap Orijinal fotoğraf
     * @return Restore edilmiş fotoğraf (orijinal boyutta)
     */
    suspend operator fun invoke(originalBitmap: Bitmap): Bitmap {
        return gfpganManager.restoreFace(
            originalBitmap = originalBitmap,
            scaleBackToOriginal = true
        )
    }

    /**
     * Yüzü restore et (Gelişmiş kontrol)
     * 
     * @param originalBitmap Orijinal fotoğraf
     * @param keepOriginalSize True ise orijinal boyuta scale et
     * @return Restore edilmiş fotoğraf
     */
    suspend fun invoke(
        originalBitmap: Bitmap,
        keepOriginalSize: Boolean,
        useFaceDetection: Boolean = true,
        applyUpscaling: Boolean = false,
        blendRatio: Float = 0.55f
    ): Bitmap {
        return gfpganManager.restoreFace(
            originalBitmap = originalBitmap,
            scaleBackToOriginal = keepOriginalSize,
            useFaceDetection = useFaceDetection,
            applyUpscaling = applyUpscaling,
            blendRatio = blendRatio
        )
    }

    /**
     * Batch işleme
     */
    suspend fun invokeBatch(bitmaps: List<Bitmap>): List<Bitmap> {
        return gfpganManager.restoreFacesBatch(bitmaps)
    }
}
