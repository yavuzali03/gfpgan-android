package com.gfpgan_android.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer

/**
 * RealESRGAN ONNX Manager - 2x upscaling using ONNX Runtime
 * 
 * Model: RealESRGAN_x2plus_fp16.onnx (Float16 - 35MB)
 * Upscale Factor: 2x
 * Backend: ONNX Runtime
 */
class RealESRGANOnnxManager(private val context: Context) {
    
    private val tag = "RealESRGANOnnx"
    private var ortSession: OrtSession? = null
    private var ortEnv: OrtEnvironment? = null
    
    // Model specifications (to be determined from actual model)
    private val upscaleFactor = 2
    
    init {
        setupOnnxRuntime()
    }
    
    private fun setupOnnxRuntime() {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setInterOpNumThreads(4)
            }
            
            // Copy model from assets to internal storage
            val modelFile = File(context.filesDir, "ai_models/RealESRGAN_x2plus_hybrid.onnx")
            
            var shouldCopy = true
            try {
                val assetDescriptor = context.assets.openFd("ai_models/RealESRGAN_x2plus_hybrid.onnx")
                val assetSize = assetDescriptor.length
                assetDescriptor.close()
                if (modelFile.exists() && modelFile.length() == assetSize) {
                    shouldCopy = false
                }
            } catch (e: Exception) {
                if (modelFile.exists() && modelFile.length() > 1024) { // Pointers are small
                    shouldCopy = false
                }
            }

            if (shouldCopy) {
                Log.d(tag, "Copying RealESRGAN Hybrid model to internal storage...")
                context.assets.open("ai_models/RealESRGAN_x2plus_hybrid.onnx").use { input ->
                    modelFile.parentFile?.mkdirs()
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(tag, "Model copied: ${modelFile.length()} bytes")
            }
            
            // Load from file path
            ortSession = ortEnv?.createSession(
                modelFile.absolutePath,
                sessionOptions
            )
            
            Log.d(tag, "✅ RealESRGAN ONNX model loaded")
            
            // Log input/output info
            ortSession?.let { session ->
                Log.d(tag, "   Input: ${session.inputNames}")
                Log.d(tag, "   Output: ${session.outputNames}")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "❌ RealESRGAN ONNX loading failed", e)
            throw e
        }
    }
    
    /**
     * Upscale image by 2x
     * 
     * @param inputBitmap Input image (any size)
     * @return Upscaled image (2x size)
     */
    suspend fun upscaleImage(inputBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val session = ortSession ?: throw IllegalStateException("RealESRGAN not initialized")
        
        Log.d(tag, "🎨 RealESRGAN 2x upscaling...")
        Log.d(tag, "   Input: ${inputBitmap.width}x${inputBitmap.height}")
        
        // Tile size with overlap to prevent seams
        val tileSize = 256
        val overlap = 32  // Overlap padding on each side
        val outputTileSize = tileSize * upscaleFactor
        val outputOverlap = overlap * upscaleFactor
        
        // If image is small enough, process directly
        if (inputBitmap.width <= tileSize && inputBitmap.height <= tileSize) {
            Log.d(tag, "   Processing single tile (no tiling needed)")
            return@withContext processSingleImage(session, inputBitmap)
        }
        
        // Tiling for large images
        Log.d(tag, "   Image too large, using tiling with overlap...")
        Log.d(tag, "   Tile: ${tileSize}x${tileSize}, Overlap: ${overlap}px")
        
        val outputWidth = inputBitmap.width * upscaleFactor
        val outputHeight = inputBitmap.height * upscaleFactor
        
        val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outputBitmap)
        
        val numTilesX = (inputBitmap.width + tileSize - 1) / tileSize
        val numTilesY = (inputBitmap.height + tileSize - 1) / tileSize
        
        Log.d(tag, "   Tiles: ${numTilesX}x${numTilesY} = ${numTilesX * numTilesY} total")
        
        var tileCount = 0
        for (ty in 0 until numTilesY) {
            for (tx in 0 until numTilesX) {
                tileCount++
                
                // Tile position with overlap
                val x = maxOf(0, tx * tileSize - overlap)
                val y = maxOf(0, ty * tileSize - overlap)
                val width = minOf(tileSize + 2 * overlap, inputBitmap.width - x)
                val height = minOf(tileSize + 2 * overlap, inputBitmap.height - y)
                
                Log.d(tag, "   Processing tile $tileCount/${numTilesX * numTilesY}: ${width}x${height} at ($x,$y)")
                
                // Extract tile with overlap
                val tile = Bitmap.createBitmap(inputBitmap, x, y, width, height)
                
                // Process tile
                val upscaledTile = processSingleImage(session, tile)
                tile.recycle()
                
                // Calculate crop region (remove overlap from upscaled tile)
                val cropLeft = if (tx > 0) outputOverlap else 0
                val cropTop = if (ty > 0) outputOverlap else 0
                val cropRight = if (tx < numTilesX - 1) outputOverlap else 0
                val cropBottom = if (ty < numTilesY - 1) outputOverlap else 0
                
                val cropWidth = upscaledTile.width - cropLeft - cropRight
                val cropHeight = upscaledTile.height - cropTop - cropBottom
                
                // Crop to remove overlap
                val croppedTile = Bitmap.createBitmap(
                    upscaledTile, 
                    cropLeft, 
                    cropTop, 
                    cropWidth, 
                    cropHeight
                )
                upscaledTile.recycle()
                
                // Place cropped tile on output canvas
                val outputX = tx * outputTileSize
                val outputY = ty * outputTileSize
                canvas.drawBitmap(croppedTile, outputX.toFloat(), outputY.toFloat(), null)
                croppedTile.recycle()
                
                // Force GC every few tiles
                if (tileCount % 4 == 0) {
                    System.gc()
                }
            }
        }
        
        Log.d(tag, "✅ Upscaled to: ${outputBitmap.width}x${outputBitmap.height}")
        
        return@withContext outputBitmap
    }
    
    /**
     * Process a single image or tile through RealESRGAN
     */
    private fun processSingleImage(session: OrtSession, bitmap: Bitmap): Bitmap {
        var workingBitmap = bitmap
        
        // RealESRGAN requires EVEN dimensions for pixel shuffle
        val needsPadding = (bitmap.width % 2 != 0) || (bitmap.height % 2 != 0)
        if (needsPadding) {
            val evenWidth = if (bitmap.width % 2 == 0) bitmap.width else bitmap.width + 1
            val evenHeight = if (bitmap.height % 2 == 0) bitmap.height else bitmap.height + 1
            
            val paddedBitmap = Bitmap.createBitmap(evenWidth, evenHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(paddedBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            workingBitmap = paddedBitmap
        }
        
        // Process through model
        val upscaled = processImage(session, workingBitmap)
        
        // Clean up padding
        if (needsPadding) {
            workingBitmap.recycle()
            
            // Crop back to target size
            val targetWidth = bitmap.width * upscaleFactor
            val targetHeight = bitmap.height * upscaleFactor
            
            val cropped = Bitmap.createBitmap(upscaled, 0, 0, targetWidth, targetHeight)
            upscaled.recycle()
            return cropped
        }
        
        return upscaled
    }
    
    /**
     * Process a single image through RealESRGAN
     */
    private fun processImage(session: OrtSession, bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Convert to float array [1, 3, H, W] normalized [0, 1]
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val inputArray = FloatArray(1 * 3 * height * width)
        var idx = 0
        
        // NCHW format: [batch, channel, height, width]
        for (c in 0 until 3) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixels[y * width + x]
                    val value = when (c) {
                        0 -> ((pixel shr 16) and 0xFF) / 255f // R
                        1 -> ((pixel shr 8) and 0xFF) / 255f  // G
                        else -> (pixel and 0xFF) / 255f       // B
                    }
                    inputArray[idx++] = value
                }
            }
        }
        
        // Create ONNX tensor
        val inputName = session.inputNames.iterator().next()
        val shape = longArrayOf(1, 3, height.toLong(), width.toLong())
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
        
        // Convert back to bitmap
        val outHeight = outputTensor[0][0].size
        val outWidth = outputTensor[0][0][0].size
        
        val outputBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(outWidth * outHeight)
        
        for (y in 0 until outHeight) {
            for (x in 0 until outWidth) {
                val r = (outputTensor[0][0][y][x] * 255f).toInt().coerceIn(0, 255)
                val g = (outputTensor[0][1][y][x] * 255f).toInt().coerceIn(0, 255)
                val b = (outputTensor[0][2][y][x] * 255f).toInt().coerceIn(0, 255)
                outputPixels[y * outWidth + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        outputBitmap.setPixels(outputPixels, 0, outWidth, 0, 0, outWidth, outHeight)
        return outputBitmap
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        ortSession?.close()
        ortSession = null
        Log.d(tag, "🧹 RealESRGAN ONNX cleaned up")
    }
}
