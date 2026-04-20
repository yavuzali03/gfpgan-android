package com.gfpgan_android.util

import android.graphics.Bitmap

/**
 * JNI Wrapper for GFPGAN ONNX Model
 * Handles face restoration using ONNX Runtime
 */
class GFPGANNative {
    
    companion object {
        init {
            // Load the native library (should match CMakeLists.txt project name)
            System.loadLibrary("gfpgan_jni")
        }
    }
    
    /**
     * Initialize the ONNX model
     * @param modelPath Absolute path to the .onnx model file
     */
    external fun initModel(modelPath: String)
    
    /**
     * Enhance/restore a face image
     * @param inputBitmap Input blurry/low-quality image (should be 512x512)
     * @param outputBitmap Output bitmap (should be pre-allocated as 512x512)
     */
    external fun enhance(inputBitmap: Bitmap, outputBitmap: Bitmap)
}