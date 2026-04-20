cmake_minimum_required(VERSION 3.18.0)

project("gfpgan_jni")

# 1. ONNX Runtime Kütüphanesini Tanıt
# libonnxruntime.so dosyasının jniLibs içinde olduğunu varsayıyoruz
add_library(onnxruntime SHARED IMPORTED)
set_target_properties(onnxruntime PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libonnxruntime.so)

# 2. Senin JNI Kodun
add_library(gfpgan_jni SHARED
        gfpgan_jni.cpp)

# 3. Android'in kendi kütüphanelerini bul (Log ve Bitmap işlemleri için)
find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics) # Bitmap kilitlemek için şart

# 4. Header Dosyalarının Yeri
# include klasörünü cpp klasörünün içine attığını varsayıyoruz
include_directories(${CMAKE_SOURCE_DIR}/include)

# 5. Bağlama (Linking)
target_link_libraries(gfpgan_jni
        onnxruntime      # ONNX Runtime
        ${log-lib}       # Loglama
        ${android-lib}   # Android native
        ${jnigraphics-lib} # Bitmap işlemleri
)