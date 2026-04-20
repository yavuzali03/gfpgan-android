package com.gfpgan_android

import android.app.Application
import com.gfpgan_android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * PhotoEnhancerApplication - Ana Application sınıfı
 * 
 * Koin DI başlatma
 */
class PhotoEnhancerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Koin DI başlat
        startKoin {
            androidContext(this@PhotoEnhancerApplication)
            modules(appModule)
        }
    }
}
