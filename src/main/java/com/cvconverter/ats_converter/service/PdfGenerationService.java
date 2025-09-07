// src/main/java/com/cvconverter/ats_converter/service/PdfGenerationService.java
package com.cvconverter.ats_converter.service;

import com.cvconverter.ats_converter.dto.CvDataDto;
import com.cvconverter.ats_converter.dto.EducationDto;
import com.cvconverter.ats_converter.dto.ExperienceDto;
import com.cvconverter.ats_converter.dto.PersonalInfoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfGenerationService {

    private final ObjectMapper objectMapper;

    public PdfGenerationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Gemini'den gelen metnin hem Java'daki null değerini hem de "null" kelimesini
     * kontrol edip temizleyen GÜÇLENDİRİLMİŞ yardımcı metot.
     * Bu metot, PDF'te istenmeyen "null" yazılarının görünmesini engeller.
     * @param text Kontrol edilecek metin.
     * @return Temizlenmiş ve kırpılmış metin veya boş string.
     */
    private String safeGet(String text) {
        if (text == null || text.trim().equalsIgnoreCase("null")) {
            return "";
        }
        return text.trim();
    }

    /**
     * Gemini'den gelen yapılandırılmış JSON verisini alır, parse eder ve ATS uyumlu bir PDF oluşturur.
     * @param structuredJsonData Gemini tarafından üretilen JSON string'i.
     * @return Oluşturulan PDF dosyasının byte dizisi.
     */
    public byte[] createAtsFriendlyPdf(String structuredJsonData) {

        CvDataDto cvData;
        try {
            cvData = objectMapper.readValue(structuredJsonData, CvDataDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Gelen JSON verisi parse edilemedi.", e);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Türkçe karakter destekli font tanımlamaları
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, "Cp1254", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bf, 18, Font.BOLD);
            Font headingFont = new Font(bf, 14, Font.BOLD);
            Font bodyFont = new Font(bf, 11);
            Font boldBodyFont = new Font(bf, 11, Font.BOLD);

            // --- KİŞİSEL BİLGİLER (EN GÜVENLİ HALİ) ---
            if (cvData.getKisiselBilgiler() != null) {
                PersonalInfoDto personalInfo = cvData.getKisiselBilgiler();
                document.add(new Paragraph(safeGet(personalInfo.getIsim()), titleFont));

                // İletişim bilgilerini sadece doluysa ve "null" değilse ekle
                List<String> contactParts = new ArrayList<>();
                String email = safeGet(personalInfo.getEmail());
                String telefon = safeGet(personalInfo.getTelefon());

                if (!email.isEmpty()) contactParts.add(email);
                if (!telefon.isEmpty()) contactParts.add(telefon);

                String contactInfo = String.join(" | ", contactParts);
                if (!contactInfo.isEmpty()) {
                    document.add(new Paragraph(contactInfo, bodyFont));
                }
                document.add(Chunk.NEWLINE);
            }

            // --- İŞ DENEYİMİ (GÜVENLİ HALİ) ---
            if (cvData.getIsDeneyimleri() != null && !cvData.getIsDeneyimleri().isEmpty()) {
                document.add(new Paragraph("İŞ DENEYİMİ", headingFont));
                addGrayLine(document);
                for (ExperienceDto exp : cvData.getIsDeneyimleri()) {
                    Paragraph expTitle = new Paragraph();
                    expTitle.add(new Chunk(safeGet(exp.getUnvan()), boldBodyFont));
                    expTitle.add(new Chunk(" | " + safeGet(exp.getSirket()), bodyFont));
                    document.add(expTitle);

                    document.add(new Paragraph(safeGet(exp.getTarihler()), bodyFont));
                    document.add(new Paragraph(safeGet(exp.getAciklama()), bodyFont));
                    document.add(Chunk.NEWLINE);
                }
            }

            // --- EĞİTİM BİLGİLERİ (GÜVENLİ HALİ) ---
            if (cvData.getEgitimBilgileri() != null && !cvData.getEgitimBilgileri().isEmpty()) {
                document.add(new Paragraph("EĞİTİM", headingFont));
                addGrayLine(document);
                for (EducationDto edu : cvData.getEgitimBilgileri()) {
                    document.add(new Paragraph(safeGet(edu.getOkul()), boldBodyFont));
                    document.add(new Paragraph(safeGet(edu.getBolum()) + " - " + safeGet(edu.getDerece()), bodyFont));
                    document.add(new Paragraph(safeGet(edu.getTarihler()), bodyFont));
                    document.add(Chunk.NEWLINE);
                }
            }

            // --- YETENEKLER (GÜVENLİ HALİ) ---
            if (cvData.getYetenekler() != null && !cvData.getYetenekler().isEmpty()) {
                document.add(new Paragraph("YETENEKLER", headingFont));
                addGrayLine(document);

                // Yetenek listesindeki "null" veya boş değerleri filtreleyerek birleştir
                String skills = cvData.getYetenekler().stream()
                        .map(this::safeGet) // Her bir yeteneği temizle
                        .filter(skill -> !skill.isEmpty()) // Boş olanları listeden çıkar
                        .collect(Collectors.joining(", ")); // Kalanları virgülle birleştir

                if (!skills.isEmpty()) {
                    document.add(new Paragraph(skills, bodyFont));
                }
            }

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("PDF oluşturulurken bir hata oluştu.", e);
        }

        return baos.toByteArray();
    }

    // PDF'e gri bir ayırıcı çizgi ekleyen yardımcı metot.
    private void addGrayLine(Document document) throws DocumentException {
        LineSeparator separator = new LineSeparator(0.5f, 100, Color.GRAY, Element.ALIGN_CENTER, 0);
        document.add(new Chunk(separator));
    }
}