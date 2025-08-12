# ATS Uyumlu CV Dönüştürücü (ATS-Friendly CV Converter)

Bu proje, kullanıcıların standart PDF formatındaki CV'lerini yükleyerek, Aday Takip Sistemleri (Applicant Tracking Systems - ATS) tarafından kolayca okunabilen ve analiz edilebilen, temiz ve yapılandırılmış bir CV formatına dönüştürmelerini sağlayan bir web uygulamasıdır. Dönüştürülen CV, yine PDF formatında indirilebilir.

Uygulama, yüklenen PDF'in içeriğini analiz etmek ve yapılandırmak için Google'ın güçlü yapay zeka modeli **Gemini AI**'dan faydalanmaktadır.

---

## 🚀 Temel Özellikler

- **PDF Yükleme:** Kullanıcılar, mevcut CV'lerini `.pdf` formatında kolayca yükleyebilirler.
- **Yapay Zeka Destekli Analiz:** Yüklenen CV'nin metin içeriği, **Google Gemini AI** kullanılarak analiz edilir ve kişisel bilgiler, iş deneyimi, eğitim ve yetenekler gibi temel bölümlere ayrılır.
- **ATS Uyumlu Çıktı:** Gemini'den gelen yapılandırılmış veri, ATS sistemlerinin rahatça okuyabileceği, basit ve temiz bir tasarıma sahip yeni bir PDF dosyasına dönüştürülür.
- **Anında İndirme:** Oluşturulan ATS uyumlu yeni CV, anında kullanıcı tarafından indirilebilir.

---

## 🛠️ Kullanılan Teknolojiler

Bu projenin geliştirilmesinde aşağıdaki teknolojiler ve kütüphaneler kullanılmıştır:

### Backend (Sunucu Tarafı)
- **Java 17**
- **Spring Boot 3:** Hızlı ve sağlam REST API geliştirme için.
- **Apache Maven:** Proje yönetimi ve bağımlılıklar için.
- **Google Gemini AI:** CV metnini anlamak ve yapılandırmak için.
- **Apache PDFBox:** Yüklenen PDF dosyalarından metin çıkarmak için.
- **OpenPDF (LibrePDF):** Yapılandırılmış veriden yeni PDF dosyası oluşturmak için.
- **Lombok:** Kod tekrarını azaltmak için.

### Frontend (Kullanıcı Arayüzü)
- **TypeScript:** Tip güvenliği sağlayan JavaScript üst kümesi.
- **React:** Modern ve reaktif kullanıcı arayüzleri oluşturmak için.
- **Axios:** Backend API'si ile iletişim kurmak için.
- **CSS3:** Temiz ve modern bir tasarım için.

---

## 🏁 Projeyi Lokal (Yerel) Bilgisayarda Çalıştırma

Bu projeyi kendi bilgisayarınızda çalıştırmak için aşağıdaki adımları izleyebilirsiniz.

### Gereksinimler

- **JDK 17** (veya daha yeni bir sürüm)
- **Apache Maven**
- **Node.js ve npm**
- **Git**
- **Google Gemini API Anahtarı:** [Google AI Studio](https://aistudio.google.com/)'dan ücretsiz olarak alabilirsiniz.

### Kurulum ve Çalıştırma

1.  **Projeyi Klonlayın:**
    ```bash
    git clone https://github.com/nazanokumus/ats-cv-converter.git
    cd ats-cv-converter
    ```

2.  **Backend'i Ayarlayın ve Çalıştırın:**
    - Projeyi IntelliJ IDEA gibi bir IDE'de açın.
    - **Ortam Değişkenini Ayarlayın:** Uygulamanın çalışabilmesi için Gemini API anahtarınızı bir ortam değişkeni olarak tanımlamanız gerekmektedir.
        - IntelliJ IDEA için: `Run` -> `Edit Configurations...` menüsüne gidin.
        - `AtsConverterApplication`'ı seçin.
        - "Environment variables" alanına `GEMINI_API_KEY=[SENİN-GERÇEK-API-ANAHTARIN]` şeklinde ekleyin.
    - Projenin bağımlılıklarının Maven tarafından otomatik olarak indirilmesini bekleyin.
    - `AtsConverterApplication.java` dosyasını bulun ve çalıştırın. Backend sunucusu `http://localhost:8080` adresinde başlayacaktır.

3.  **Frontend'i Ayarlayın ve Çalıştırın:**
    - Yeni bir terminal penceresi açın ve projenin frontend klasörüne gidin:
      ```bash
      cd ats-cv-ui
      ```
    - Gerekli Node.js paketlerini yükleyin:
      ```bash
      npm install
      ```
    - Geliştirme sunucusunu başlatın:
      ```bash
      npm start
      ```
    - Tarayıcınızda otomatik olarak `http://localhost:3000` adresi açılacaktır. Artık uygulamayı kullanabilirsiniz!

---

## 🔮 Gelecek Planları ve İyileştirmeler

- [ ] Kullanıcı kayıt ve giriş sistemi eklemek.
- [ ] Kullanıcıların oluşturduğu CV'leri veritabanında saklamak.
- [ ] Farklı CV şablonu seçenekleri sunmak.
- [ ] Projeyi Docker ile konteyner haline getirmek ve bulut platformlarında yayınlamak.
- [ ] Kod kalitesini artırmak için birim (unit) ve entegrasyon (integration) testleri yazmak.
