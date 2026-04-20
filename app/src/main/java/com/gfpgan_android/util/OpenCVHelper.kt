package com.gfpgan_android.util

import android.content.Context
import org.opencv.android.OpenCVLoader

/**
 * OpenCV Initialization Helper
 * 
 * OpenCV kütüphanesini başlatır
 */
object OpenCVHelper {
    
    private var isInitialized = false
    
    /**
     * OpenCV'yi başlat
     * @return Başarılı ise true
     */
    fun initOpenCV(context: Context): Boolean {
        if (isInitialized) return true
        
        return try {
            // QuickBird Studios OpenCV wrapper için initDebug kullanılır
            isInitialized = OpenCVLoader.initDebug()
            
            if (!isInitialized) {
                android.util.Log.e("OpenCVHelper", "OpenCV initialization failed!")
            } else {
                android.util.Log.i("OpenCVHelper", "OpenCV initialized successfully")
            }
            
            isInitialized
        } catch (e: Exception) {
            android.util.Log.e("OpenCVHelper", "OpenCV initialization error: ${e.message}")
            false
        }
    }
    
    /**
     * OpenCV başlatıldı mı kontrol et
     */
    fun isOpenCVInitialized(): Boolean = isInitialized
}
