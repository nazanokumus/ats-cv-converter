// Content.java
package com.cvconverter.ats_converter.dto.gemini;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class Content { // <-- public anahtar kelimesi burada
    private List<Part> parts;
}
