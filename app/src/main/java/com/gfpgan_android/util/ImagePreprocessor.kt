package com.gfpgan_android.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * ImagePreprocessor - GFPGAN Model için resim ön işleme yardımcıları
 * 
 * GFPGAN modeli 512x512 sabit boyut bekliyor.
 * Bu sınıf:
 * - Resmi 512x512'ye ölçeklendirir
 * - Aspect ratio koruma seçeneği sunar
 * - RGB/BGR dönüşümünü yönetir
 * - ARGB_8888 formatında çıktı verir
 */
object ImagePreprocessor {

    const val GFPGAN_INPUT_SIZE = 512
    
    /**
     * Resmi 512x512'ye ölçeklendir (Aspect Ratio korumadan - Stretch)
     * 
     * GFPGAN fixed input için en basit yöntem.
     * Resim deforme olur ama bu portre restore için genelde kabul edilebilir.
     * 
     * @param bitmap Orijinal resim
     * @return 512x512 ARGB_8888 bitmap
     */
    fun scaleToFixed512(bitmap: Bitmap): Bitmap {
        require(bitmap.config == Bitmap.Config.ARGB_8888 || bitmap.config == Bitmap.Config.RGB_565) {
            "Bitmap config must be ARGB_8888 or RGB_565, got: ${bitmap.config}"
        }
        
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            GFPGAN_INPUT_SIZE,
            GFPGAN_INPUT_SIZE,
            true // bilinear filtering
        )
        
        // Config kontrolü - ARGB_8888 bekliyor
        return if (scaled.config != Bitmap.Config.ARGB_8888) {
            scaled.copy(Bitmap.Config.ARGB_8888, false).also {
                scaled.recycle()
            }
        } else {
            scaled
        }
    }
    
    /**
     * Resmi 512x512'ye ölçeklendir (Aspect Ratio koruyarak - Center Crop)
     * 
     * Resmin ortasından 512x512'lik kare kesilir.
     * Portre fotoğraflar için önerilir - yüz merkezde kalır.
     * 
     * @param bitmap Orijinal resim
     * @return 512x512 ARGB_8888 bitmap (merkezden kırpılmış)
     */
    fun scaleAndCenterCrop512(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 1. En küçük kenarı 512'ye ölçekle
        val scale = max(
            GFPGAN_INPUT_SIZE.toFloat() / width,
            GFPGAN_INPUT_SIZE.toFloat() / height
        )
        
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        // Edge case: Eğer scale sonrası hala 512'den küçükse, direkt resize yap
        if (scaledWidth < GFPGAN_INPUT_SIZE || scaledHeight < GFPGAN_INPUT_SIZE) {
            return scaleToFixed512(bitmap)
        }
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // 2. Merkezden 512x512 kırp (bounds checking ile)
        val xOffset = ((scaledWidth - GFPGAN_INPUT_SIZE) / 2).coerceAtLeast(0)
        val yOffset = ((scaledHeight - GFPGAN_INPUT_SIZE) / 2).coerceAtLeast(0)
        
        // Ensure we don't exceed bitmap bounds
        val cropWidth = GFPGAN_INPUT_SIZE.coerceAtMost(scaledWidth - xOffset)
        val cropHeight = GFPGAN_INPUT_SIZE.coerceAtMost(scaledHeight - yOffset)
        
        val croppedBitmap = Bitmap.createBitmap(
            scaledBitmap,
            xOffset,
            yOffset,
            cropWidth,
            cropHeight
        )
        
        scaledBitmap.recycle()
        
        // Eğer crop 512x512'den küçükse, resize et
        val finalBitmap = if (cropWidth != GFPGAN_INPUT_SIZE || cropHeight != GFPGAN_INPUT_SIZE) {
            val resized = Bitmap.createScaledBitmap(croppedBitmap, GFPGAN_INPUT_SIZE, GFPGAN_INPUT_SIZE, true)
            croppedBitmap.recycle()
            resized
        } else {
            croppedBitmap
        }
        
        // Config kontrolü
        return if (finalBitmap.config != Bitmap.Config.ARGB_8888) {
            finalBitmap.copy(Bitmap.Config.ARGB_8888, false).also {
                finalBitmap.recycle()
            }
        } else {
            finalBitmap
        }
    }
    
    /**
     * Resmi 512x512'ye ölçeklendir (Aspect Ratio koruyarak - Padding)
     * 
     * Resmi sığdırır ve eksik kısımları siyah ile doldurur.
     * Tüm resim korunur ama kenarlar boş kalabilir.
     * 
     * @param bitmap Orijinal resim
     * @param paddingColor Dolgu rengi (default: siyah)
     * @return 512x512 ARGB_8888 bitmap (padding'li)
     */
    fun scaleAndPad512(bitmap: Bitmap, paddingColor: Int = 0xFF000000.toInt()): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 1. En büyük kenarı 512'ye sığdır
        val scale = min(
            GFPGAN_INPUT_SIZE.toFloat() / width,
            GFPGAN_INPUT_SIZE.toFloat() / height
        )
        
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // 2. 512x512 canvas oluştur (siyah arkaplan)
        val paddedBitmap = Bitmap.createBitmap(
            GFPGAN_INPUT_SIZE,
            GFPGAN_INPUT_SIZE,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(paddedBitmap)
        canvas.drawColor(paddingColor)
        
        // 3. Resmi ortaya çiz
        val xOffset = (GFPGAN_INPUT_SIZE - scaledWidth) / 2f
        val yOffset = (GFPGAN_INPUT_SIZE - scaledHeight) / 2f
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(scaledBitmap, xOffset, yOffset, paint)
        scaledBitmap.recycle()
        
        return paddedBitmap
    }
    
    /**
     * Resmi orijinal boyutuna geri ölçeklendir
     * 
     * GFPGAN işleminden sonra 512x512 output alırsınız.
     * Bu fonksiyon onu orijinal boyuta geri getirir.
     * 
     * @param processed512 GFPGAN output (512x512)
     * @param originalWidth Orijinal genişlik
     * @param originalHeight Orijinal yükseklik
     * @return Orijinal boyutta bitmap
     */
    fun scaleBackToOriginalSize(
        processed512: Bitmap,
        originalWidth: Int,
        originalHeight: Int
    ): Bitmap {
        require(processed512.width == GFPGAN_INPUT_SIZE && processed512.height == GFPGAN_INPUT_SIZE) {
            "Input must be 512x512, got: ${processed512.width}x${processed512.height}"
        }
        
        return Bitmap.createScaledBitmap(
            processed512,
            originalWidth,
            originalHeight,
            true // High quality filtering
        )
    }
    
    /**
     * Smart preprocess - Otomatik en iyi yöntemi seç
     * 
     * Algoritma:
     * - Kare resimler (1:1 ratio) -> Direct scale
     * - Portre resimler (dikey) -> Center crop
     * - Manzara/geniş resimler (yatay) -> Center crop
     * - Çok extreme ratio (>2:1) -> Padding
     * 
     * @param bitmap Orijinal resim
     * @return 512x512 ARGB_8888 bitmap
     */
    fun smartPreprocess512(bitmap: Bitmap): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        
        return when {
            // Kareye yakın (0.9 - 1.1 arası)
            aspectRatio in 0.9f..1.1f -> scaleToFixed512(bitmap)
            
            // Çok extreme ratio (panoramik veya çok uzun)
            aspectRatio > 2.0f || aspectRatio < 0.5f -> scaleAndPad512(bitmap)
            
            // Diğer durumlar (orta seviye portrait/landscape)
            else -> scaleAndCenterCrop512(bitmap)
        }
    }
    
    /**
     * Bitmap konfigürasyonunu ARGB_8888'e dönüştür
     * 
     * Android tarafında ARGB_8888 en güvenlidir ve ONNX Runtime ile uyumludur.
     * 
     * @param bitmap Herhangi bir config'te bitmap
     * @return ARGB_8888 bitmap
     */
    fun ensureARGB8888(bitmap: Bitmap): Bitmap {
        return if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    }
    
    /**
     * Resim bilgilerini logla (Debug için)
     */
    fun logBitmapInfo(tag: String, bitmap: Bitmap?) {
        bitmap?.let {
            android.util.Log.d(
                "ImagePreprocessor",
                "$tag: ${it.width}x${it.height}, Config: ${it.config}, " +
                "Bytes: ${it.byteCount}, HasAlpha: ${it.hasAlpha()}"
            )
        } ?: android.util.Log.w("ImagePreprocessor", "$tag: Bitmap is null")
    }
}
