package com.cvconverter.ats_converter.service;

import com.cvconverter.ats_converter.dto.gemini.Candidate;
import com.cvconverter.ats_converter.dto.gemini.Content;
import com.cvconverter.ats_converter.dto.gemini.GeminiResponse;
import com.cvconverter.ats_converter.dto.gemini.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvProcessingServiceTest {

    @Mock // Bu, "sahte" bir RestTemplate. Gerçek bir HTTP isteği yapmayacak.
    private RestTemplate restTemplate;

    @InjectMocks // Bu, test edeceğimiz GERÇEK CvProcessingService. Mockito, yukarıdaki sahte RestTemplate'i bunun içine enjekte edecek.
    private CvProcessingService cvProcessingService;

    // Her testten ÖNCE bu metodun çalışmasını sağlayan bir setup metodu.
    @BeforeEach
    void setUp() {
        // Servisin içindeki 'geminiApiKey' alanına sahte bir değer atıyoruz.
        // Bu, application.properties'e bağımlılığı ortadan kaldırır.
        ReflectionTestUtils.setField(cvProcessingService, "geminiApiKey", "SAHTE_API_ANAHTARI");
    }

    @Test
    void getStructuredDataFromGemini_shouldReturnCorrectJson_whenApiCallIsSuccessful() {
        // --- ARRANGE (Hazırlık) ---
        // Test için kullanılacak sahte bir CV metni
        String dummyCvText = "Ben Nazan, bir yazılım mühendisiyim.";

        // Gemini'nin DÖNDÜRMESİNİ İSTEDİĞİMİZ sahte cevabı hazırlıyoruz
        Part responsePart = new Part("{\"isim\":\"Nazan\"}");
        Content responseContent = new Content(Collections.singletonList(responsePart));
        Candidate responseCandidate = new Candidate();
        responseCandidate.setContent(responseContent);
        GeminiResponse fakeGeminiResponse = new GeminiResponse();
        fakeGeminiResponse.setCandidates(Collections.singletonList(responseCandidate));

        // Mockito'ya o en önemli emri veriyoruz:
        // "Ne zaman ki 'restTemplate.postForEntity' metodu çağrılırsa...
        // ... (URL, body ne olursa olsun fark etmez) ...
        // ... GERÇEK BİR AĞ ÇAĞRISI YAPMA. Bunun yerine, benim yukarıda hazırladığım 'fakeGeminiResponse' nesnesini DÖNDÜR."
        when(restTemplate.postForEntity(anyString(), any(), eq(GeminiResponse.class)))
                .thenReturn(ResponseEntity.ok(fakeGeminiResponse));

        // --- ACT (Eylem) ---
        // Şimdi, test ettiğimiz metodu GERÇEKTEN çağırıyoruz.
        String actualJsonResponse = cvProcessingService.getStructuredDataFromGemini(dummyCvText);

        // --- ASSERT (Doğrulama) ---
        // Sonucun beklediğimiz gibi olup olmadığını kontrol ediyoruz
        assertNotNull(actualJsonResponse, "Dönen JSON null olmamalı.");
        assertEquals("{\"isim\":\"Nazan\"}", actualJsonResponse, "Dönen JSON, sahte cevaptakiyle aynı olmalı.");

        // Bonus: RestTemplate.postForEntity metodunun TAM OLARAK 1 KERE çağrıldığından emin ol.
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(GeminiResponse.class));
    }
}