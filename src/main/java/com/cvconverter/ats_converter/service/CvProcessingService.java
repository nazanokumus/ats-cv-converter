package com.cvconverter.ats_converter.service;

import com.cvconverter.ats_converter.dto.gemini.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

@Service
public class CvProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(CvProcessingService.class);

    private static final String CV_PARSING_PROMPT =
            "Sen, yapılandırılmamış CV metnini analiz edip bunu belirtilen JSON formatına dönüştüren bir veri çıkarma uzmanısın. " +
                    "Sana yol göstermesi için aşağıda bir örnek bulunmaktadır. Bu örneği dikkatlice incele ve ardından sana verilen asıl CV metnini aynı mantıkla işle.\n\n" +

                    "--- ÖRNEK BAŞLANGICI ---\n" +
                    "ÖRNEK CV METNİ:\n" +
                    "Nazan Okumuş\n" +
                    "nazan.okumus@email.com | 555-123-4567\n\n" +
                    "İŞ DENEYİMİ\n\n" +
                    "Kıdemli Java Geliştirici\n" +
                    "Teknoloji A.Ş., İstanbul\n" +
                    "Ocak 2020 – Halen\n" +
                    "- Spring Boot ve mikroservis mimarisi kullanarak yüksek performanslı API'ler geliştirdim.\n" +
                    "- Kafka ile anlık veri işleme projelerinde görev aldım.\n\n" +
                    "Yazılım Geliştirici\n" +
                    "Çözüm Ltd., Ankara\n" +
                    "Haziran 2018 – Aralık 2019\n" +
                    "- Mevcut monolitik uygulamanın bakımını yaptım ve yeni özellikler ekledim.\n" +
                    "\n" +
                    "EĞİTİM\n\n" +
                    "Bilgisayar Mühendisliği (Lisans)\n" +
                    "Orta Doğu Teknik Üniversitesi\n" +
                    "2014 - 2018\n\n" +
                    "YETENEKLER\n\n" +
                    "Java, Spring Boot, Python, Docker, SQL, Git\n" +
                    "--- ÖRNEK SONU ---\n\n" +

                    "Yukarıdaki metin için BEKLENEN JSON ÇIKTISI:\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"kisisel_bilgiler\": {\n" +
                    "    \"isim\": \"Nazan Okumuş\",\n" +
                    "    \"email\": \"nazan.okumus@email.com\",\n" +
                    "    \"telefon\": \"555-123-4567\"\n" +
                    "  },\n" +
                    "  \"is_deneyimleri\": [\n" +
                    "    {\n" +
                    "      \"unvan\": \"Kıdemli Java Geliştirici\",\n" +
                    "      \"sirket\": \"Teknoloji A.Ş.\",\n" +
                    "      \"tarihler\": \"Ocak 2020 – Halen\",\n" +
                    "      \"aciklama\": \"- Spring Boot ve mikroservis mimarisi kullanarak yüksek performanslı API'ler geliştirdim.\\n- Kafka ile anlık veri işleme projelerinde görev aldım.\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"unvan\": \"Yazılım Geliştirici\",\n" +
                    "      \"sirket\": \"Çözüm Ltd.\",\n" +
                    "      \"tarihler\": \"Haziran 2018 – Aralık 2019\",\n" +
                    "      \"aciklama\": \"- Mevcut monolitik uygulamanın bakımını yaptım ve yeni özellikler ekledim.\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"egitim_bilgileri\": [\n" +
                    "    {\n" +
                    "      \"okul\": \"Orta Doğu Teknik Üniversitesi\",\n" +
                    "      \"bolum\": \"Bilgisayar Mühendisliği\",\n" +
                    "      \"derece\": \"Lisans\",\n" +
                    "      \"tarihler\": \"2014 - 2018\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"yetenekler\": [\n" +
                    "    \"Java\",\n" +
                    "    \"Spring Boot\",\n" +
                    "    \"Python\",\n" +
                    "    \"Docker\",\n" +
                    "    \"SQL\",\n" +
                    "    \"Git\"\n" +
                    "  ]\n" +
                    "}\n" +
                    "```\n\n" +

                    "**ÖNEMLİ KURAL:** Bilgiyi CV metninde bulamazsan, JSON'daki ilgili alanı boş string `\"\"` olarak bırak. Asla `null` yazma.\n" +
                    "**SON İSTEK:** Şimdi, bu mantığı ve kuralları kullanarak aşağıdaki asıl CV Metnini işle. Sadece ve sadece JSON objesini döndür, başka hiçbir metin, markdown formatlaması veya açıklama ekleme.\n\n" +
                    "ASIL CV METNİ:\n\n";

    private static final String COVER_LETTER_GENERATION_PROMPT =
            "Sen, iş başvurusunda bulunan adayın ta kendisisin. " +
                    "Aşağıdaki JSON formatındaki CV verilerini ve iş ilanı metnini kullanarak, ilana başvuran kişi olarak, **birinci tekil şahıs ağzından ('ben', 'sahibim', 'geliştirdim' gibi ifadelerle)** profesyonel ve akıcı bir ön yazı yaz. " +
                    "Ön yazıda, CV'ndeki en güçlü yeteneklerini ve tecrübelerini, iş ilanındaki anahtar gereksinimlerle eşleştirerek bu pozisyon için neden uygun olduğunu vurgula. " +
                    "Metin, birbiriyle bağlantılı paragraflardan oluşsun. **Metne kesinlikle 'Giriş:', 'Gelişme:', 'Sonuç:' gibi başlıklar veya ara başlıklar KOYMA, metin tek bir bütün olarak doğal bir şekilde aksın.** " +
                    "Metnin en sonuna, 'Saygılarımla,' gibi bir kapanış ifadesi ekle ve bir alt satıra CV verisindeki adayın ismini ve soyismini yaz. " +
                    "Sadece ve sadece ön yazının tam metnini döndür, başka hiçbir başlık, açıklama veya not ekleme.\n\n" +
                    "CV Verisi: %s\n\n" +
                    "İş İlanı Metni: %s";

    // --- DEĞİŞİKLİK: ARTIK @VALUE("${gemini.api.key}") YOK! ---
    // Anahtarı artık dışarıdan alacağımız için bu değişkene ihtiyacımız kalmadı.
    // Bu, kodumuzu daha esnek ve stateless (durumsuz) hale getirir.
    private final RestTemplate restTemplate;

    public CvProcessingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * PDF'ten metin çıkarma işi aynı kaldı. Buna dokunmuyoruz.
     */
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                logger.warn("Şifreli bir PDF dosyası yüklendi: {}", file.getOriginalFilename());
                throw new IOException("Şifreli PDF dosyaları desteklenmemektedir.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            logger.error("PDF dosyasından metin çıkarılırken bir hata oluştu.", e);
            throw new RuntimeException("PDF dosyası okunurken bir hata oluştu: " + e.getMessage(), e);
        }
    }

    /**
     * DEĞİŞİKLİK: Bu metot artık KULLANICININ API ANAHTARINI da alıyor.
     */
    public String getStructuredDataFromGemini(String cvText, String apiKey) {
        logger.info("CV verisini yapılandırmak için Gemini API'ye istek gönderiliyor...");
        String prompt = CV_PARSING_PROMPT + cvText;
        return callGeminiApi(prompt, apiKey); // Yardımcı metoda apiKey'i de paslıyoruz.
    }

    /**
     * DEĞİŞİKLİK: Bu metot da artık KULLANICININ API ANAHTARINI alıyor.
     */
    public String generateCoverLetter(String cvDataJson, String jobDescription, String apiKey) {
        logger.info("Ön yazı oluşturmak için Gemini API'ye istek gönderiliyor...");
        String prompt = String.format(COVER_LETTER_GENERATION_PROMPT, cvDataJson, jobDescription);
        return callGeminiApi(prompt, apiKey); // Yardımcı metoda apiKey'i de paslıyoruz.
    }

    /**
     * YARDIMCI METOT GÜNCELLENDİ: Artık hangi API anahtarını kullanacağını parametre olarak alıyor.
     */
    private String callGeminiApi(String prompt, String apiKey) {
        // URL'i artık dışarıdan gelen 'apiKey' ile dinamik olarak oluşturuyoruz.
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Part part = new Part(prompt);
        Content content = new Content(Collections.singletonList(part));
        GeminiRequest geminiRequest = new GeminiRequest(Collections.singletonList(content));

        HttpEntity<GeminiRequest> entity = new HttpEntity<>(geminiRequest, headers);

        try {
            ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url, entity, GeminiResponse.class);

            if (response.getBody() == null || response.getBody().getCandidates() == null || response.getBody().getCandidates().isEmpty()) {
                throw new RuntimeException("Gemini API'den geçersiz veya boş bir cevap alındı.");
            }

            logger.info("Gemini API'den başarılı bir cevap alındı.");
            String resultText = response.getBody().getCandidates().get(0).getContent().getParts().get(0).getText();

            // Cevap temizleme mantığı aynı
            if (resultText.startsWith("```json")) {
                resultText = resultText.substring(7, resultText.length() - 3).trim();
            } else if (resultText.startsWith("```")) {
                resultText = resultText.substring(3, resultText.length() - 3).trim();
            }

            return resultText;

        } catch (HttpClientErrorException e) {
            logger.error("Gemini API hatası. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Hata mesajını daha spesifik hale getirdik.
            throw new RuntimeException("Yapay zeka servisi ile iletişim kurulamadı. Lütfen API anahtarınızın geçerli olduğundan emin olun.");
        } catch (Exception e) {
            logger.error("Gemini API çağrısı sırasında beklenmedik hata.", e);
            throw new RuntimeException("Veri işlenirken beklenmedik bir hata oluştu.");
        }
    }
}