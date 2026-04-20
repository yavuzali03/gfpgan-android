package com.gfpgan_android.util

import android.Manifest
import android.os.Build

/**
 * PermissionUtils - İzin yönetimi için yardımcı sınıf
 * 
 * Android 13+ (API 33+) ile uyumlu izin yönetimi
 */
object PermissionUtils {
    
    /**
     * Galeri erişimi için gerekli izinler
     * Android 13+ için READ_MEDIA_IMAGES
     * Android 12 ve altı için READ_EXTERNAL_STORAGE
     */
    fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Android 12 ve öncesi
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Kamera erişimi için gerekli izin
     * Tüm Android sürümlerinde aynı
     */
    fun getCameraPermission(): String {
        return Manifest.permission.CAMERA
    }
    
    /**
     * Tüm gerekli izinleri döndürür
     * Galeri + Kamera
     */
    fun getAllPermissions(): Array<String> {
        return getStoragePermissions() + getCameraPermission()
    }
}
