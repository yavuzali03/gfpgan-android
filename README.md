# PhotoEnhancer-Android: Profesyonel Yapay Zeka Destekli Fotoğraf İyileştirme Platformu

Bu proje, mobil cihazlarda yüksek performanslı yapay zeka (AI) ve makine öğrenimi (ML) modellerini kullanarak düşük çözünürlüklü veya bozuk fotoğrafları iyileştirmek için geliştirilmiş, **modern Android** mimarisiyle kurgulanmış bir prototiptir. Teknik bir mülakat portfolyosu olarak sunulmak üzere, endüstri standardı mimariler ve optimize edilmiş sistemler üzerine inşa edilmiştir.

---

## 🏗️ Mimari Yapı (Architecture)

Proje, sürdürülebilirlik, test edilebilirlik ve ölçeklenebilirlik prensipleri doğrultusunda **Clean Architecture** prensiplerini temel alır.

### 1. Katmanlı Mimari (Layered Architecture)
- **Presentation Layer (Sunum Katmanı):** UI bileşenleri (**Jetpack Compose**) ve durum yönetimi (**ViewModel + StateFlow**) bu katmanda yer alır. **MVVM (Model-View-ViewModel)** deseni kullanılarak UI mantığı ile iş mantığı birbirinden ayrılmıştır.
- **Domain Layer (İş Mantığı Katmanı):** Uygulamanın temel iş kurallarını (UseCases) kapsar. Data ve Presentation katmanları arasında köprü görevi görür ve platform bağımsızlık prensibine sadık kalır.
- **Data Layer (Veri Katmanı):** Veri kaynaklarını (ML Modelleri, Local/Remote Storage) yönetir. **Repository Pattern** kullanılarak veri erişimi soyutlanmıştır.

### 2. Modern Yaklaşımlar
- **Unidirectional Data Flow (UDF):** Compose ile entegre bir şekilde, durumların (State) tek bir kaynaktan aktığı, olayların (Events) ise yukarıya taşındığı bir yapı kurgulanmıştır.
- **Dependency Injection (DI):** Projenin tamamında **Koin** kullanılarak bileşenler arası bağımlılıklar gevşek bağlı (loose coupling) hale getirilmiştir.

---

## 🚀 Teknik Detaylar ve AI/ML Sistemleri

Uygulamanın kalbi olan Yapay Zeka hattı, birden fazla motorun orkestrasyonu ile çalışır:

### 1. Yapay Zeka Modelleri ve Çalışma Motorları (Inference Engines)
- **ONNX Runtime (Android):** GFPGAN ve Real-ESRGAN modellerinin GPU/NPU hızlandırmalı çalıştırılması için tercih edilmiştir. `v1.19.2` sürümü ile en yeni ONNX operator desteği sağlanmıştır.
- **GFPGAN & CodeFormer:** Yüz restorasyonu için kullanılan üretken (generative) modeller. Native (JNI/C++) entegrasyonu ile bellek yönetimi optimize edilmiştir.
- **Real-ESRGAN (x2 Plus):** Süper çözünürlük (super-resolution) işlemleri için tiled-inference tekniği uygulanmıştır.
- **MediaPipe & ML Kit:** Yüz segmentasyonu ve tespit işlemleri için düşük gecikmeli (low-latency) çözümler entegre edilmiştir.
- **OpenCV:** Görüntü işleme, harmanlama (blending) ve son işlem (post-processing) aşamalarında performanslı bitmap operasyonları için kullanılmıştır.

### 2. Performans Optimizasyonları
- **Tiled Processing:** Yüksek çözünürlüklü fotoğrafları işlerken `Out Of Memory (OOM)` hatalarını önlemek için görüntü küçük parçalara (tiles) bölünerek işlenir ve dikişsiz (overlap) şekilde birleştirilir.
- **Memory Management:** Büyük bitmaplerin bellek yönetimi için manuel `recycle()` ve `System.gc()` stratejileri uygulanmıştır.
- **Concurrency (Eşzamanlılık):** Ağır ML yükleri, Kotlin **Coroutines** kullanılarak `Dispatchers.Default` (CPU-bound) ve `Dispatchers.IO` kanalları üzerinden asenkron şekilde yönetilir; bu sayede UI thread asla bloke olmaz.
- **Native JNI Layers:** Kritik performans gerektiren model yükleme ve ön işleme adımları C++ katmanında (CMake) koşturulur.

---

## 🛠️ Teknoloji Yığını (Tech Stack)

| Kategori | Teknolojiler |
| :--- | :--- |
| **Dil** | Kotlin (100%), C++ (JNI) |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Hızlandırma** | ONNX Runtime, MediaPipe Vision, ML Kit Face Detection |
| **DI / Lifecycle** | Koin for Compose, AndroidX Lifecycle |
| **Image Loading** | Coil |
| **Ağ / Data** | Kotlin Coroutines, StateFlow |
| **Görüntü İşleme** | OpenCV SDK |

---

## 🧩 Uygulama Akışı (Pipeline Workflow)

1.  **Input Validation:** Orijinal görselin çözünürlüğü ve renk uzayı analizi.
2.  **Face Segmentation:** MediaPipe ile görseldeki yüzlerin tespiti ve izole edilmesi.
3.  **AI Restoration (GFPGAN/CodeFormer):** Tespit edilen yüzlerin 512x512 düzleminde iyileştirilmesi.
4.  **Upscaling (Real-ESRGAN):** Tüm görselin 2x oranında, tiling algoritmasıyla çözünürlüğünün artırılması.
5.  **Blending:** İyileştirilmiş görselin orijinal dokuyla (weight management) profesyonel şekilde harmanlanması.
6.  **Final Output:** Optimize edilmiş bitmap'in kullanıcıya sunulması ve kaydedilmesi.

---

## 📌 Neden Bu Yaklaşımlar Seçildi?

Bu projenin mimarisi, gerçek dünya kullanım senaryolarında karşılaşılan **bellek kısıtları** ve **cihaz çeşitliliği** problemleri düşünülerek tasarlanmıştır. Clean Architecture kullanımı, gelecekte yeni bir ML modelinin (örneğin PyTorch) sisteme sadece yeni bir Repository implementasyonu ile dahil edilmesine olanak tanır. Modern Android kütüphaneleri ile stabilite, asenkron yapılarla ise pürüzsüz bir kullanıcı deneyimi hedeflenmiştir.

---

👨‍💻 **Geliştirici:** [Yavuz Ali]  
📅 **Son Güncelleme:** Mart 2026
