package com.gfpgan_android.data.ai

import android.content.Context
import android.graphics.Bitmap
import com.gfpgan_android.util.GFPGANNative
import com.gfpgan_android.util.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * GfpganManager - GFPGAN (ONNX Runtime) Model Yöneticisi
 * 
 * GFPGAN: Generative Facial Prior GAN
 * - Face restoration (yüz restorasyonu)
 * - 512x512 fixed input/output
 * - ONNX Runtime backend
 * 
 * Architecture Uyumluluğu:
 * - Data Layer (RealESRGANManager ile aynı katman)
 * - UseCase'ler tarafından kullanılır
 * - Threading: Dispatchers.Default (CPU/GPU yoğun işlem)
 * 
 * @param context Application context (model yükleme için)
 */
class GfpganManager(context: Context) {

    private val gfpganNative = GFPGANNative()
    private var isModelLoaded = false
    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "GfpganManager"
        private const val MODEL_INPUT_SIZE = 512
        private const val MODEL_OUTPUT_SIZE = 512
    }

    init {
        setupModel()
    }

    /**
     * ONNX Runtime modelini yükle
     * 
     * GFPGANNative.initModel() JNI üzerinden şunları yapar:
     * 1. assets/GFPGANv1.4_mobile_fp16_fixed.onnx dosyasını internal storage'a kopyalar
     * 2. ONNX Runtime session'ı başlatır
     * 3. Model graph optimization yapar
     */
    private fun setupModel() {
        try {
            android.util.Log.i(TAG, "Loading GFPGAN ONNX model...")
            
            val modelPath = copyAssetToFile("ai_models/GFPGANv1.4_mobile_fp16_fixed.onnx")
            gfpganNative.initModel(modelPath)
            
            isModelLoaded = true
            android.util.Log.i(TAG, "✓ GFPGAN model loaded successfully from: $modelPath")
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e(TAG, "Model loading exception: ${e.message}")
            throw RuntimeException("GFPGAN model yükleme hatası: ${e.message}", e)
        }
    }

    /**
     * Assets klasöründen internal storage'a dosya kopyala
     */
    private fun copyAssetToFile(assetName: String): String {
        val file = File(appContext.filesDir, assetName)
        
        try {
            val assetDescriptor = appContext.assets.openFd(assetName)
            val assetSize = assetDescriptor.length
            assetDescriptor.close()
            
            if (file.exists() && file.length() == assetSize) {
                return file.absolutePath
            }
        } catch (e: Exception) {
            // openFd might fail for compressed assets, fallback to length check
            if (file.exists() && file.length() > 1024) { // LFS pointers are < 1KB
                return file.absolutePath
            }
        }
        
        appContext.assets.open(assetName).use { input ->
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    /**
     * Model yüklendi mi?
     */
    fun isInitialized(): Boolean = isModelLoaded

    /**
     * Yüz restorasyonu uygula (Temel fonksiyon)
     * 
     * Threading:
     * - withContext(Dispatchers.Default) kullanır
     * - UI thread'i bloke etmez
     * - GPU/Vulkan işlemleri background'da çalışır
     * 
     * Preprocessing:
     * - Otomatik 512x512'ye ölçeklendirir (smart crop/pad)
     * - ARGB_8888 formatına dönüştürür
     * 
     * Processing:
     * - ONNX Runtime inference (JNI üzerinden C++)
     * - RGB formatında işlenir
     * 
     * Postprocessing:
     * - Output 512x512 olarak döner
     * - İsteğe bağlı orijinal boyuta scale edilebilir
     * 
     * @param originalBitmap Orijinal fotoğraf (herhangi bir boyut)
     * @param scaleBackToOriginal true ise orijinal boyuta döner
     * @return İyileştirilmiş fotoğraf
     */
    suspend fun restoreFace(
        originalBitmap: Bitmap,
        scaleBackToOriginal: Boolean = true,
        useFaceDetection: Boolean = true,
        applyUpscaling: Boolean = false,
        blendRatio: Float = 0.55f  // 0.0 = pure original, 1.0 = pure AI
    ): Bitmap = withContext(Dispatchers.Default) {
        
        require(isModelLoaded) { "Model henüz yüklenmedi!" }
        
        // Input validation
        require(originalBitmap.width > 0 && originalBitmap.height > 0) {
            "Bitmap boyutları geçersiz: ${originalBitmap.width}x${originalBitmap.height}"
        }
        
        require(!originalBitmap.isRecycled) {
            "Bitmap zaten recycle edilmiş!"
        }
        
        ImagePreprocessor.logBitmapInfo("Input", originalBitmap)
        
        // If face detection is enabled, use GfpganOnnxManager instead
        if (useFaceDetection) {
            android.util.Log.d(TAG, "Using GfpganOnnxManager with face detection")
            val gfpganOnnx = com.gfpgan_android.data.ml.GfpganOnnxManager(appContext)
            val faceDetector = com.gfpgan_android.data.ml.FaceDetectionManager(appContext)
            
            val aiResult = gfpganOnnx.enhanceWithFaceDetection(
                inputBitmap = originalBitmap,
                faceDetector = faceDetector
            )
            
            faceDetector.cleanup()
            gfpganOnnx.cleanup()
            
            // Apply blending
            val blendedResult = if (blendRatio < 1.0f) {
                android.util.Log.d(TAG, "Blending with ratio: $blendRatio")
                val blended = com.gfpgan_android.util.ImageBlendingUtils.blendBitmaps(
                    aiBitmap = aiResult,
                    originalBitmap = originalBitmap,
                    aiWeight = blendRatio
                )
                aiResult.recycle()
                blended
            } else {
                aiResult
            }
            
            // Apply RealESRGAN if requested
            if (applyUpscaling) {
                android.util.Log.d(TAG, "Applying RealESRGAN 2x upscaling...")
                val esrganManager = com.gfpgan_android.data.ml.RealESRGANOnnxManager(appContext)
                val upscaled = esrganManager.upscaleImage(blendedResult)
                esrganManager.cleanup()
                blendedResult.recycle()
                return@withContext upscaled
            }
            
            return@withContext blendedResult
        }
        
        // 1. PREPROCESSING: 512x512'ye dönüştür
        val preprocessed = try {
            ImagePreprocessor.smartPreprocess512(originalBitmap)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Preprocessing error: ${e.message}")
            throw RuntimeException("Ön işleme hatası: ${e.message}", e)
        }
        ImagePreprocessor.logBitmapInfo("Preprocessed", preprocessed)
        
        // 2. INFERENCE: ONNX Runtime modelden geçir (JNI Call)
        val processed512 = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        
        try {
            android.util.Log.d(TAG, "Running GFPGAN ONNX inference...")
            val startTime = System.currentTimeMillis()
            
            gfpganNative.enhance(preprocessed, processed512)
            
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.i(TAG, "✓ Inference completed in ${duration}ms")
            
        } catch (e: Exception) {
            preprocessed.recycle()
            processed512.recycle()
            android.util.Log.e(TAG, "Inference error: ${e.message}")
            throw RuntimeException("GFPGAN inference hatası: ${e.message}", e)
        }
        
        preprocessed.recycle()
        
        ImagePreprocessor.logBitmapInfo("Output (512x512)", processed512)
        
        // 3. POSTPROCESSING: Orijinal boyuta döndür (opsiyonel)
        val restoredResult = if (scaleBackToOriginal) {
            val scaled = ImagePreprocessor.scaleBackToOriginalSize(
                processed512,
                originalBitmap.width,
                originalBitmap.height
            )
            processed512.recycle()
            ImagePreprocessor.logBitmapInfo("Final (scaled back)", scaled)
            scaled
        } else {
            processed512
        }
        
        // 3.5: Apply blending
        val blendedResult = if (blendRatio < 1.0f) {
            android.util.Log.d(TAG, "Blending with ratio: $blendRatio")
            val blended = com.gfpgan_android.util.ImageBlendingUtils.blendBitmaps(
                aiBitmap = restoredResult,
                originalBitmap = originalBitmap,
                aiWeight = blendRatio
            )
            restoredResult.recycle()
            blended
        } else {
            restoredResult
        }
        
        // 4. Apply RealESRGAN 2x upscaling if requested
        if (applyUpscaling) {
            android.util.Log.d(TAG, "Applying RealESRGAN 2x upscaling...")
            val esrganManager = com.gfpgan_android.data.ml.RealESRGANOnnxManager(appContext)
            val upscaled = esrganManager.upscaleImage(blendedResult)
            esrganManager.cleanup()
            blendedResult.recycle()
            return@withContext upscaled
        }
        
        return@withContext blendedResult
    }

    /**
     * Batch processing - Birden fazla yüzü sırayla işle
     * 
     * Use case: Grup fotoğraflarında her yüz için ayrı ayrı
     * (Gelecekte face detection ile entegre edilebilir)
     * 
     * @param bitmaps Resim listesi
     * @return İşlenmiş resim listesi
     */
    suspend fun restoreFacesBatch(bitmaps: List<Bitmap>): List<Bitmap> = withContext(Dispatchers.Default) {
        bitmaps.map { bitmap ->
            restoreFace(bitmap, scaleBackToOriginal = true)
        }
    }

    /**
     * Advanced: Manuel preprocessing ile işleme
     * 
     * UseCase tarafında özel preprocessing stratejisi kullanmak isteyenler için.
     * 
     * UYARI: inputBitmap MUTLAKA 512x512 ve ARGB_8888 olmalı!
     * 
     * @param preprocessed512 Önceden işlenmiş 512x512 bitmap
     * @return 512x512 işlenmiş bitmap
     */
    suspend fun restoreFaceRaw(preprocessed512: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        
        require(preprocessed512.width == MODEL_INPUT_SIZE && preprocessed512.height == MODEL_INPUT_SIZE) {
            "Input must be 512x512, got: ${preprocessed512.width}x${preprocessed512.height}"
        }
        
        require(preprocessed512.config == Bitmap.Config.ARGB_8888) {
            "Input must be ARGB_8888, got: ${preprocessed512.config}"
        }
        
        require(isModelLoaded) { "Model henüz yüklenmedi!" }
        
        return@withContext try {
            val outputBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            gfpganNative.enhance(preprocessed512, outputBitmap)
            outputBitmap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Raw inference error: ${e.message}")
            throw RuntimeException("GFPGAN inference hatası: ${e.message}", e)
        }
    }

    /**
     * Kaynakları temizle
     * 
     * Not: ONNX Runtime native memory yönetimi otomatiktir
     * Ancak gelecekte explicit cleanup eklenebilir
     */
    fun cleanup() {
        android.util.Log.i(TAG, "GfpganManager cleanup called")
        // ONNX Runtime cleanup JNI tarafında otomatik
        isModelLoaded = false
    }
}
