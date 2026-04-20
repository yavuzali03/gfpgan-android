package com.gfpgan_android.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Image Blending Utilities
 * 
 * Inspired by Python: cv2.addWeighted(ai_base, 0.55, orig_base, 0.45, 0)
 * Blends AI-enhanced image with original to preserve natural look.
 */
object ImageBlendingUtils {
    
    /**
     * Blend two bitmaps with weighted average
     * 
     * Formula: result = (aiImage * aiWeight) + (originalImage * originalWeight)
     * 
     * @param aiBitmap AI-enhanced image
     * @param originalBitmap Original image (must be same size as aiBitmap)
     * @param aiWeight Weight for AI image (0.0 to 1.0), default 0.55
     * @return Blended bitmap
     * 
     * Examples:
     * - blendBitmaps(ai, orig, 0.55f) → 55% AI + 45% Original (natural)
     * - blendBitmaps(ai, orig, 0.70f) → 70% AI + 30% Original (sharper)
     * - blendBitmaps(ai, orig, 1.0f)  → 100% AI (no blending)
     * - blendBitmaps(ai, orig, 0.0f)  → 100% Original (no AI)
     */
    fun blendBitmaps(
        aiBitmap: Bitmap,
        originalBitmap: Bitmap,
        aiWeight: Float = 0.55f
    ): Bitmap {
        require(aiBitmap.width == originalBitmap.width && aiBitmap.height == originalBitmap.height) {
            "Bitmaps must have same dimensions. AI: ${aiBitmap.width}x${aiBitmap.height}, Original: ${originalBitmap.width}x${originalBitmap.height}"
        }
        
        require(aiWeight in 0.0f..1.0f) {
            "aiWeight must be between 0.0 and 1.0, got: $aiWeight"
        }
        
        // Calculate weights
        val originalWeight = 1.0f - aiWeight
        
        android.util.Log.d("ImageBlending", "Blending: ${(aiWeight * 100).toInt()}% AI + ${(originalWeight * 100).toInt()}% Original")
        
        val width = aiBitmap.width
        val height = aiBitmap.height
        
        // Extract pixels
        val aiPixels = IntArray(width * height)
        val origPixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)
        
        aiBitmap.getPixels(aiPixels, 0, width, 0, 0, width, height)
        originalBitmap.getPixels(origPixels, 0, width, 0, 0, width, height)
        
        // Blend each pixel
        for (i in aiPixels.indices) {
            val aiPixel = aiPixels[i]
            val origPixel = origPixels[i]
            
            // Extract ARGB components
            val aiA = (aiPixel shr 24) and 0xFF
            val aiR = (aiPixel shr 16) and 0xFF
            val aiG = (aiPixel shr 8) and 0xFF
            val aiB = aiPixel and 0xFF
            
            val origA = (origPixel shr 24) and 0xFF
            val origR = (origPixel shr 16) and 0xFF
            val origG = (origPixel shr 8) and 0xFF
            val origB = origPixel and 0xFF
            
            // Weighted blend
            val blendedA = ((aiA * aiWeight + origA * originalWeight).toInt().coerceIn(0, 255))
            val blendedR = ((aiR * aiWeight + origR * originalWeight).toInt().coerceIn(0, 255))
            val blendedG = ((aiG * aiWeight + origG * originalWeight).toInt().coerceIn(0, 255))
            val blendedB = ((aiB * aiWeight + origB * originalWeight).toInt().coerceIn(0, 255))
            
            // Reconstruct pixel
            resultPixels[i] = (blendedA shl 24) or (blendedR shl 16) or (blendedG shl 8) or blendedB
        }
        
        // Create result bitmap
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
        
        return resultBitmap
    }
    
    /**
     * Preset blending ratios
     */
    object Presets {
        const val NATURAL = 0.55f      // 55% AI - Balanced, natural look (Python default)
        const val SUBTLE = 0.40f       // 40% AI - Very subtle enhancement
        const val BALANCED = 0.65f     // 65% AI - Stronger AI effect
        const val SHARP = 0.80f        // 80% AI - Sharp, detailed
        const val PURE_AI = 1.0f       // 100% AI - No blending
        const val PURE_ORIGINAL = 0.0f // 0% AI - Original image
    }
}
