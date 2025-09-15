package com.cvconverter.ats_converter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    private static final Logger logger = LoggerFactory.getLogger(ZipService.class);

    /**
     * Verilen dosyaları (dosya adı -> dosya içeriği) içeren bir ZİP arşivi oluşturur.
     * Bu metot, kaynakları güvenli bir şekilde yönetmek için try-with-resources kullanır.
     * @param filesToZip Dosya adını (String) dosya içeriğine (byte[]) eşleyen bir harita.
     * @return Oluşturulan ZİP dosyasının byte dizisi.
     */
    public byte[] createZipFile(Map<String, byte[]> filesToZip) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            logger.info("{} adet dosya ZİP'leniyor...", filesToZip.size());

            for (Map.Entry<String, byte[]> fileEntry : filesToZip.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(fileEntry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(fileEntry.getValue());
                zos.closeEntry();
            }

            zos.finish();
            logger.info("ZİP dosyası başarıyla oluşturuldu.");
            return baos.toByteArray();

        } catch (IOException e) {
            // Hata durumunda, kontrol edilmesi gereken bir istisna yerine RuntimeException fırlat.
            // Bu, diğer servislerin hata yönetim stiliyle tutarlıdır.
            logger.error("ZİP dosyası oluşturulurken bir G/Ç hatası oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("ZİP dosyası oluşturulurken bir hata meydana geldi.", e);
        }
    }
}