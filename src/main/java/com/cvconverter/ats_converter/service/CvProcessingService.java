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

    // --- PROMPT'LARIMIZ AYNI, ONLAR STRATEJİMİZİN BİR PARÇASI ---

    private static final String CV_PARSING_PROMPT =
            "Aşağıdaki CV metnini analiz et ve bilgileri şu JSON formatında yapılandır: " +
                    "{ \"kisisel_bilgiler\": { \"isim\": \"\", \"email\": \"\", \"telefon\": \"\" }, " +
                    "\"is_deneyimleri\": [ { \"unvan\": \"\", \"sirket\": \"\", \"tarihler\": \"\", \"aciklama\": \"\" } ], " +
                    "\"egitim_bilgileri\": [ { \"okul\": \"\", \"bolum\": \"\", \"derece\": \"\", \"tarihler\": \"\" } ], " +
                    "\"yetenekler\": [] }. " +
                    "Sadece ve sadece bu JSON objesini döndür, başka hiçbir metin, markdown formatlaması (` ```json`) veya açıklama ekleme. CV Metni: \n\n";

    private static final String COVER_LETTER_GENERATION_PROMPT =
            "Sen, bir adayın iş başvurusunda ona yardımcı olan profesyonel bir kariyer danışmanısın. " +
                    "Aşağıdaki JSON formatındaki CV verisini ve iş ilanı metnini kullanarak, bu işe başvuran aday için profesyonel, samimi ve etkileyici bir ön yazı oluştur. " +
                    "Ön yazı, adayın CV'sindeki en güçlü yetenekleri ve tecrübeleri, iş ilanındaki anahtar gereksinimlerle zekice eşleştirmelidir. " +
                    "Giriş, gelişme ve sonuç paragraflarından oluşan, akıcı bir metin oluştur. Sadece ve sadece ön yazının metnini döndür, başka hiçbir başlık veya açıklama ekleme.\n\n" +
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