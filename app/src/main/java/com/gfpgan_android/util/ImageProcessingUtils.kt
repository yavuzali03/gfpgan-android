package com.gfpgan_android.util

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.sin

/**
 * OpenCV-based image processing utilities.
 * Replicates Python ImageProcessor feature methods.
 */
object ImageProcessingUtils {
    
    private const val TAG = "ImageProcessingUtils"
    
    /**
     * Python: _feature_halftone_fix_restore()
     * 
     * Gaussian blur denoising:
     * - Downscale 2x
     * - Apply Gaussian blur
     * - Upscale back with LANCZOS
     */
    fun applyGaussianDenoising(bitmap: Bitmap): Bitmap {
        Log.d(TAG, "      -> 📉💧📈 [GAUSSIAN BLUR] Tram eritiliyor...")
        
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)
        
        val origWidth = mat.cols()
        val origHeight = mat.rows()
        
        // Downscale 2x
        val small = Mat()
        Imgproc.resize(
            mat, small, 
            Size((origWidth / 2.0), (origHeight / 2.0)), 
            0.0, 0.0, 
            Imgproc.INTER_AREA
        )
        
        // Gaussian blur
        Imgproc.GaussianBlur(small, small, Size(3.0, 3.0), 0.0)
        
        // Upscale back with LANCZOS
        val restored = Mat()
        Imgproc.resize(
            small, restored, 
            Size(origWidth.toDouble(), origHeight.toDouble()), 
            0.0, 0.0, 
            Imgproc.INTER_LANCZOS4
        )
        
        // Convert back to Bitmap
        val output = Bitmap.createBitmap(
            restored.cols(), restored.rows(), 
            Bitmap.Config.ARGB_8888
        )
        Imgproc.cvtColor(restored, restored, Imgproc.COLOR_RGB2RGBA)
        Utils.matToBitmap(restored, output)
        
        // Cleanup
        mat.release()
        small.release()
        restored.release()
        
        return output
    }
    
    /**
     * Python: _feature_master_curve()
     * 
     * Apply master curve for light/shadow adjustment.
     * 
     * @param bendFactor Curve strength (negative = darken shadows)
     */
    fun applyMasterCurve(bitmap: Bitmap, bendFactor: Float = -0.12f): Bitmap {
        Log.d(TAG, "      -> 📈 [MASTER CURVE] Ton eğrisi uygulanıyor...")
        
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)
        
        // Create LUT (Look-Up Table)
        val lut = Mat(1, 256, CvType.CV_8UC1)
        val lutData = ByteArray(256)
        for (i in 0 until 256) {
            val x = i / 255.0
            val y = (x + bendFactor * sin(x * PI)) * 255.0
            lutData[i] = y.coerceIn(0.0, 255.0).toInt().toByte()
        }
        lut.put(0, 0, lutData)
        
        // Apply LUT
        val result = Mat()
        Core.LUT(mat, lut, result)
        
        // Convert back
        val output = Bitmap.createBitmap(
            result.cols(), result.rows(), 
            Bitmap.Config.ARGB_8888
        )
        Imgproc.cvtColor(result, result, Imgproc.COLOR_RGB2RGBA)
        Utils.matToBitmap(result, output)
        
        // Cleanup
        mat.release()
        lut.release()
        result.release()
        
        return output
    }
    
    /**
     * Python: _feature_lightroom_texture()
     * 
     * Lightroom-style texture enhancement (unsharp mask).
     * 
     * @param intensity Texture strength (0.0 - 1.0)
     */
    fun applyTexture(bitmap: Bitmap, intensity: Float = 0.25f): Bitmap {
        if (intensity <= 0.0f) return bitmap
        
        Log.d(TAG, "      -> ✨ [TEXTURE] Doku güçlendiriliyor (${intensity})...")
        
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)
        
        // Gaussian blur
        val blurred = Mat()
        Imgproc.GaussianBlur(mat, blurred, Size(0.0, 0.0), 3.0)
        
        // Unsharp mask: original * (1 + intensity) - blurred * intensity
        val sharpened = Mat()
        Core.addWeighted(
            mat, (1.0 + intensity).toDouble(), 
            blurred, -intensity.toDouble(), 
            0.0, 
            sharpened
        )
        
        // Convert back
        val output = Bitmap.createBitmap(
            sharpened.cols(), sharpened.rows(), 
            Bitmap.Config.ARGB_8888
        )
        Imgproc.cvtColor(sharpened, sharpened, Imgproc.COLOR_RGB2RGBA)
        Utils.matToBitmap(sharpened, output)
        
        // Cleanup
        mat.release()
        blurred.release()
        sharpened.release()
        
        return output
    }
    
    /**
     * Python: _feature_true_shadow_recovery()
     * 
     * Lightroom-style shadow recovery using tone mapping.
     * 
     * @param strength Shadow recovery strength (0.0 - 1.0)
     */
    fun applyShadowRecovery(bitmap: Bitmap, strength: Float = 0.5f): Bitmap {
        Log.d(TAG, "      -> 🌓 [PRO SHADOWS] Karanlık alanlar Tone Mapping ile işleniyor...")
        
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)
        
        // Convert to float
        val floatMat = Mat()
        mat.convertTo(floatMat, CvType.CV_32FC3, 1.0 / 255.0)
        
        // Extract luminance (weighted RGB)
        val channels = mutableListOf<Mat>()
        Core.split(floatMat, channels)
        val r = channels[0]
        val g = channels[1]
        val b = channels[2]
        
        val luminance = Mat()
        Core.addWeighted(r, 0.299, g, 0.587, 0.0, luminance)
        Core.addWeighted(luminance, 1.0, b, 0.114, 0.0, luminance)
        
        // Create shadow mask (dark areas < 0.35)
        val darkThreshold = 0.35
        val shadowMask = Mat()
        
        // For each pixel: if lum < 0.35, mask = ((1 - lum/0.35)^5), else 0
        val onesmat = Mat.ones(luminance.size(), CvType.CV_32FC1)
        val thresholdMat = Mat(luminance.size(), CvType.CV_32FC1, Scalar(darkThreshold))
        
        val temp = Mat()
        val divided = Mat()
        Core.divide(luminance, thresholdMat, divided)
        Core.subtract(onesmat, divided, temp)
        Core.pow(temp, 5.0, shadowMask)
        divided.release()
        
        // Set mask to 0 where luminance >= threshold
        val maskCondition = Mat()
        Core.compare(luminance, thresholdMat, maskCondition, Core.CMP_GE)
        shadowMask.setTo(Scalar(0.0), maskCondition)
        
        // Brightening factor = 1.0 + (mask * strength)
        val brighteningFactor = Mat()
        Core.addWeighted(
            onesmat, 1.0, 
            shadowMask, strength.toDouble(), 
            0.0, 
            brighteningFactor
        )
        
        // Apply to each channel
        for (i in 0..2) {
            Core.multiply(channels[i], brighteningFactor, channels[i])
        }
        
        // Merge channels
        val result = Mat()
        Core.merge(channels, result)
        
        // Saturation boost in affected areas (HSV)
        val resultU8 = Mat()
        result.convertTo(resultU8, CvType.CV_8UC3, 255.0)
        
        val hsv = Mat()
        Imgproc.cvtColor(resultU8, hsv, Imgproc.COLOR_RGB2HSV)
        
        val hsvChannels = mutableListOf<Mat>()
        Core.split(hsv, hsvChannels)
        
        // Boost saturation where shadows were recovered
        val saturationFloat = Mat()
        hsvChannels[1].convertTo(saturationFloat, CvType.CV_32FC1)
        
        val satBoost = Mat()
        Core.addWeighted(
            onesmat, 1.0, 
            shadowMask, 0.15, 
            0.0, 
            satBoost
        )
        
        // Resize satBoost to match saturation size
        Imgproc.resize(satBoost, satBoost, saturationFloat.size())
        Core.multiply(saturationFloat, satBoost, saturationFloat)
        
        // Clip to [0, 255]
        saturationFloat.convertTo(hsvChannels[1], CvType.CV_8UC1)
        Core.min(hsvChannels[1], Scalar(255.0), hsvChannels[1])
        
        // Merge and convert back to RGB
        Core.merge(hsvChannels, hsv)
        val finalRgb = Mat()
        Imgproc.cvtColor(hsv, finalRgb, Imgproc.COLOR_HSV2RGB)
        
        // Convert back to Bitmap
        val output = Bitmap.createBitmap(
            finalRgb.cols(), finalRgb.rows(), 
            Bitmap.Config.ARGB_8888
        )
        Imgproc.cvtColor(finalRgb, finalRgb, Imgproc.COLOR_RGB2RGBA)
        Utils.matToBitmap(finalRgb, output)
        
        // Cleanup
        mat.release()
        floatMat.release()
        channels.forEach { it.release() }
        luminance.release()
        shadowMask.release()
        brighteningFactor.release()
        result.release()
        resultU8.release()
        hsv.release()
        hsvChannels.forEach { it.release() }
        saturationFloat.release()
        satBoost.release()
        onesmat.release()
        thresholdMat.release()
        temp.release()
        maskCondition.release()
        finalRgb.release()
        
        return output
    }
    
    /**
     * OpenCV addWeighted for blending two bitmaps.
     * Python: cv2.addWeighted(src1, alpha, src2, beta, 0)
     * 
     * Result = src1 * alpha + src2 * beta
     */
    fun addWeighted(
        src1: Bitmap, 
        alpha: Float, 
        src2: Bitmap, 
        beta: Float
    ): Bitmap {
        require(src1.width == src2.width && src1.height == src2.height) {
            "Bitmaps must have same dimensions"
        }
        
        val mat1 = Mat()
        val mat2 = Mat()
        Utils.bitmapToMat(src1, mat1)
        Utils.bitmapToMat(src2, mat2)
        
        Imgproc.cvtColor(mat1, mat1, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(mat2, mat2, Imgproc.COLOR_RGBA2RGB)
        
        val result = Mat()
        Core.addWeighted(
            mat1, alpha.toDouble(), 
            mat2, beta.toDouble(), 
            0.0, 
            result
        )
        
        val output = Bitmap.createBitmap(
            result.cols(), result.rows(), 
            Bitmap.Config.ARGB_8888
        )
        Imgproc.cvtColor(result, result, Imgproc.COLOR_RGB2RGBA)
        Utils.matToBitmap(result, output)
        
        // Cleanup
        mat1.release()
        mat2.release()
        result.release()
        
        return output
    }
}
