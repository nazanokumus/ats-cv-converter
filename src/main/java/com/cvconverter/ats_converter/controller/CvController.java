// src/main/java/com/cvconverter/ats_converter/controller/CvController.java
package com.cvconverter.ats_converter.controller;

import com.cvconverter.ats_converter.service.CvProcessingService;
import com.cvconverter.ats_converter.service.PdfGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/cv")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3002"}) // Frontend'den gelen isteklere izin ver
public class CvController {

    private static final Logger logger = LoggerFactory.getLogger(CvController.class);

    private final CvProcessingService cvProcessingService;
    private final PdfGenerationService pdfGenerationService;

    public CvController(CvProcessingService cvProcessingService, PdfGenerationService pdfGenerationService) {
        this.cvProcessingService = cvProcessingService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @PostMapping(value = "/convert", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> convertCv(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            logger.warn("Boş bir dosya yükleme denemesi yapıldı.");
            return ResponseEntity.badRequest().body("Lütfen bir dosya seçin.");
        }

        try {
            logger.info("CV dönüştürme işlemi başlatıldı: {}", file.getOriginalFilename());

            // 1. ADIM: PDF dosyasından metni çıkar.
            String extractedText = cvProcessingService.extractTextFromPdf(file);
            logger.info("PDF'ten metin başarıyla çıkarıldı. Uzunluk: {} karakter.", extractedText.length());

            // 2. ADIM: Çıkarılan metni Gemini AI'ye göndererek yapılandırılmış JSON verisi al.
            //    Artık örnek boş JSON yerine GERÇEK metodu çağırıyoruz.
            String structuredData = cvProcessingService.getStructuredDataFromGemini(extractedText);
            logger.info("Gemini'den yapılandırılmış veri başarıyla alındı.");

            // 3. ADIM: Alınan yapılandırılmış veriden ATS uyumlu yeni bir PDF oluştur.
            byte[] pdfBytes = pdfGenerationService.createAtsFriendlyPdf(structuredData);
            logger.info("ATS uyumlu PDF başarıyla oluşturuldu. Boyut: {} bytes.", pdfBytes.length);

            // 4. ADIM: Başarılı bir şekilde oluşturulan PDF'i kullanıcıya gönder.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // 'inline' yerine 'attachment' kullanmak, dosyanın direkt indirilmesini tetikler.
            headers.setContentDispositionFormData("attachment", "ATS_Uyumlu_CV.pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            // Servis katmanlarından gelebilecek her türlü hatayı burada yakalıyoruz.
            logger.error("CV dönüştürme sırasında bir hata oluştu: ", e);

            // Kullanıcıya anlamlı ve güvenli bir hata mesajı döndürüyoruz.
            // e.getMessage() diyerek serviste fırlattığımız mesajı da kullanıcıya iletiyoruz.
            // Bu, "PDF şifreli" gibi özel durumları frontend'e bildirmemizi sağlar.
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}