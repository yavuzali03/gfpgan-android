package com.gfpgan_android.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Image Save Utility
 * Saves bitmaps to device gallery
 */
object ImageSaveUtils {
    
    /**
     * Save bitmap to gallery
     * 
     * @param context Application context
     * @param bitmap Bitmap to save
     * @param displayName File name (without extension)
     * @return URI of saved image, or null if failed
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String = "PhotoEnhancer_${System.currentTimeMillis()}"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = "$displayName.jpg"
            val mimeType = "image/jpeg"
            
            // Use MediaStore for Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoEnhancer")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }
                    
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(it, values, null, null)
                    
                    android.util.Log.d("ImageSave", "✓ Image saved to: $it")
                    return@withContext it
                }
            } else {
                // Legacy approach for older Android versions
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "PhotoEnhancer")
                
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                
                val imageFile = File(appDir, fileName)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                
                // Notify media scanner
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                
                android.util.Log.d("ImageSave", "✓ Image saved to: ${imageFile.absolutePath}")
                return@withContext uri
            }
            
            android.util.Log.e("ImageSave", "✗ Failed to save image")
            return@withContext null
            
        } catch (e: Exception) {
            android.util.Log.e("ImageSave", "✗ Error saving image: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }
}
