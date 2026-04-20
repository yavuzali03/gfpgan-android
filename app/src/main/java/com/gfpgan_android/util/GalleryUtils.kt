package com.gfpgan_android.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GalleryUtils - Galeriden fotoğraf okuma yardımcı sınıfı
 * 
 * MediaStore kullanarak galeriden fotoğrafları okur
 */
object GalleryUtils {
    
    /**
     * Galeriden tüm fotoğrafları getirir
     * 
     * @param context Android context
     * @return Fotoğraf URI listesi (en yeni önce)
     */
    suspend fun getGalleryImages(context: Context): List<Uri> = withContext(Dispatchers.IO) {
        val images = mutableListOf<Uri>()
        
        // MediaStore query
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                images.add(contentUri)
            }
        }
        
        images
    }
}
