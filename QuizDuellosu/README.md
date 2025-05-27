# Quiz Düellosu

Quiz Düellosu, çok oyunculu bir bilgi yarışması uygulamasıdır. Oyuncular farklı türlerdeki soruları yanıtlayarak puan kazanmaya çalışırlar. Uygulama, çeşitli soru formatlarını, jokerleri ve rekabetçi oyun mekaniklerini destekler.

## Özellikler

*   **Oyuncu Girişi**:
    *   Maksimum 6 oyuncu ile oynanabilir.
    *   Her oyuncu kendi ismini girer.
*   **Soru Teması Seçimi**:
    *   Oyuncular, yarışacakları 2 adet soru teması seçebilirler.
*   **Farklı Soru Tipleri**:
    *   **Hız Soruları**: İlk doğru cevabı veren oyuncu puan kazanır. Yanlış cevapta eksi puan uygulanır ve cevap hakkı (simüle edilmiş olarak) diğer oyunculara geçebilir.
    *   **Boşluk Doldurma**: Sorudaki boşluğu doğru kelimeyle dolduran ilk oyuncu puan kazanır.
    *   **Sıralama Soruları**: Verilen maddeleri doğru sıraya dizmek için 15 saniye süre tanınır.
    *   **Resimli Sorular**: Gösterilen resimle ilgili sorular sorulur.
    *   **Görünür Hale Gelen Resimli Etap**: Resim yavaşça (10 saniye içinde) görünür hale gelir. İlk doğru cevabı veren oyuncu puan kazanır, yanlış cevapta hak devri simüle edilir.
    *   **Müzik/Sesli Sorular**: 10 saniye boyunca bir ses/müzik parçası çalınır ve bununla ilgili soru sorulur. İlk doğru cevabı veren oyuncu puan kazanır, yanlış cevapta hak devri simüle edilir.
*   **Oyun Akışı**:
    *   **Soru Zamanlayıcısı**: Genel olarak her soru için 30 saniye süre verilir. Sıralama soruları için bu süre 15 saniyedir.
    *   **Periyodik Puan Durumu**: Her 6 soruda bir oyun duraklatılarak mevcut puan durumu gösterilir.
    *   **Oyun Sonu**: Tüm sorular bittiğinde final skor tablosu gösterilir.
    *   **Yeni Oyuna Başlama**: Oyun sonu ekranından yeni bir oyuna başlama seçeneği sunulur.
*   **Özel Güçler (Jokerler)**:
    *   **Çalma Jokeri**: Aktif oyuncu, bu jokeri kullanarak şıklı sorularda doğru cevabı görebilir. Her oyuncu oyun başına bir kez kullanabilir.
    *   **Şans Çarkı**: Oyun sırasında her 10 dakikada bir rastgele bir oyuncuya +5 puan bonus verilir.
*   **Donanım Simülasyonu**:
    *   **Ses Efektleri**: Doğru/yanlış cevaplar için ses efektleri `Toast` mesajları ile simüle edilir.
    *   **Işık Yönlendirmesi**: Cevaplama hakkı kazanan oyuncuya doğru tavan ışıklarının yönlendirilmesi `Log` mesajları ile simüle edilir.
*   **Veri Yönetimi**:
    *   Tüm yarışma soruları, projenin `assets` klasöründeki `questions.json` dosyasından yüklenir.

## Proje Yapısı

Proje, Android geliştirme için modern bir yaklaşım benimseyerek, sorumlulukların ayrıldığı ve yönetilebilir bir kod tabanı oluşturmayı hedefler. MVVM (Model-View-ViewModel) benzeri bir mimari desen takip edilmeye çalışılmıştır.

Ana uygulama kodu `app/src/main/java/com/example/quizduellosu/` paketi altında yer alır ve şu ana paketlere ayrılmıştır:

*   **`ui/activities`**: Kullanıcı arayüzünün Activity katmanını içerir.
    *   `PlayerInputActivity`: Oyuncu isimlerinin girildiği ekran.
    *   `TopicSelectionActivity`: Soru temalarının seçildiği ekran.
    *   `GameActivity`: Asıl yarışmanın gerçekleştiği, soruların gösterildiği ve cevapların alındığı ana oyun ekranı.
*   **`ui/viewmodels`**: Kullanıcı arayüzü ile iş mantığı arasında köprü görevi gören ViewModel'leri içerir.
    *   `GameViewModel`: Oyunun tüm mantığını, durumunu, skorları, zamanlayıcıları ve soru akışını yöneten merkezi ViewModel'dir.
*   **`data/models`**: Uygulamanın veri modellerini içerir.
    *   `Question.kt` (ve alt tipleri: `SpeedQuestion`, `FillInTheBlankQuestion`, `OrderingQuestion`, `ImageQuestion`, `AudioQuestion`, `RevealingImageQuestion`): Farklı soru yapılarını temsil eden data class'lardır.
    *   `PlayerState.kt`: Her bir oyuncunun adını, skorunu ve joker durumunu tutar.
*   **`data/repositories`**: Veri kaynaklarından veri alımını soyutlayan Repository katmanını içerir.
    *   `QuestionLoaderRepository.kt`: `assets/questions.json` dosyasından soruları okuyup, parse edip `Question` nesnelerine dönüştürür.

**`app/src/main/assets/`**:
*   Bu klasör, uygulama tarafından kullanılan statik varlıkları içerir.
*   `questions.json`: Yarışma sorularının JSON formatında tutulduğu dosyadır. Her bir soru nesnesi `id`, `questionType`, `text`, `topic` gibi ortak alanların yanı sıra soru tipine özel alanlar içerir (örn: `options`, `correctOptionIndex`, `correctAnswers`, `imageUrl`, `audioUrl` vb.).

## Kurulum ve Çalıştırma

**Gereksinimler**:
*   Android Studio (Arctic Fox 2020.3.1 veya daha güncel bir sürümü önerilir).
*   Android SDK (Proje `compileSdk 33` ve `minSdk 21` ile yapılandırılmıştır).

**Adımlar**:
1.  Projeyi Android Studio'da açın (`File -> Open -> Projenin ana dizinini seçin`).
2.  Gerekli Gradle senkronizasyonunun tamamlanmasını bekleyin.
3.  Bir Android emülatörü başlatın veya fiziksel bir Android cihaz bağlayın.
4.  Araç çubuğundan "Run 'app'" seçeneğine tıklayın veya `Shift + F10` klavye kısayolunu kullanın.

## Testler

Proje, iş mantığının ve kullanıcı arayüzü akışlarının doğruluğunu sağlamak için birim ve UI testlerini içerir.

*   **Birim Testleri (`app/src/test/`)**:
    *   `GameViewModelTest.kt`: `GameViewModel` içindeki oyun mantığı, puanlama, soru akışı, joker kullanımı ve zamanlayıcılar gibi kritik işlevleri test eder.
    *   **Çalıştırma**: Android Studio'da `GameViewModelTest.kt` dosyasına veya içindeki belirli bir test metoduna sağ tıklayıp "Run 'GameViewModelTest'" (veya benzeri) seçeneğini seçerek çalıştırabilirsiniz.

*   **UI Testleri (`app/src/androidTest/`)**:
    *   `PlayerInputToGameFlowTest.kt`: `PlayerInputActivity`'den başlayarak oyuncu ismi girme, konu seçme ve `GameActivity`'ye ulaşma gibi temel kullanıcı akışını Espresso kullanarak test eder.
    *   **Çalıştırma**: Android Studio'da `PlayerInputToGameFlowTest.kt` dosyasına veya içindeki belirli bir test metoduna sağ tıklayıp "Run 'PlayerInputToGameFlowTest'" (veya benzeri) seçeneğini seçerek çalıştırabilirsiniz. Bu testler bir emülatör veya fiziksel cihaz üzerinde çalışacaktır.
