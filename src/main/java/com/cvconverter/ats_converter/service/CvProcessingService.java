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

    // --- PROMPT'LARI, SAVAŞ EMİRLERİ GİBİ EN TEPEYE TAŞIDIK ---

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
                    "CV Verisi: %s\n\n" + // %s, String.format ile doldurulacak yer tutucudur.
                    "İş İlanı Metni: %s";


    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public CvProcessingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Verilen PDF dosyasından metin içeriğini çıkarır. (Bu metot aynı kaldı)
     */
    public String extractTextFromPdf(MultipartFile file) {
        // ... (Bu metodun içi, öncekiyle birebir aynı. Hiçbir değişiklik yok) ...
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
     * CV metnini Gemini AI'ye gönderir ve yapılandırılmış JSON verisini alır.
     */
    public String getStructuredDataFromGemini(String cvText) {
        logger.info("CV verisini yapılandırmak için Gemini API'ye istek gönderiliyor...");
        String prompt = CV_PARSING_PROMPT + cvText;
        return callGeminiApi(prompt);
    }

    /**
     * YENİ METOT: CV verisi ve iş ilanından ön yazı üretir.
     */
    public String generateCoverLetter(String cvDataJson, String jobDescription) {
        logger.info("Ön yazı oluşturmak için Gemini API'ye istek gönderiliyor...");
        // String.format ile, o yer tutucuları (%s) gerçek verilerle dolduruyoruz.
        String prompt = String.format(COVER_LETTER_GENERATION_PROMPT, cvDataJson, jobDescription);
        return callGeminiApi(prompt);
    }

    /**
     * YENİ YARDIMCI METOT: Gemini API çağrısını merkezileştirdik.
     * Bu, kod tekrarını önler ve bütün Gemini çağrılarını tek bir yerden yönetmemizi sağlar.
     */
    private String callGeminiApi(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + geminiApiKey;

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

            // Cevabı temizleme mantığı aynı
            if (resultText.startsWith("```json")) {
                resultText = resultText.substring(7, resultText.length() - 3).trim();
            } else if (resultText.startsWith("```")) {
                resultText = resultText.substring(3, resultText.length() - 3).trim();
            }

            return resultText;

        } catch (HttpClientErrorException e) {
            logger.error("Gemini API hatası. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Yapay zeka servisi ile iletişim kurulamadı. Lütfen API anahtarınızı ve model adını kontrol edin.");
        } catch (Exception e) {
            logger.error("Gemini API çağrısı sırasında beklenmedik hata.", e);
            throw new RuntimeException("Veri işlenirken beklenmedik bir hata oluştu.");
        }
    }
}