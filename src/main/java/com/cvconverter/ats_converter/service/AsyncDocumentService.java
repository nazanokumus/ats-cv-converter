package com.cvconverter.ats_converter.service;

import com.cvconverter.ats_converter.dto.ProgressUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Service
public class AsyncDocumentService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncDocumentService.class);

    private final CvProcessingService cvProcessingService;
    private final PdfGenerationService pdfGenerationService;
    private final ZipService zipService;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;


    public AsyncDocumentService(CvProcessingService cvProcessingService,
                                PdfGenerationService pdfGenerationService,
                                ZipService zipService,
                                ObjectMapper objectMapper,
                                FileStorageService fileStorageService) {
        this.cvProcessingService = cvProcessingService;
        this.pdfGenerationService = pdfGenerationService;
        this.zipService = zipService;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
    }

    @Async
    public void processAndGenerateDocumentsStream(SseEmitter emitter, MultipartFile file, String apiKey, String jobDescription, boolean generateCoverLetter) {
        try {
            sendProgress(emitter, "EXTRACTING_TEXT", "CV'den metin çıkarılıyor...", null);
            String extractedCvText = cvProcessingService.extractTextFromPdf(file);

            sendProgress(emitter, "PROCESSING_CV", "Yapay zeka CV'nizi analiz ediyor...", null);
            String structuredCvData = cvProcessingService.getStructuredDataFromGemini(extractedCvText, apiKey);
            byte[] atsCvPdfBytes = pdfGenerationService.createAtsFriendlyPdf(structuredCvData);

            byte[] finalFileBytes;
            String finalFileName;

            if (generateCoverLetter) {
                sendProgress(emitter, "GENERATING_COVER_LETTER", "Ön yazı oluşturuluyor, bu son adım...", null);
                String coverLetterText = cvProcessingService.generateCoverLetter(structuredCvData, jobDescription, apiKey);

                sendProgress(emitter, "ZIPPING_FILES", "Dosyalar paketleniyor...", null);
                Map<String, byte[]> filesToZip = Map.of(
                        "ATS_Uyumlu_CV.pdf", atsCvPdfBytes,
                        "On_Yazi.txt", coverLetterText.getBytes()
                );
                finalFileBytes = zipService.createZipFile(filesToZip);
                finalFileName = "CV_ve_On_Yazi.zip";
            } else {
                finalFileBytes = atsCvPdfBytes;
                finalFileName = "ATS_Uyumlu_CV.pdf";
            }

            sendProgress(emitter, "SAVING_FILE", "Dosya indirmeye hazırlanıyor...", null);
            String fileId = fileStorageService.saveFile(finalFileBytes);

            // --- YENİ EKLENEN MANUEL TEST LOGU ---
            // Bu log, sorunun frontend mi backend mi olduğunu kesinleştirecek.
            String downloadUrl = "http://localhost:8080/api/v1/cv/download?fileId=" + fileId + "&filename=" + finalFileName;
            System.out.println("=====================================================================");
            System.out.println("INDIRME URL'SI HAZIR: " + downloadUrl);
            System.out.println("LÜTFEN BU URL'YI KOPYALAYIP TARAYICINIZA YAPIŞTIRIN.");
            System.out.println("=====================================================================");
            // --- TEST LOGU SONU ---

            ProgressUpdate finalUpdate = new ProgressUpdate("DOWNLOAD_READY", finalFileName, fileId);
            sendProgressObject(emitter, finalUpdate);

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("İndirmeye hazırlık beklemesi sırasında kesinti oluştu.", e);
            }

            emitter.complete();

        } catch (Exception e) {
            logger.error("Asenkron akış sırasında bir hata oluştu.", e);
            try {
                String userFriendlyError = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                sendProgress(emitter, "ERROR", userFriendlyError, null);
                emitter.completeWithError(e);
            } catch (Exception ex) {
                logger.error("SSE Emitter'a hata durumu gönderilemedi.", ex);
            }
        }
    }

    private void sendProgressObject(SseEmitter emitter, ProgressUpdate update) {
        try {
            logger.info("İstemciye durum güncellemesi gönderiliyor: {}", update.getMessage());
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(update)));

            Thread.sleep(50);

        } catch (Exception e) {
            logger.warn("İstemciye SSE mesajı gönderilemedi (bağlantı kopmuş olabilir): {}", e.getMessage());
        }
    }

    private void sendProgress(SseEmitter emitter, String stage, String message, String data) {
        sendProgressObject(emitter, new ProgressUpdate(stage, message, data));
    }

    @Async
    public void executeTestStream(SseEmitter emitter) {
        try {
            logger.info(">>> ASYNC TEST: Spring tarafından yönetilen test akışı başladı.");
            emitter.send(SseEmitter.event().data("{\"stage\":\"TEST_1\", \"message\":\"Test Adım 1: Bağlantı kuruldu.\"}"));
            Thread.sleep(1500);

            logger.info(">>> ASYNC TEST: İkinci mesaj gönderiliyor.");
            emitter.send(SseEmitter.event().data("{\"stage\":\"TEST_2\", \"message\":\"İşlem devam ediyor...\"}"));
            Thread.sleep(1500);

            logger.info(">>> ASYNC TEST: Son mesaj gönderiliyor.");
            emitter.send(SseEmitter.event().data("{\"stage\":\"TEST_COMPLETE\", \"message\":\"Test başarıyla tamamlandı!\"}"));

            logger.info(">>> ASYNC TEST: Akış tamamlandı.");
            emitter.complete();
        } catch (Exception e) {
            logger.error(">>> ASYNC TEST HATA:", e);
            emitter.completeWithError(e);
        }
    }
}