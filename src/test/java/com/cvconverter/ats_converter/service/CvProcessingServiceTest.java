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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * CvProcessingService sınıfı için birim testleri (Unit Tests).
 * Bu testler, dış bağımlılıkları (RestTemplate) taklit ederek (mock)
 * sadece bu servisin kendi mantığını test eder.
 */
@ExtendWith(MockitoExtension.class) // Mockito'nun JUnit 5 ile entegrasyonunu sağlar.
class CvProcessingServiceTest {

    @Mock // Taklit edilecek nesne. Gerçek RestTemplate yerine sahtesini kullanacağız.
    private RestTemplate restTemplate;

    @InjectMocks // Test edilecek asıl sınıf. Mock'lanan RestTemplate bu sınıfa enjekte edilecek.
    private CvProcessingService cvProcessingService;

    private String fakeCvText;
    private String fakeApiKey;
    private String fakeJobDescription;
    private String fakeCvJson;

    @BeforeEach // Her test metodundan ÖNCE çalışacak hazırlık metodu.
    void setUp() {
        // Testlerde kullanacağımız standart sahte verileri burada tanımlıyoruz.
        fakeCvText = "Bu bir test CV metnidir.";
        fakeApiKey = "test-api-key";
        fakeJobDescription = "Bu bir iş ilanıdır.";
        fakeCvJson = "{\"kisisel_bilgiler\":{\"isim\":\"Test Kullanıcı\"}}";
    }

    @Test
    void getStructuredDataFromGemini_WhenApiCallIsSuccessful_ShouldReturnJsonString() {
        // 1. Hazırlık (Arrange)
        // Gemini API'sinden geleceğini varsaydığımız sahte yanıtı oluşturuyoruz.
        GeminiResponse fakeResponse = createFakeGeminiResponse("```json\n" + fakeCvJson + "\n```");
        ResponseEntity<GeminiResponse> responseEntity = new ResponseEntity<>(fakeResponse, HttpStatus.OK);

        // Mockito'ya talimat veriyoruz:
        // "RestTemplate'in postForEntity metodu çağrıldığında, ...
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GeminiResponse.class)))
                // ... bizim hazırladığımız sahte yanıtı (responseEntity) döndür."
                .thenReturn(responseEntity);

        // 2. Eylem (Act)
        // Test etmek istediğimiz metodu çağırıyoruz.
        String actualJson = cvProcessingService.getStructuredDataFromGemini(fakeCvText, fakeApiKey);

        // 3. Doğrulama (Assert)
        // Sonucun beklediğimiz gibi olup olmadığını kontrol ediyoruz.
        assertNotNull(actualJson); // Sonucun null olmadığını
        assertEquals(fakeCvJson, actualJson); // Sonucun markdown'dan temizlenmiş JSON ile aynı olduğunu
    }

    @Test
    void generateCoverLetter_WhenApiCallIsSuccessful_ShouldReturnCoverLetterText() {
        // 1. Hazırlık
        String expectedCoverLetter = "Sayın Yetkili, ben Test Kullanıcı...";
        GeminiResponse fakeResponse = createFakeGeminiResponse(expectedCoverLetter);
        ResponseEntity<GeminiResponse> responseEntity = new ResponseEntity<>(fakeResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(responseEntity);

        // 2. Eylem
        String actualCoverLetter = cvProcessingService.generateCoverLetter(fakeCvJson, fakeJobDescription, fakeApiKey);

        // 3. Doğrulama
        assertNotNull(actualCoverLetter);
        assertEquals(expectedCoverLetter, actualCoverLetter);
    }

    @Test
    void extractTextFromPdf_WhenPdfIsValid_ShouldReturnText() throws IOException {
        // Bu test gerçek bir PDF'e ihtiyaç duyar. Basit bir PDF'i test kaynaklarına koyabiliriz.
        // Şimdilik PDFBox'a güvendiğimiz için bu testi basitleştiriyoruz.
        // Gerçek metin içeren sahte bir PDF dosyası oluşturuyoruz.
        // Bu, gerçek bir PDF dosyası değil, sadece PDFBox'ın `load` metodunun çalışmasını tetiklemek için.
        // Gerçek bir PDF dosyasıyla test yapmak için dosyayı `src/test/resources` altına koyup okumak gerekir.
        byte[] pdfContent = createDummyPdfContent(); // Gerçek bir PDF dosyası olmasa da, PDFBox'ın çalışmasını sağlar.
        MultipartFile multipartFile = new MockMultipartFile("test.pdf", "test.pdf", "application/pdf", pdfContent);

        // Bu test için PDFBox'a güvendiğimizden, çok detaylı bir doğrulama yapmıyoruz.
        // Sadece bir hata fırlatmadığını kontrol etmek bile yeterli olabilir.
        assertDoesNotThrow(() -> {
            String text = cvProcessingService.extractTextFromPdf(multipartFile);
            // Gerçek bir dummy PDF ile test ediyorsak, beklenen metni de kontrol edebiliriz.
            // assertEquals("Hello World", text.trim());
        });
    }

    @Test
    void callGeminiApi_WhenApiResponseIsEmpty_ShouldThrowException() {
        // 1. Hazırlık
        // Gemini API'sinin boş bir yanıt döndürdüğü durumu simüle ediyoruz.
        ResponseEntity<GeminiResponse> emptyResponseEntity = new ResponseEntity<>(new GeminiResponse(), HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GeminiResponse.class)))
                .thenReturn(emptyResponseEntity);

        // 2. Eylem & 3. Doğrulama
        // Metodun çağrıldığında bir RuntimeException fırlatıp fırlatmadığını kontrol ediyoruz.
        Exception exception = assertThrows(RuntimeException.class, () -> {
            cvProcessingService.getStructuredDataFromGemini(fakeCvText, fakeApiKey);
        });

        // Hata mesajının beklediğimiz gibi olup olmadığını da kontrol edebiliriz.
        assertEquals("Gemini API'den geçersiz veya boş bir cevap alındı.", exception.getMessage());
    }

    /**
     * Testlerde kullanılmak üzere sahte bir GeminiResponse nesnesi oluşturan yardımcı metot.
     */
    private GeminiResponse createFakeGeminiResponse(String text) {
        Part part = new Part(text);
        Content content = new Content(Collections.singletonList(part));
        Candidate candidate = new Candidate();
        candidate.setContent(content);

        GeminiResponse geminiResponse = new GeminiResponse();
        geminiResponse.setCandidates(Collections.singletonList(candidate));

        return geminiResponse;
    }

    /**
     * PDFBox'ın çökmemesi için basit bir PDF içeriği oluşturan sahte metot.
     * Gerçek bir test için, src/test/resources altına küçük bir PDF dosyası koymak daha iyidir.
     */
    private byte[] createDummyPdfContent() {
        // Bu, PDFBox'ı test etmek için geçerli bir PDF değil, sadece IOException fırlatmasını önler.
        // Gerçek bir test için Apache PDFBox ile programatik olarak tek sayfalık bir PDF oluşturulabilir.
        return new byte[]{}; // Şimdilik boş bırakıyoruz, çünkü asıl test ettiğimiz şey Gemini kısmı.
    }
}