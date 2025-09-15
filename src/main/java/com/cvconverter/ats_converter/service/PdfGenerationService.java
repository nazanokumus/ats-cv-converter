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

    private String safeGet(String text) {
        if (text == null || text.trim().equalsIgnoreCase("null") || text.trim().isEmpty()) {
            return "";
        }
        return text.trim();
    }

    public byte[] createAtsFriendlyPdf(String structuredJsonData) {
        CvDataDto cvData;
        try {
            cvData = objectMapper.readValue(structuredJsonData, CvDataDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Yapay zekadan gelen JSON verisi anlaşılamadı.", e);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40); // Kenar boşlukları
        boolean hasContentBeenAdded = false;

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, "Cp1254", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bf, 18, Font.BOLD);
            Font headingFont = new Font(bf, 12, Font.BOLD);
            Font bodyFont = new Font(bf, 10);
            Font boldBodyFont = new Font(bf, 10, Font.BOLD);

            // --- KİŞİSEL BİLGİLER ---
            if (cvData.getKisiselBilgiler() != null) {
                PersonalInfoDto p = cvData.getKisiselBilgiler();
                String isim = safeGet(p.getIsim());
                if (!isim.isEmpty()) {
                    Paragraph nameParagraph = new Paragraph(isim.toUpperCase(), titleFont);
                    nameParagraph.setAlignment(Element.ALIGN_CENTER);
                    document.add(nameParagraph);
                    hasContentBeenAdded = true;
                }

                List<String> contactParts = new ArrayList<>();
                if (!safeGet(p.getTelefon()).isEmpty()) contactParts.add(safeGet(p.getTelefon()));
                if (!safeGet(p.getEmail()).isEmpty()) contactParts.add(safeGet(p.getEmail()));
                if (!safeGet(p.getAdres()).isEmpty()) contactParts.add(safeGet(p.getAdres()));

                String contactInfo = String.join(" | ", contactParts);
                if (!contactInfo.isEmpty()) {
                    Paragraph contactParagraph = new Paragraph(contactInfo, bodyFont);
                    contactParagraph.setAlignment(Element.ALIGN_CENTER);
                    document.add(contactParagraph);
                    document.add(new Paragraph(" "));
                }
            }

            // --- İŞ DENEYİMİ ---
            if (cvData.getIsDeneyimleri() != null && !cvData.getIsDeneyimleri().isEmpty()) {
                document.add(new Paragraph("İŞ DENEYİMİ", headingFont));
                addGrayLine(document);
                for (ExperienceDto exp : cvData.getIsDeneyimleri()) {
                    if (safeGet(exp.getUnvan()).isEmpty() && safeGet(exp.getSirket()).isEmpty()) continue;

                    Paragraph experienceHeader = new Paragraph();
                    experienceHeader.add(new Chunk(safeGet(exp.getUnvan()), boldBodyFont));
                    experienceHeader.add(new Chunk(" at ", bodyFont));
                    experienceHeader.add(new Chunk(safeGet(exp.getSirket()), boldBodyFont));
                    document.add(experienceHeader);

                    Paragraph dates = new Paragraph(safeGet(exp.getTarihler()), bodyFont);
                    document.add(dates);

                    if(!safeGet(exp.getAciklama()).isEmpty()){
                        Paragraph description = new Paragraph(safeGet(exp.getAciklama()), bodyFont);
                        document.add(description);
                    }

                    document.add(new Paragraph(" "));
                    hasContentBeenAdded = true;
                }
            }

            // --- EĞİTİM BİLGİLERİ ---
            if (cvData.getEgitimBilgileri() != null && !cvData.getEgitimBilgileri().isEmpty()) {
                document.add(new Paragraph("EĞİTİM", headingFont));
                addGrayLine(document);
                for (EducationDto edu : cvData.getEgitimBilgileri()) {
                    if (safeGet(edu.getOkul()).isEmpty() && safeGet(edu.getBolum()).isEmpty()) continue;

                    Paragraph eduHeader = new Paragraph();
                    eduHeader.add(new Chunk(safeGet(edu.getBolum()), boldBodyFont));
                    eduHeader.add(new Chunk(", " + safeGet(edu.getDerece()), bodyFont));
                    document.add(eduHeader);

                    Paragraph school = new Paragraph(safeGet(edu.getOkul()), bodyFont);
                    document.add(school);

                    Paragraph dates = new Paragraph(safeGet(edu.getTarihler()), bodyFont);
                    document.add(dates);

                    document.add(new Paragraph(" "));
                    hasContentBeenAdded = true;
                }
            }

            // --- YETENEKLER ---
            if (cvData.getYetenekler() != null && !cvData.getYetenekler().isEmpty()) {
                String skills = cvData.getYetenekler().stream()
                        .map(this::safeGet)
                        .filter(skill -> !skill.isEmpty())
                        .collect(Collectors.joining(", "));
                if (!skills.isEmpty()) {
                    document.add(new Paragraph("YETENEKLER", headingFont));
                    addGrayLine(document);
                    document.add(new Paragraph(skills, bodyFont));
                    hasContentBeenAdded = true;
                }
            }

            if (!hasContentBeenAdded) {
                throw new RuntimeException("Yapay zeka CV'den herhangi bir anlamlı veri çıkaramadı. Lütfen farklı bir CV dosyası deneyin.");
            }

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("PDF oluşturulurken bir hata oluştu.", e);
        }

        return baos.toByteArray();
    }

    private void addGrayLine(Document document) throws DocumentException {
        document.add(new Paragraph(" ")); // Çizgi öncesi küçük bir boşluk
        LineSeparator separator = new LineSeparator(0.5f, 100, Color.GRAY, Element.ALIGN_CENTER, -5);
        document.add(new Chunk(separator));
        document.add(new Paragraph(" ")); // Çizgi sonrası küçük bir boşluk
    }
}