// src/main/java/com/cvconverter/ats_converter/service/CvProcessingService.java
package com.cvconverter.ats_converter.service;

import com.cvconverter.ats_converter.dto.gemini.*;
import org.apache.pdfbox.Loader; // <-- EKLENDİ
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

    private static final String GEMINI_PROMPT =
            "Aşağıdaki CV metnini analiz et ve bilgileri şu JSON formatında yapılandır: " +
                    "{ \"kisisel_bilgiler\": { \"isim\": \"\", \"email\": \"\", \"telefon\": \"\" }, " +
                    "\"is_deneyimleri\": [ { \"unvan\": \"\", \"sirket\": \"\", \"tarihler\": \"\", \"aciklama\": \"\" } ], " +
                    "\"egitim_bilgileri\": [ { \"okul\": \"\", \"bolum\": \"\", \"derece\": \"\", \"tarihler\": \"\" } ], " +
                    "\"yetenekler\": [] }. " +
                    "Sadece ve sadece bu JSON objesini döndür, başka hiçbir metin veya açıklama ekleme. CV Metni: \n\n";

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public CvProcessingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Verilen PDF dosyasından metin içeriğini çıkarır.
     */
    public String extractTextFromPdf(MultipartFile file) {
        // try-with-resources, document'ın her durumda kapatılmasını sağlar.
        try (PDDocument document = Loader.loadPDF(file.getBytes())) { // <-- DEĞİŞTİ
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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String fullPrompt = GEMINI_PROMPT + cvText;
        Part part = new Part(fullPrompt);
        Content content = new Content(Collections.singletonList(part));
        GeminiRequest geminiRequest = new GeminiRequest(Collections.singletonList(content));

        HttpEntity<GeminiRequest> entity = new HttpEntity<>(geminiRequest, headers);

        try {
            logger.info("Gemini API'ye istek gönderiliyor...");
            ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(url, entity, GeminiResponse.class);

            if (response.getBody() == null || response.getBody().getCandidates() == null || response.getBody().getCandidates().isEmpty()) {
                throw new RuntimeException("Gemini API'den geçersiz veya boş bir cevap alındı.");
            }

            String resultText = response.getBody().getCandidates().get(0).getContent().getParts().get(0).getText();
            logger.info("Gemini API'den başarılı bir cevap alındı.");

            // Bazen Gemini cevabı ```json ... ``` bloğu içinde dönebilir, bu bloğu temizleyelim.
            if (resultText.startsWith("```json")) {
                resultText = resultText.substring(7, resultText.length() - 3).trim();
            } else if (resultText.startsWith("```")) {
                resultText = resultText.substring(3, resultText.length() - 3).trim();
            }

            return resultText;

        } catch (HttpClientErrorException e) {
            logger.error("Gemini API'ye erişirken bir istemci/sunucu hatası oluştu. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API ile iletişim kurulamadı. Hata: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Gemini API'ye istek gönderilirken beklenmedik bir hata oluştu.", e);
            throw new RuntimeException("Veri işlenirken beklenmedik bir hata oluştu: " + e.getMessage(), e);
        }
    }
}