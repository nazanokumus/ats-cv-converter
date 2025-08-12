// src/main/java/com/cvconverter/ats_converter/dto/CvDataDto.java
package com.cvconverter.ats_converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CvDataDto {
    // JSON'daki 'kisisel_bilgiler' anahtarını bu alana eşle
    @JsonProperty("kisisel_bilgiler")
    private PersonalInfoDto kisiselBilgiler;

    // JSON'daki 'is_deneyimleri' anahtarını bu alana eşle
    @JsonProperty("is_deneyimleri")
    private List<ExperienceDto> isDeneyimleri;

    // JSON'daki 'egitim_bilgileri' anahtarını bu alana eşle
    @JsonProperty("egitim_bilgileri")
    private List<EducationDto> egitimBilgileri;

    private List<String> yetenekler;
}
