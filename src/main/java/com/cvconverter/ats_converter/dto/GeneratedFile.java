// src/main/java/com/cvconverter/ats_converter/dto/GeneratedFile.java
package com.cvconverter.ats_converter.dto;

import org.springframework.http.MediaType;

public class GeneratedFile {
    private final byte[] content;
    private final String fileName;
    private final MediaType mediaType;

    public GeneratedFile(byte[] content, String fileName, MediaType mediaType) {
        this.content = content;
        this.fileName = fileName;
        this.mediaType = mediaType;
    }

    // Getter metotları
    public byte[] getContent() { return content; }
    public String getFileName() { return fileName; }
    public MediaType getMediaType() { return mediaType; }
}