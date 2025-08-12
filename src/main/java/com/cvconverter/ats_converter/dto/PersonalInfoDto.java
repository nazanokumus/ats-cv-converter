// src/main/java/com/cvconverter/ats_converter/dto/PersonalInfoDto.java
package com.cvconverter.ats_converter.dto;

import lombok.Data;

@Data // Getter, Setter, toString gibi metotları otomatik oluşturur.
public class PersonalInfoDto {
    private String isim;
    private String email;
    private String telefon;
    private String adres; // Ekstra alanlar ekleyebiliriz.
}
