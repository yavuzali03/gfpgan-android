#include "onnxruntime_cxx_api.h" // ONNX Header dosyası
#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>

#define LOG_TAG "GFPGAN_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global değişkenler (Session'ı her seferinde yeniden yaratmamak için)
Ort::Env *env = nullptr;
Ort::Session *session = nullptr;

extern "C" JNIEXPORT void JNICALL
Java_com_photoenhancer_util_GFPGANNative_initModel(JNIEnv *env_jni,
                                                   jobject /* this */,
                                                   jstring modelPathStr) {

  const char *modelPath = env_jni->GetStringUTFChars(modelPathStr, 0);

  try {
    // 1. ONNX Ortamını Başlat
    if (env == nullptr) {
      std::string instanceName = "GFPGAN_Session";
      env = new Ort::Env(ORT_LOGGING_LEVEL_WARNING, instanceName.c_str());
    }

    // 2. Session Ayarları
    Ort::SessionOptions sessionOptions;
    sessionOptions.SetIntraOpNumThreads(1);
    sessionOptions.SetGraphOptimizationLevel(
        GraphOptimizationLevel::ORT_ENABLE_EXTENDED);

    // 3. Modeli Yükle
    if (session == nullptr) {
      session = new Ort::Session(*env, modelPath, sessionOptions);
      LOGI("✓ Model başarıyla yüklendi: %s", modelPath);
    }
    env_jni->ReleaseStringUTFChars(modelPathStr, modelPath);

  } catch (const std::exception &e) {
    LOGE("❌ Model yüklenemedi: %s", e.what());
    env_jni->ReleaseStringUTFChars(modelPathStr, modelPath);

    // Java Exception fırlat
    jclass exceptionClass = env_jni->FindClass("java/lang/RuntimeException");
    if (exceptionClass != nullptr) {
      std::string errorMsg = "ONNX model yükleme hatası: ";
      errorMsg += e.what();
      env_jni->ThrowNew(exceptionClass, errorMsg.c_str());
    }
    return;
  }
}

extern "C" JNIEXPORT void JNICALL
Java_com_photoenhancer_util_GFPGANNative_enhance(
    JNIEnv *jenv, jobject /* this */,
    jobject inputBitmap,  // Bulanık resim
    jobject outputBitmap) // Boş ama boyutu ayarlanmış çıktı resmi
{
  if (session == nullptr) {
    LOGE("Session başlatılmamış! Önce initModel çağır.");
    return;
  }

  AndroidBitmapInfo info;
  void *pixels;
  void *outPixels;

  // --- 1. PRE-PROCESSING (GİRİŞ HAZIRLIĞI) ---
  AndroidBitmap_getInfo(jenv, inputBitmap, &info);
  AndroidBitmap_lockPixels(jenv, inputBitmap, &pixels);

  // Modelin beklediği boyut (512x512)
  int width = 512;
  int height = 512;
  int channel = 3;

  // Giriş Tensor Boyutu: [1, 3, 512, 512]
  std::vector<int64_t> inputShape = {1, 3, height, width};
  size_t inputTensorSize = 1 * 3 * height * width;
  std::vector<float> inputTensorValues(inputTensorSize);

  uint32_t *src = (uint32_t *)pixels;

  // HWC (Bitmap) -> NCHW (Tensor) Dönüşümü ve Normalizasyon
  // Formül: (val / 255.0 - 0.5) / 0.5
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      uint32_t pixel = src[y * width + x];

      // RGBA formatından RGB alıyoruz
      float r = ((pixel & 0x000000FF)) / 255.0f;
      float g = ((pixel & 0x0000FF00) >> 8) / 255.0f;
      float b = ((pixel & 0x00FF0000) >> 16) / 255.0f;

      // Normalizasyon
      r = (r - 0.5f) / 0.5f;
      g = (g - 0.5f) / 0.5f;
      b = (b - 0.5f) / 0.5f;

      // Planar yapıya diziyoruz (RRR...GGG...BBB...)
      inputTensorValues[0 * height * width + y * width + x] = r;
      inputTensorValues[1 * height * width + y * width + x] = g;
      inputTensorValues[2 * height * width + y * width + x] = b;
    }
  }
  AndroidBitmap_unlockPixels(jenv, inputBitmap);

  // --- 2. INFERENCE (ÇALIŞTIRMA) ---
  auto memoryInfo =
      Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);

  // Giriş ve Çıkış İsimleri (Colab'da 'input' ve 'output' olarak ayarlamıştık)
  const char *inputNames[] = {"input"};
  const char *outputNames[] = {"output"};

  Ort::Value inputTensor = Ort::Value::CreateTensor<float>(
      memoryInfo, inputTensorValues.data(), inputTensorSize, inputShape.data(),
      inputShape.size());

  // Run!
  auto outputTensors = session->Run(Ort::RunOptions{nullptr}, inputNames,
                                    &inputTensor, 1, outputNames, 1);

  // --- 3. POST-PROCESSING (ÇIKTIYI ALMA) ---
  float *floatArr = outputTensors.front().GetTensorMutableData<float>();

  AndroidBitmap_lockPixels(jenv, outputBitmap, &outPixels);
  uint32_t *dst = (uint32_t *)outPixels;

  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      // NCHW -> HWC Dönüşümü ve Denormalizasyon
      // Formül: (val * 0.5) + 0.5 -> 0..255
      float r = floatArr[0 * height * width + y * width + x];
      float g = floatArr[1 * height * width + y * width + x];
      float b = floatArr[2 * height * width + y * width + x];

      int ir = (int)((r * 0.5f + 0.5f) * 255.0f);
      int ig = (int)((g * 0.5f + 0.5f) * 255.0f);
      int ib = (int)((b * 0.5f + 0.5f) * 255.0f);

      // Clamp (Taşmaları önle)
      if (ir < 0)
        ir = 0;
      if (ir > 255)
        ir = 255;
      if (ig < 0)
        ig = 0;
      if (ig > 255)
        ig = 255;
      if (ib < 0)
        ib = 0;
      if (ib > 255)
        ib = 255;

      // Bitmap formatına geri yaz (ABGR veya ARGB, Android sürümüne göre
      // değişebilir ama genelde little-endian) Alpha kanalı tam görünür (0xFF)
      dst[y * width + x] = (0xFF << 24) | (ib << 16) | (ig << 8) | ir;
    }
  }
  AndroidBitmap_unlockPixels(jenv, outputBitmap);
}