// src/main/java/com/cvconverter/ats_converter/service/PdfGenerationService.java
package com.cvconverter.ats_converter.service;

import com.cvconverter.ats_converter.dto.CvDataDto;
import com.cvconverter.ats_converter.dto.EducationDto;
import com.cvconverter.ats_converter.dto.ExperienceDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.awt.Color;
import com.lowagie.text.Element; // Bu da gri çizgi için gerekli olabilir.

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfGenerationService {

    // Jackson'ın JSON'dan Java nesnesine dönüştürme işlemini yapacak olan sihirli nesnesi.
    // Spring, bu nesneyi bizim için otomatik olarak oluşturup enjekte eder.
    private final ObjectMapper objectMapper;

    public PdfGenerationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Gemini'den gelen yapılandırılmış JSON verisini alır, parse eder ve ATS uyumlu bir PDF oluşturur.
     * @param structuredJsonData Gemini tarafından üretilen JSON string'i.
     * @return Oluşturulan PDF dosyasının byte dizisi.
     */
    public byte[] createAtsFriendlyPdf(String structuredJsonData) {

        // 1. ADIM: JSON String'ini Java Nesnesine (CvDataDto) Dönüştürme
        CvDataDto cvData;
        try {
            // objectMapper, JSON metnini okur ve CvDataDto sınıfımızın yapısına göre doldurur.
            cvData = objectMapper.readValue(structuredJsonData, CvDataDto.class);
        } catch (JsonProcessingException e) {
            // Eğer Gemini'den gelen JSON bozuksa veya beklediğimiz formatta değilse hata fırlat.
            throw new RuntimeException("Gelen JSON verisi parse edilemedi.", e);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // 2. ADIM: PDF için Fontları Tanımlama (Türkçe Karakter Destekli)
            // "Cp1254" karakter seti, ı, ğ, ş gibi Türkçe karakterleri destekler.
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, "Cp1254", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bf, 18, Font.BOLD);
            Font headingFont = new Font(bf, 14, Font.BOLD);
            Font bodyFont = new Font(bf, 11);
            Font boldBodyFont = new Font(bf, 11, Font.BOLD);

            // 3. ADIM: Parse Edilen Verileri Dinamik Olarak PDF'e Yazma

            // --- KİŞİSEL BİLGİLER ---
            if (cvData.getKisiselBilgiler() != null) {
                document.add(new Paragraph(cvData.getKisiselBilgiler().getIsim(), titleFont));
                String contactInfo = String.join(" | ",
                        cvData.getKisiselBilgiler().getEmail(),
                        cvData.getKisiselBilgiler().getTelefon());
                document.add(new Paragraph(contactInfo, bodyFont));
                document.add(Chunk.NEWLINE); // Boşluk ekler
            }

            // --- İŞ DENEYİMİ ---
            if (cvData.getIsDeneyimleri() != null && !cvData.getIsDeneyimleri().isEmpty()) {
                document.add(new Paragraph("İŞ DENEYİMİ", headingFont));
                addGrayLine(document); // Ayırıcı çizgi
                for (ExperienceDto exp : cvData.getIsDeneyimleri()) {
                    Paragraph expTitle = new Paragraph();
                    expTitle.add(new Chunk(exp.getUnvan(), boldBodyFont));
                    expTitle.add(new Chunk(" | " + exp.getSirket(), bodyFont));
                    document.add(expTitle);

                    document.add(new Paragraph(exp.getTarihler(), bodyFont));
                    document.add(new Paragraph(exp.getAciklama(), bodyFont));
                    document.add(Chunk.NEWLINE);
                }
            }

            // --- EĞİTİM BİLGİLERİ ---
            if (cvData.getEgitimBilgileri() != null && !cvData.getEgitimBilgileri().isEmpty()) {
                document.add(new Paragraph("EĞİTİM", headingFont));
                addGrayLine(document);
                for (EducationDto edu : cvData.getEgitimBilgileri()) {
                    document.add(new Paragraph(edu.getOkul(), boldBodyFont));
                    document.add(new Paragraph(edu.getBolum() + " - " + edu.getDerece(), bodyFont));
                    document.add(new Paragraph(edu.getTarihler(), bodyFont));
                    document.add(Chunk.NEWLINE);
                }
            }

            // --- YETENEKLER ---
            if (cvData.getYetenekler() != null && !cvData.getYetenekler().isEmpty()) {
                document.add(new Paragraph("YETENEKLER", headingFont));
                addGrayLine(document);
                // Yetenekleri virgülle ayırarak tek bir satırda yaz.
                String skills = String.join(", ", cvData.getYetenekler());
                document.add(new Paragraph(skills, bodyFont));
            }

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("PDF oluşturulurken bir hata oluştu.", e);
        }

        return baos.toByteArray();
    }

    // PDF'e gri bir ayırıcı çizgi ekleyen yardımcı metot.
    private void addGrayLine(Document document) throws DocumentException {
        Paragraph p = new Paragraph(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(0.5f, 100, Color.GRAY, Element.ALIGN_CENTER, 0)));
        document.add(p);
    }
}