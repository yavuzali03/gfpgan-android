package com.gfpgan_android.util

import android.graphics.Bitmap
import android.util.Log
import java.lang.ref.WeakReference

/**
 * Memory management utilities for bitmap-heavy operations.
 * 
 * Python equivalent: utils.flush_memory()
 */
object MemoryUtils {
    
    private const val TAG = "MemoryUtils"
    private val bitmapRefs = mutableListOf<WeakReference<Bitmap>>()
    
    /**
     * Python equivalent: flush_memory()
     * 
     * Aggressively cleans up memory:
     * - Calls System.gc()
     * - Recycles tracked bitmaps
     */
    fun flushMemory() {
        try {
            // Clean up tracked bitmaps
            bitmapRefs.removeAll { ref ->
                val bitmap = ref.get()
                if (bitmap != null && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
                true // Remove all references
            }
            
            // Force garbage collection (aggressive)
            System.gc()
            System.runFinalization()
            
            Log.d(TAG, "🧹 Memory flushed")
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing memory", e)
        }
    }
    
    /**
     * Track a bitmap for automatic cleanup.
     * Use this when creating temporary bitmaps.
     */
    fun trackBitmap(bitmap: Bitmap) {
        bitmapRefs.add(WeakReference(bitmap))
    }
    
    /**
     * Safely recycle a bitmap if not already recycled.
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
                Log.d(TAG, "♻️ Bitmap recycled: ${it.width}x${it.height}")
            }
        }
    }
    
    /**
     * Get current memory usage info (for debugging).
     */
    fun logMemoryInfo() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        
        Log.d(TAG, "📊 Memory: ${usedMemory}MB / ${maxMemory}MB")
    }
}
