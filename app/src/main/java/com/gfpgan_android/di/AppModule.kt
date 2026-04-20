package com.gfpgan_android.di

import com.gfpgan_android.data.ai.GfpganManager
import com.gfpgan_android.data.ml.FaceDetectionManager
import com.gfpgan_android.domain.usecase.RestoreFaceUseCase
import com.gfpgan_android.presentation.viewmodels.FaceRestoreViewModel
import com.gfpgan_android.presentation.viewmodels.EditorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin Dependency Injection Module
 * 
 * Tüm bağımlılıkları merkezi olarak yönetir
 */
val appModule = module {
    
    // ==================== AI Managers ====================
    // Singleton pattern - Model yükleme maliyetli, bir kere yükle
    
    // GFPGAN: Face Restoration (512x512 fixed)
    single { GfpganManager(androidContext()) }
    
    // FaceDetectionManager: Google ML Kit face detection
    factory { FaceDetectionManager(androidContext()) }
    
    // ==================== Use Cases ====================
    // Factory pattern - Her çağrıda yeni instance
    
    // Face restoration pipeline
    factory { RestoreFaceUseCase(get()) }
    
    // ==================== ViewModels ====================
    // ViewModel scope - UI lifecycle'a bağlı
    
    viewModel { FaceRestoreViewModel(get()) }
    viewModel { EditorViewModel(androidContext(), get()) }
}
