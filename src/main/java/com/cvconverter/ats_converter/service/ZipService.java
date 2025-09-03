package com.cvconverter.ats_converter.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    /**
     * Verilen dosyaları (dosya adı -> dosya içeriği) tek bir ZIP dosyası olarak paketler.
     * @param filesToZip ZIP'lenecek dosyaları içeren bir harita (Map).
     * @return Oluşturulan ZIP dosyasının byte dizisi.
     * @throws IOException ZIP oluşturma sırasında bir hata olursa fırlatılır.
     */
    public byte[] createZipFile(Map<String, byte[]> filesToZip) throws IOException {
        // Sonuç ZIP dosyasının byte'larını hafızada tutmak için bir ByteArrayOutputStream oluşturuyoruz.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // try-with-resources ile, ZipOutputStream'in her durumda kapatılmasını garantiliyoruz.
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Bize verilen her bir dosya için döngüye giriyoruz.
            for (Map.Entry<String, byte[]> entry : filesToZip.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileContent = entry.getValue();

                // ZIP dosyasının içine, o dosya adıyla yeni bir "girdi" (entry) oluşturuyoruz.
                // Bu, ZIP'in içindeki dosyanın adı ve yapısıdır.
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);

                // O girdinin içine, dosyanın asıl içeriğini (byte'larını) yazıyoruz.
                zos.write(fileContent);

                // Bu girdiyi kapatarak, bir sonraki dosyaya hazırlanıyoruz.
                zos.closeEntry();
            }
        }

        // Bütün dosyalar eklendikten sonra, hafızadaki o stream'i alıp,
        // bir byte dizisine dönüştürüyoruz ve geri yolluyoruz.
        return baos.toByteArray();
    }
}