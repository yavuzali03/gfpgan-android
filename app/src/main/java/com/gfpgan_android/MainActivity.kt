package com.gfpgan_android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gfpgan_android.presentation.navigation.AppNavGraph
import com.gfpgan_android.ui.theme.PhotoEnhancerTheme
import com.gfpgan_android.util.OpenCVHelper
import com.gfpgan_android.util.GFPGANNative
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    // Global erişim için (GFPGANNative instance)
    lateinit var gfpganNative: GFPGANNative

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. OpenCV'yi başlat (Eğer hala kullanıyorsan)
        OpenCVHelper.initOpenCV(this)

        // 2. GFPGAN ONNX Modelini Hazırla
        try {
            // JNI Sınıfını Başlat
            gfpganNative = GFPGANNative()

            // Modeli Assets'ten Telefona Kopyala (C++ okuyabilsin diye)
            val modelPath = getModelPath("GFPGANv1.4_mobile_fp16_fixed.onnx")

            // Native Init Fonksiyonunu Çağır
            gfpganNative.initModel(modelPath)

            Log.d("MainActivity", "✅ GFPGAN (ONNX) FP16 Modeli Başarıyla Yüklendi: $modelPath")

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ GFPGAN model yükleme hatası: ${e.message}")
        }

        setContent {
            val systemDarkMode = isSystemInDarkTheme()
            var isDarkMode by rememberSaveable { mutableStateOf(systemDarkMode) }

            PhotoEnhancerTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier,
                        isDarkMode = isDarkMode,
                        onThemeToggle = { isDarkMode = it }
                    )
                }
            }
        }
    }

    /**
     * Assets klasöründeki modeli telefonun dahili hafızasına kopyalar
     * ve dosya yolunu (String) döndürür.
     */
    private fun getModelPath(assetName: String): String {
        val file = File(filesDir, assetName)

        // Dosya zaten varsa ve boyutu > 0 ise tekrar kopyalama (Performans)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        try {
            assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Dosya kopyalanamadı: $assetName", e)
            throw e
        }

        return file.absolutePath
    }
}