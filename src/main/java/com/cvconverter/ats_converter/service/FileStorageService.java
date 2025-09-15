package com.cvconverter.ats_converter.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Oluşturulan dosyaları indirme isteği gelene kadar geçici olarak hafızada tutan servis.
 * Bu yapı, dosyaların Base64 formatında ağ üzerinden gönderilmesini engeller.
 */
@Service
public class FileStorageService {

    // Eş zamanlı erişime uygun, thread-safe bir Map kullanarak dosyaları saklar.
    // Key: Benzersiz dosya ID'si (UUID), Value: Dosyanın byte içeriği
    private final Map<String, byte[]> temporaryStorage = new ConcurrentHashMap<>();

    /**
     * Verilen dosya içeriğini hafızaya kaydeder ve ona özel benzersiz bir kimlik (ID) döndürür.
     * @param fileContent Saklanacak dosyanın byte dizisi.
     * @return Dosyaya erişim için kullanılacak benzersiz String ID.
     */
    public String saveFile(byte[] fileContent) {
        String fileId = UUID.randomUUID().toString();
        temporaryStorage.put(fileId, fileContent);
        return fileId;
    }

    /**
     * Verilen ID'ye sahip dosyayı hafızadan getirir ve ardından siler.
     * Bu, her dosyanın sadece bir kez indirilebilmesini sağlar ve hafızayı temiz tutar.
     * @param fileId İndirilmek istenen dosyanın ID'si.
     * @return Dosyanın byte dizisi veya bulunamazsa null.
     */
    public byte[] getFile(String fileId) {
        return temporaryStorage.remove(fileId);
    }
}