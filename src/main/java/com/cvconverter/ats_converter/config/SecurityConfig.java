package com.cvconverter.ats_converter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed.origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS Ayarları: Frontend'den gelen isteklere izin ver.
                // withDefaults() yerine kendi özel yapılandırmamızı kullanıyoruz.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. CSRF Koruması: REST API'ler için genellikle devre dışı bırakılır.
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Yetkilendirme Kuralları: Hangi yollara kimin erişebileceğini tanımla.
                .authorizeHttpRequests(auth -> auth
                        // Bizim API endpoint'lerimizin bulunduğu "/api/**" yoluna gelen
                        // TÜM isteklere kimlik doğrulaması olmadan izin ver.
                        .requestMatchers("/api/**").permitAll()

                        // Yukarıdaki kural dışında kalan diğer tüm isteklere
                        // (örneğin Spring'in kendi /error endpoint'i veya gelecekte eklenecek başka yollar)
                        // yine de izin ver. Bu, "varsayılan olarak her şeyi engelle" mantığını kaldırır
                        // ve asenkron hata yönetimindeki çakışmaları önler.
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // application.properties'den gelen frontend adreslerine izin ver.
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // Gerekli HTTP metotlarına izin ver.
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Tüm başlıklara izin ver.
        configuration.setAllowedHeaders(List.of("*"));

        // Kimlik bilgileriyle isteklere izin ver.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Tanımlanan bu CORS kurallarını projedeki tüm yollar için geçerli kıl.
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}