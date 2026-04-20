package com.gfpgan_android.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * ML Kit Face Detection wrapper for CodeFormer integration.
 * 
 * Detects faces in images and provides bounding boxes for targeted processing.
 */
class FaceDetectionManager(
    private val context: Context
) {
    
    private val tag = "FaceDetectionManager"
    private val detector: FaceDetector
    
    /**
     * Detected face data.
     */
    data class DetectedFace(
        val boundingBox: Rect,      // Face bounding box
        val confidence: Float        // Detection confidence (always 1.0 for ML Kit)
    )
    
    init {
        // Configure ML Kit Face Detector
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)  // Minimum face size: 10% of image
            .enableTracking()
            .build()
        
        detector = FaceDetection.getClient(options)
        
        Log.d(tag, "✅ ML Kit Face Detector initialized")
    }
    
    /**
     * Detect faces in a bitmap.
     * 
     * @param bitmap Input image
     * @return List of detected faces with bounding boxes
     */
    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> = withContext(Dispatchers.Default) {
        try {
            Log.d(tag, "🔍 Detecting faces in ${bitmap.width}x${bitmap.height} image...")
            
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(inputImage).await()
            
            val detectedFaces = faces.map { face ->
                DetectedFace(
                    boundingBox = face.boundingBox,
                    confidence = 1.0f  // ML Kit doesn't provide confidence
                )
            }
            
            Log.d(tag, "   ✅ Detected ${detectedFaces.size} face(s)")
            
            detectedFaces.forEach { face ->
                Log.d(tag, "   📦 Face: ${face.boundingBox}")
            }
            
            return@withContext detectedFaces
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Face detection failed", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Expand a rectangle with padding.
     * 
     * @param rect Original rectangle
     * @param padding Padding ratio (0.3 = 30% padding)
     * @param imageWidth Image width for bounds checking
     * @param imageHeight Image height for bounds checking
     * @return Expanded rectangle
     */
    fun expandRect(
        rect: Rect, 
        padding: Float,
        imageWidth: Int,
        imageHeight: Int
    ): Rect {
        val width = rect.width()
        val height = rect.height()
        
        val padW = (width * padding).toInt()
        val padH = (height * padding).toInt()
        
        return Rect(
            maxOf(0, rect.left - padW),
            maxOf(0, rect.top - padH),
            minOf(imageWidth, rect.right + padW),
            minOf(imageHeight, rect.bottom + padH)
        )
    }
    
    /**
     * Cleanup resources.
     */
    fun cleanup() {
        detector.close()
        Log.d(tag, "🧹 Face Detector cleaned up")
    }
}
