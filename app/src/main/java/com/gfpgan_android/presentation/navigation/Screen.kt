package com.gfpgan_android.presentation.navigation

/**
 * Sealed class defining all navigation routes in the app
 * Clean Architecture: Presentation Layer
 */
sealed class Screen(val route: String) {
    
    /**
     * Home screen - Ana ekran
     * Fotoğraf seçimi ve geçmiş işlemler
     */
    data object Home : Screen("home")
    
    /**
     * Enhancement screen - Fotoğraf iyileştirme ekranı
     * AI işlemi yapılırken progress gösterimi
     * @param imageUri: Seçilen fotoğrafın URI'si (navigation argument)
     */
    data object Enhancement : Screen("enhancement/{imageUri}") {
        fun createRoute(imageUri: String): String {
            return "enhancement/$imageUri"
        }
    }
    
    /**
     * Result screen - Sonuç ekranı
     * İyileştirilmiş fotoğrafı göster, kaydet, paylaş
     * @param resultUri: İyileştirilmiş fotoğrafın URI'si
     */
    data object Result : Screen("result/{resultUri}") {
        fun createRoute(resultUri: String): String {
            return "result/$resultUri"
        }
    }
    
    /**
     * Settings screen - Ayarlar ekranı (opsiyonel)
     */
    data object Settings : Screen("settings")
    
    /**
     * Face Restore screen - GFPGAN yüz restorasyonu
     * ONNX Runtime tabanlı yüz iyileştirme ekranı
     */
    data object FaceRestore : Screen("face_restore")
    
    /**
     * CodeFormer Only screen - Pure CodeFormer without ESRGAN
     * Direct face restoration test
     */
    data object CodeFormerOnly : Screen("codeformer_only")
    
    /**
     * RealESRGAN Test screen - Pure 2x upscaling test
     */
    data object RealESRGANTest : Screen("realesrgan_test")
    
    /**
     * Editor screen - Unified image editor
     * Model selection (GFPGAN/CodeFormer) + parameters
     */
    data object Editor : Screen("editor/{imageUri}") {
        fun createRoute(imageUri: String): String {
            return "editor/$imageUri"
        }
    }
}
