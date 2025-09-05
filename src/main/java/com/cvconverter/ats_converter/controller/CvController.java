package com.cvconverter.ats_converter.controller;

import com.cvconverter.ats_converter.service.CvProcessingService;
import com.cvconverter.ats_converter.service.PdfGenerationService;
import com.cvconverter.ats_converter.service.ZipService; // ZİP SERVİSİMİZ GERİ GELDİ!
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cv")
public class CvController {

    private static final Logger logger = LoggerFactory.getLogger(CvController.class);

    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    // TÜM SERVİSLERİMİZ YERLİ YERİNDE
    private final CvProcessingService cvProcessingService;
    private final PdfGenerationService pdfGenerationService;
    private final ZipService zipService;

    public CvController(CvProcessingService cvProcessingService,
                        PdfGenerationService pdfGenerationService,
                        ZipService zipService) {
        this.cvProcessingService = cvProcessingService;
        this.pdfGenerationService = pdfGenerationService;
        this.zipService = zipService;
    }

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateDocuments(
            @RequestParam("file") MultipartFile file,
            @RequestParam("apiKey") String apiKey, // EN KRİTİK YENİ PARAMETRE
            @RequestParam(value = "jobDescription", required = false, defaultValue = "") String jobDescription,
            @RequestParam(value = "generateCoverLetter", defaultValue = "false") boolean generateCoverLetter) {

        // --- GİRİŞ DOĞRULAMA KISMI (API KEY KONTROLÜ İLE GÜÇLENDİRİLDİ) ---
        if (file.isEmpty()) { /* ... aynı ... */ return ResponseEntity.badRequest().body("Lütfen bir dosya seçin."); }
        if (apiKey == null || apiKey.isBlank()) { /* ... YENİ ... */ return ResponseEntity.badRequest().body("API anahtarı zorunludur."); }
        // ... Diğer dosya kontrolleri aynı ...
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(ALLOWED_CONTENT_TYPE)) { /* ... aynı ... */ return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Lütfen sadece PDF formatında dosya yükleyin."); }
        if (file.getSize() > MAX_FILE_SIZE) { /* ... aynı ... */ return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Dosya boyutu 5 MB'dan büyük olamaz."); }
        if (generateCoverLetter && jobDescription.isBlank()) { /* ... aynı ... */ return ResponseEntity.badRequest().body("Ön yazı oluşturmak için iş ilanı metni zorunludur."); }

        logger.info("Doküman oluşturma işlemi başlatıldı. Ön Yazı İsteği: {}", generateCoverLetter);

        try {
            // --- ÇEKİRDEK İŞLEMLER (ARTIK API KEY İLE ÇALIŞIYOR) ---
            String extractedCvText = cvProcessingService.extractTextFromPdf(file);
            // Gemini'ye isteği artık kullanıcının API anahtarıyla atıyoruz.
            String structuredCvData = cvProcessingService.getStructuredDataFromGemini(extractedCvText, apiKey);
            byte[] atsCvPdfBytes = pdfGenerationService.createAtsFriendlyPdf(structuredCvData);

            // --- KARAR MEKANİZMASI (OLDUĞU GİBİ KORUNDU) ---
            if (generateCoverLetter) {
                logger.info("Ön yazı oluşturma işlemi başlatıldı.");
                // Ön yazı oluşturma metoduna da API anahtarını yolluyoruz.
                String coverLetterText = cvProcessingService.generateCoverLetter(structuredCvData, jobDescription, apiKey);

                Map<String, byte[]> filesToZip = Map.of(
                        "ATS_Uyumlu_CV.pdf", atsCvPdfBytes,
                        "On_Yazi.txt", coverLetterText.getBytes()
                );

                byte[] zipBytes = zipService.createZipFile(filesToZip);
                logger.info("CV ve Ön Yazı başarıyla ZİP dosyasına eklendi.");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", "CV_ve_On_Yazi.zip");
                return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

            } else {
                logger.info("Sadece CV oluşturma işlemi tamamlandı.");
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "ATS_Uyumlu_CV.pdf");
                return new ResponseEntity<>(atsCvPdfBytes, headers, HttpStatus.OK);
            }

        } catch (IOException | RuntimeException e) {
            logger.error("Doküman oluşturma sırasında bir hata oluştu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("CV işlenirken bir sunucu hatası oluştu: " + e.getMessage());
        }
    }
}