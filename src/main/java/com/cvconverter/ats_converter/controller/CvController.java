package com.cvconverter.ats_converter.controller;

import com.cvconverter.ats_converter.service.CvProcessingService;
import com.cvconverter.ats_converter.service.PdfGenerationService;
import com.cvconverter.ats_converter.service.ZipService; // <-- YENİ SERVİSİMİZ
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

    // Sabitlerimizi en tepeye taşıdık, daha temiz.
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final CvProcessingService cvProcessingService;
    private final PdfGenerationService pdfGenerationService;
    private final ZipService zipService; // <-- YENİ ZİP SERVİSİNİ ENJEKTE ETTİK

    // Constructor'ı yeni servisi de alacak şekilde güncelledik.
    public CvController(CvProcessingService cvProcessingService,
                        PdfGenerationService pdfGenerationService,
                        ZipService zipService) {
        this.cvProcessingService = cvProcessingService;
        this.pdfGenerationService = pdfGenerationService;
        this.zipService = zipService;
    }

    // Eski `/convert` endpoint'ini daha genel bir `/generate` ile değiştirdik.
    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateDocuments(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @RequestParam(value = "generateCoverLetter", defaultValue = "false") boolean generateCoverLetter) {

        // --- GİRİŞ DOĞRULAMA KISMI ---
        if (file.isEmpty()) {
            logger.warn("VALIDATION_FAIL: Boş bir dosya yükleme denemesi yapıldı.");
            return ResponseEntity.badRequest().body("Lütfen bir dosya seçin.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(ALLOWED_CONTENT_TYPE)) {
            logger.warn("VALIDATION_FAIL: Geçersiz dosya tipi. Gelen: '{}'", contentType);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Lütfen sadece PDF formatında dosya yükleyin.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            logger.warn("VALIDATION_FAIL: Dosya boyutu limiti aşıldı. Gelen: {}", file.getSize());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Dosya boyutu 5 MB'dan büyük olamaz.");
        }
        if (generateCoverLetter && (jobDescription == null || jobDescription.isBlank())) {
            logger.warn("VALIDATION_FAIL: Ön yazı istendi ama iş ilanı metni boş.");
            return ResponseEntity.badRequest().body("Ön yazı oluşturmak için iş ilanı metni zorunludur.");
        }

        String requestId = (String) org.slf4j.MDC.get("requestId");
        logger.info("[{}] Doküman oluşturma işlemi başlatıldı. Ön Yazı İsteği: {}", requestId, generateCoverLetter);

        try {
            // --- ÇEKİRDEK İŞLEMLER ---
            String extractedCvText = cvProcessingService.extractTextFromPdf(file);
            String structuredCvData = cvProcessingService.getStructuredDataFromGemini(extractedCvText);
            byte[] atsCvPdfBytes = pdfGenerationService.createAtsFriendlyPdf(structuredCvData);

            // --- KARAR MEKANİZMASI: Sadece CV mi, yoksa paket mi? ---
            if (generateCoverLetter) {
                logger.info("[{}] Ön yazı oluşturma işlemi başlatıldı.", requestId);
                String coverLetterText = cvProcessingService.generateCoverLetter(structuredCvData, jobDescription);

                // İki dosyayı bir araya getirecek bir harita (map) oluşturuyoruz.
                Map<String, byte[]> filesToZip = Map.of(
                        "ATS_Uyumlu_CV.pdf", atsCvPdfBytes,
                        "On_Yazi.txt", coverLetterText.getBytes() // Ön yazıyı basit bir txt dosyası olarak ekliyoruz.
                );

                byte[] zipBytes = zipService.createZipFile(filesToZip);
                logger.info("[{}] CV ve Ön Yazı başarıyla ZİP dosyasına eklendi.", requestId);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // ZİP için doğru içerik tipi
                headers.setContentDispositionFormData("attachment", "CV_ve_On_Yazi.zip");
                return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

            } else {
                logger.info("[{}] Sadece CV oluşturma işlemi tamamlandı.", requestId);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "ATS_Uyumlu_CV.pdf");
                return new ResponseEntity<>(atsCvPdfBytes, headers, HttpStatus.OK);
            }

        } catch (IOException | RuntimeException e) { // Birden fazla exception türünü yakalıyoruz
            String requestIdForError = (String) org.slf4j.MDC.get("requestId");
            logger.error("[{}] Doküman oluşturma sırasında bir hata oluştu: {}", requestIdForError, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}