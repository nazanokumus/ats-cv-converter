package com.cvconverter.ats_converter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdate {
    private String stage; // Örn: "PROCESSING_CV", "GENERATING_COVER_LETTER", "COMPLETE", "ERROR"
    private String message; // Kullanıcıya gösterilecek mesaj
    private String data; // Tamamlandığında dosya verisi (Base64) veya hata detayı
}
