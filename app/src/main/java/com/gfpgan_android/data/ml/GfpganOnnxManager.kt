package com.gfpgan_android.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.onnxruntime.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.*

/**
 * GFPGAN face enhancement using ONNX Runtime.
 * 
 * Processes images by resizing to multiples of 512, then tiling.
 */
class GfpganOnnxManager(
    private val context: Context
) {
    
    private val tag = "GfpganOnnxManager"
    private var ortSession: OrtSession? = null
    private var ortEnv: OrtEnvironment? = null
    
    // GFPGAN model input/output size
    private val modelSize = 512
    
    init {
        setupOnnxRuntime()
    }
    
    private fun setupOnnxRuntime() {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setInterOpNumThreads(4)
                // Use CPU for now (can add GPU delegate later)
            }
            
            // Copy model from assets to internal storage (avoid OOM)
            val modelFile = File(context.filesDir, "GFPGANv1.4_mobile_fp16_fixed.onnx")
            
            if (!modelFile.exists() || modelFile.length() == 0L) {
                Log.d(tag, "📥 Copying GFPGAN FP16 model to internal storage...")
                context.assets.open("GFPGANv1.4_mobile_fp16_fixed.onnx").use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(tag, "Model copied: ${modelFile.length()} bytes")
            }
            
            // Load from file path (avoids loading 341MB into memory)
            ortSession = ortEnv?.createSession(
                modelFile.absolutePath,
                sessionOptions
            )
            
            Log.d(tag, "✅ GFPGAN ONNX model loaded")
            
            // Log input/output info
            ortSession?.let { session ->
                Log.d(tag, "   Input: ${session.inputNames}")
                Log.d(tag, "   Output: ${session.outputNames}")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "❌ GFPGAN ONNX loading failed", e)
            throw e
        }
    }
    
    /**
     * Enhance image using GFPGAN.
     * 
     * Strategy: Resize to multiple of 512 → Process in perfect tiles → Restore aspect ratio
     * 
     * @param inputBitmap Input image
     * @return Enhanced image (same size as input)
     */
    suspend fun enhance(inputBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val session = ortSession ?: throw IllegalStateException("GFPGAN not initialized")
        
        Log.d(tag, "🎨 GFPGAN enhancement starting...")
        Log.d(tag, "   Input: ${inputBitmap.width}x${inputBitmap.height}")
        
        val originalWidth = inputBitmap.width
        val originalHeight = inputBitmap.height
        
        // Step 1: Resize to nearest multiple of 512 (avoids partial tiles)
        val tileSize = 512
        val resizedWidth = ((originalWidth + tileSize - 1) / tileSize) * tileSize
        val resizedHeight = ((originalHeight + tileSize - 1) / tileSize) * tileSize
        
        Log.d(tag, "   Resizing to: ${resizedWidth}x${resizedHeight} (multiple of 512)")
        
        val resizedInput = Bitmap.createScaledBitmap(
            inputBitmap,
            resizedWidth,
            resizedHeight,
            true
        )
        
        // Step 2: Process with GFPGAN in perfect 512x512 tiles
        val width = resizedInput.width
        val height = resizedInput.height
        
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        var tilesProcessed = 0
        val totalTiles = (width / tileSize) * (height / tileSize)
        
        Log.d(tag, "   🎭 Processing ${totalTiles} tiles (${width / tileSize} x ${height / tileSize})...")
        
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                // Extract perfect 512x512 tile
                val tile = Bitmap.createBitmap(resizedInput, x, y, tileSize, tileSize)
                
                // Process with GFPGAN
                val enhancedTile = processGfpganTile(session, tile)
                tile.recycle()
                
                // Paste to output
                canvas.drawBitmap(enhancedTile, x.toFloat(), y.toFloat(), null)
                enhancedTile.recycle()
                
                tilesProcessed++
                if (tilesProcessed % 5 == 0 || tilesProcessed == totalTiles) {
                    Log.d(tag, "   Progress: $tilesProcessed/$totalTiles tiles")
                }
                
                x += tileSize
            }
            y += tileSize
        }
        
        resizedInput.recycle()
        
        // Step 3: Resize back to original dimensions (restore aspect ratio)
        Log.d(tag, "   Resizing back to: ${originalWidth}x${originalHeight}")
        
        val finalOutput = Bitmap.createScaledBitmap(
            output,
            originalWidth,
            originalHeight,
            true
        )
        output.recycle()
        
        Log.d(tag, "✅ GFPGAN enhancement complete!")
        return@withContext finalOutput
    }
    
    /**
     * Enhance image using GFPGAN with face detection.
     * 
     * Only processes detected face regions instead of full image tiling.
     * Falls back to full-image enhancement if no faces detected.
     * 
     * @param inputBitmap Input image
     * @param faceDetector Face detection manager
     * @param padding Padding ratio around detected faces (0.3 = 30%)
     * @return Enhanced image (same size as input)
     */
    suspend fun enhanceWithFaceDetection(
        inputBitmap: Bitmap,
        faceDetector: FaceDetectionManager,
        padding: Float = 0.3f
    ): Bitmap = withContext(Dispatchers.Default) {
        val session = ortSession ?: throw IllegalStateException("GFPGAN not initialized")
        
        Log.d(tag, "🎨 GFPGAN enhancement with face detection...")
        Log.d(tag, "   Input: ${inputBitmap.width}x${inputBitmap.height}")
        
        // Step 1: Detect faces
        val faces = faceDetector.detectFaces(inputBitmap)
        
        if (faces.isEmpty()) {
            Log.d(tag, "   ⚠️ No faces detected, falling back to full-image tiling")
            return@withContext enhance(inputBitmap)
        }
        
        Log.d(tag, "   ✅ Detected ${faces.size} face(s), processing each...")
        
        // Step 2: Create output bitmap (copy of input)
        val output = inputBitmap.copy(inputBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        
        // Step 3: Process each detected face
        faces.forEachIndexed { index, face ->
            try {
                // Expand bounding box with padding
                val paddedRect = faceDetector.expandRect(
                    face.boundingBox,
                    padding,
                    inputBitmap.width,
                    inputBitmap.height
                )
                
                Log.d(tag, "   Processing face ${index + 1}/${faces.size}: $paddedRect")
                
                // Crop face region
                val faceCrop = Bitmap.createBitmap(
                    inputBitmap,
                    paddedRect.left,
                    paddedRect.top,
                    paddedRect.width(),
                    paddedRect.height()
                )
                
                // Resize to 512x512 for GFPGAN
                val resized = Bitmap.createScaledBitmap(faceCrop, 512, 512, true)
                faceCrop.recycle()
                
                // Process with GFPGAN
                val enhanced = processGfpganTile(session, resized)
                resized.recycle()
                
                // Resize back to original crop size
                val finalFace = Bitmap.createScaledBitmap(
                    enhanced,
                    paddedRect.width(),
                    paddedRect.height(),
                    true
                )
                enhanced.recycle()
                
                // Paste back to original position
                canvas.drawBitmap(
                    finalFace,
                    paddedRect.left.toFloat(),
                    paddedRect.top.toFloat(),
                    null
                )
                finalFace.recycle()
                
            } catch (e: Exception) {
                Log.e(tag, "   ❌ Failed to process face ${index + 1}", e)
            }
        }
        
        Log.d(tag, "✅ GFPGAN face enhancement complete!")
        return@withContext output
    }
    
    /**
     * Process a single 512x512 tile through GFPGAN.
     */
    private fun processGfpganTile(session: OrtSession, tile: Bitmap): Bitmap {
        // Ensure 512x512
        require(tile.width == 512 && tile.height == 512) {
            "GFPGAN requires 512x512 input"
        }
        
        // Convert to float array [1, 3, 512, 512] normalized [-1, 1]
        val pixels = IntArray(512 * 512)
        tile.getPixels(pixels, 0, 512, 0, 0, 512, 512)
        
        val inputArray = FloatArray(1 * 3 * 512 * 512)
        var idx = 0
        
        // NCHW format: [batch, channel, height, width]
        for (c in 0 until 3) {
            for (y in 0 until 512) {
                for (x in 0 until 512) {
                    val pixel = pixels[y * 512 + x]
                    val value = when (c) {
                        0 -> ((pixel shr 16) and 0xFF) / 127.5f - 1f // R
                        1 -> ((pixel shr 8) and 0xFF) / 127.5f - 1f  // G
                        else -> (pixel and 0xFF) / 127.5f - 1f       // B
                    }
                    inputArray[idx++] = value
                }
            }
        }
        
        // Create ONNX tensor
        val inputName = session.inputNames.iterator().next()
        val shape = longArrayOf(1, 3, 512, 512)
        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            FloatBuffer.wrap(inputArray),
            shape
        )
        
        // Run inference
        val inputs = mapOf(inputName to inputTensor)
        val results = session.run(inputs)
        
        // Get output
        val outputTensor = results[0].value as Array<Array<Array<FloatArray>>>
        results.close()
        inputTensor.close()
        
        // Convert back to bitmap [1, 3, 512, 512] -> Bitmap
        val outputBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(512 * 512)
        
        for (y in 0 until 512) {
            for (x in 0 until 512) {
                val r = ((outputTensor[0][0][y][x] + 1f) * 127.5f).toInt().coerceIn(0, 255)
                val g = ((outputTensor[0][1][y][x] + 1f) * 127.5f).toInt().coerceIn(0, 255)
                val b = ((outputTensor[0][2][y][x] + 1f) * 127.5f).toInt().coerceIn(0, 255)
                outputPixels[y * 512 + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        outputBitmap.setPixels(outputPixels, 0, 512, 0, 0, 512, 512)
        return outputBitmap
    }
    
    /**
     * Cleanup resources.
     */
    fun cleanup() {
        ortSession?.close()
        ortSession = null
        Log.d(tag, "🧹 GFPGAN ONNX cleaned up")
    }
}
