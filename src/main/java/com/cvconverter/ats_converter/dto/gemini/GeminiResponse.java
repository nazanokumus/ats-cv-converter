// src/main/java/com/cvconverter/ats_converter/dto/gemini/GeminiResponse.java
package com.cvconverter.ats_converter.dto.gemini;

import lombok.Data;
import java.util.List;

@Data
public class GeminiResponse {
    private List<Candidate> candidates;
}