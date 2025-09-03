package com.cvconverter.ats_converter.config; // Kendi paket adına göre düzelttiğinden emin ol!

import org.springframework.beans.factory.annotation.Value; // @Value anotasyonu için gerekli
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity // Spring Security'nin bu sınıftaki ayarları kullanacağını belirtir.
public class SecurityConfig {

    // application.properties (veya .yml) dosyasındaki ayarları buraya enjekte ediyoruz.
    // Bu sayede cors.allowed.origins değerini merkezi bir yerden yönetebiliriz.
    @Value("${cors.allowed.origins}") // application.properties dosyasındaki "cors.allowed.origins" değerini buraya alacak.
    private String[] allowedOrigins;

    @Bean // Spring'e "Bu bir bean'dir, onu yönet" diyoruz.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS AYARLARI: Frontend'den gelen isteklere izin veriyoruz.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF KORUMASI: REST API'lar için genellikle devre dışı bırakılır, çünkü
                // stateless (durumsuz) yapılar için extra bir karmaşıklık getirir.
                .csrf(csrf -> csrf.disable())

                // YETKİLENDİRME KURALLARI: Hangi URL'lere kimin erişebileceğini belirliyoruz.
                .authorizeHttpRequests(auth -> auth
                        // "/api/**" ile başlayan TÜM yollara (bizim endpoint'lerimiz)
                        // sorgusuz sualsiz erişime izin veriyoruz.
                        .requestMatchers("/api/**").permitAll()
                        // Geri kalan TÜM diğer isteklere ise...
                        .anyRequest()
                        // ...kimlik doğrulaması (authentication) yapmadan GİREMEZsin diyoruz.
                        // (Bu projede henüz login/auth mekanizması olmadığı için bu satırın da bir önemi var.)
                        .authenticated()
                );

        return http.build();
    }

    // CORS Kurallarını Tanımlayan Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // İzin Verilen Kaynaklar (Origin):
        // Development ortamı için frontend'in çalıştığı portları buraya ekliyoruz.
        // Production ortamı için ise sadece kendi canlı uygulamanızın adresini eklemelisiniz.
        // '*' her yerden izin vermek anlamına gelir ama bu PRODUCTION için KESİNLİKLE KULLANILMAZ.
        // Yaptığımız değişiklik ile bu değer, application.properties'den dinamik olarak okunacak.
        // Şimdilik, local'de çalışırken bütün portlara izin verdiğimiz varsayımıyla devam ediyoruz.
        // TODO: Production için burayı sadece kendi domain'inizle değiştirin.
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // İzin Verilen Metotlar: Hangi HTTP metotlarına izin verilecek? (GET, POST, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // İzin Verilen Header'lar: İstemden hangi header'ların gelmesine izin verilecek?
        // "*" hepsine izin verir, ama güvenlik için sadece ihtiyacınız olanları belirtmek daha iyidir.
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Kimlik Bilgileri İzni: Tarayıcının isteklerle birlikte Cookie göndermesine izin verilir.
        // Bu, genellikle token tabanlı olmayan session yönetimi için kullanılır.
        // API'larınızda kimlik doğrulama (authentication) mekanizması varsa bu önemlidir.
        configuration.setAllowCredentials(true);

        // Bu ayarların nerede geçerli olacağını söyleyen kaynak nesnesi
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Bütün yollar ("/**") için bu konfigurasyonu uygula
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}