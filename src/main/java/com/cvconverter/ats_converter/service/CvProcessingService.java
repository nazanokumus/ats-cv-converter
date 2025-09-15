package com.cvconverter.ats_converter.service;

import com.cvconverter.ats_converter.dto.gemini.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

@Service
public class CvProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(CvProcessingService.class);

    /**
     * Gemini'ye, CV metnini nasıl JSON formatına dönüştüreceğini anlatan komut.
     * Bu versiyon, boş veya tamamen null değerli JSON objeleri döndürmesini engellemek için
     * daha net ve katı kurallar içerir.
     */
    private static final String CV_PARSING_PROMPT =
            "Aşağıdaki CV metnini analiz et ve bilgileri şu JSON formatında yapılandır. " +
                    "**KURAL: Metinden çıkarabildiğin KADAR ÇOK bilgiyi doldurmaya çalış.** Eğer bir bilgi (örneğin telefon numarası) metinde yoksa, o alanı boş string `\"\"` olarak bırak. " +
                    "**KESİNLİKLE boş bir JSON objesi (`{}`) veya tüm alanları `null` olan bir JSON döndürme.** En azından `isim` gibi temel bir alanı doldurmaya çalışmalısın.\n\n" +
                    "İstenen JSON Formatı:\n" +
                    "{ \"kisisel_bilgiler\": { \"isim\": \"\", \"email\": \"\", \"telefon\": \"\" }, " +
                    "\"is_deneyimleri\": [ { \"unvan\": \"\", \"sirket\": \"\", \"tarihler\": \"\", \"aciklama\": \"\" } ], " +
                    "\"egitim_bilgileri\": [ { \"okul\": \"\", \"bolum\": \"\", \"derece\": \"\", \"tarihler\": \"\" } ], " +
                    "\"yetenekler\": [] }\n\n" +
                    "Sadece ve sadece bu JSON objesini döndür, başka hiçbir metin, markdown (` ```json`) veya açıklama ekleme. CV Metni: \n\n";

    /**
     * Ön yazı oluşturmak için kullanılacak komut.
     */
    private static final String COVER_LETTER_GENERATION_PROMPT =
            "Sen, iş başvurusunda bulunan adayın ta kendisisin. " +
                    "Aşağıdaki JSON formatındaki CV verilerini ve iş ilanı metnini kullanarak, ilana başvuran kişi olarak, **birinci tekil şahıs ağzından ('ben', 'sahibim', 'geliştirdim' gibi ifadelerle)** profesyonel ve akıcı bir ön yazı yaz. " +
                    "**Metnin en başına 'Sayın Yetkili,' gibi genel ve profesyonel bir selamlama ifadesi ekleyerek başla.** " +
                    "Ardından, CV'ndeki en güçlü yeteneklerini ve tecrübelerini, iş ilanındaki anahtar gereksinimlerle eşleştirerek bu pozisyon için neden uygun olduğunu vurgula. " +
                    "Metin, birbiriyle bağlantılı paragraflardan oluşsun. **Metne kesinlikle 'Giriş:', 'Gelişme:', 'Sonuç:' gibi başlıklar veya ara başlıklar KOYMA, metin tek bir bütün olarak doğal bir şekilde aksın.** " +
                    "Metnin en sonuna, 'Saygılarımla,' gibi bir kapanış ifadesi ekle ve bir alt satıra CV verisindeki adayın ismini ve soyismini yaz. " +
                    "Sadece ve sadece ön yazının tam metnini döndür, başka hiçbir başlık, açıklama veya not ekleme.\n\n" +
                    "CV Verisi: %s\n\n" +
                    "İş İlanı Metni: %s";

    private final RestTemplate restTemplate;

    public CvProcessingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Yüklenen PDF dosyasından metin içeriğini çıkarır.
     */
    public String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new IOException("Şifreli PDF dosyaları desteklenmemektedir.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException("PDF dosyası okunurken bir hata oluştu: " + e.getMessage(), e);
        }
    }

    /**
     * Çıkarılan CV metnini yapılandırılmış JSON formatına dönüştürmek için Gemini API'sini çağırır.
     */
    public String getStructuredDataFromGemini(String cvText, String apiKey) {
        logger.info("CV verisini yapılandırmak için Gemini API'ye istek gönderiliyor...");
        String prompt = CV_PARSING_PROMPT + cvText;
        return callGeminiApi(prompt, apiKey);
    }

    /**
     * Yapılandırılmış CV verisi ve iş tanımını kullanarak bir ön yazı oluşturmak için Gemini API'sini çağırır.
     */
    public String generateCoverLetter(String cvDataJson, String jobDescription, String apiKey) {
        logger.info("Ön yazı oluşturmak için Gemini API'ye istek gönderiliyor...");
        String prompt = String.format(COVER_LETTER_GENERATION_PROMPT, cvDataJson, jobDescription);
        return callGeminiApi(prompt, apiKey);
    }

    /**
     * Gemini API'sine asıl isteği gönderen ve cevabı işleyen özel metot.
     * Hata yönetimini de bu metot üstlenir.
     */
    private String callGeminiApi(String prompt, String apiKey) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=" + apiKey;
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

            // Cevap içindeki olası markdown formatlamasını temizle
            if (resultText.startsWith("```json")) {
                resultText = resultText.substring(7, resultText.length() - 3).trim();
            } else if (resultText.startsWith("```")) {
                resultText = resultText.substring(3, resultText.length() - 3).trim();
            }
            return resultText;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Gemini API hatası. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            // API'den gelen hatayı, gövdesiyle birlikte yeniden fırlatarak daha anlamlı bir hata mesajı oluştur.
            String errorMessage = "Yapay zeka servisi bir hata döndürdü: " + e.getResponseBodyAsString();
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            logger.error("Gemini API çağrısı sırasında beklenmedik bir hata oluştu.", e);
            throw new RuntimeException("Veri işlenirken beklenmedik bir hata oluştu.", e);
        }
    }
}