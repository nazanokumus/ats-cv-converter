// src/main/java/com/cvconverter/ats_converter/dto/gemini/GeminiRequest.java
package com.cvconverter.ats_converter.dto.gemini;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class GeminiRequest {
    private List<Content> contents;
}