package com.cvconverter.ats_converter.controller;

import com.cvconverter.ats_converter.service.AsyncDocumentService;
import com.cvconverter.ats_converter.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/cv")
public class CvController {
    private static final Logger logger = LoggerFactory.getLogger(CvController.class);

    private final AsyncDocumentService asyncDocumentService;
    private final FileStorageService fileStorageService;

    public CvController(AsyncDocumentService asyncDocumentService, FileStorageService fileStorageService) {
        this.asyncDocumentService = asyncDocumentService;
        this.fileStorageService = fileStorageService;
    }

    // Gerçek endpoint'i testlerden arındırıp eski, temiz haline geri getiriyoruz.
    @PostMapping(value = "/generate-stream", consumes = "multipart/form-data")
    public SseEmitter generateDocumentsStream(
            @RequestParam("file") MultipartFile file,
            @RequestParam("apiKey") String apiKey,
            @RequestParam(value = "jobDescription", required = false, defaultValue = "") String jobDescription,
            @RequestParam(value = "generateCoverLetter", defaultValue = "false") boolean generateCoverLetter) {

        SseEmitter emitter = new SseEmitter(180000L); // 3 dakika

        emitter.onCompletion(() -> logger.info("SseEmitter (main) tamamlandı."));
        emitter.onError(ex -> logger.error("SseEmitter (main) hatası!", ex));
        emitter.onTimeout(() -> logger.warn("SseEmitter (main) zaman aşımına uğradı."));

        // --- ÇÖZÜM BURADA ---
        // Düz metin yerine, frontend'in beklediği JSON formatında bir bağlantı mesajı gönder.
        try {
            String connectionMessage = "{\"stage\":\"CONNECTION_ESTABLISHED\", \"message\":\"Sunucuya başarıyla bağlanıldı...\", \"data\":null}";
            emitter.send(SseEmitter.event().name("connection_established").data(connectionMessage));
        } catch (IOException e) {
            logger.error("İlk SSE 'connection_established' mesajı gönderilemedi.", e);
        }
        // --- ÇÖZÜM SONU ---

        logger.info("SSE bağlantısı oluşturuldu. Asenkron servise devrediliyor.");
        asyncDocumentService.processAndGenerateDocumentsStream(emitter, file, apiKey, jobDescription, generateCoverLetter);

        return emitter;
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String fileId, @RequestParam String filename) {
        byte[] fileContent = fileStorageService.getFile(fileId);
        if (fileContent == null) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(fileContent);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fileContent.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // Test endpoint'ini artık kaldırabiliriz, görevini tamamladı.
}