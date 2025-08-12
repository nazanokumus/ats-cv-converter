package com.cvconverter.ats_converter.config; // Kendi paket adınla değiştir!

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. ADIM: CSRF korumasını devre dışı bırak.
                // Modern, stateless REST API'lar için bu yaygın bir pratiktir.
                .csrf(csrf -> csrf.disable())

                // 2. ADIM: İstekler için yetkilendirme kurallarını belirle.
                .authorizeHttpRequests(auth -> auth
                        // "/api/**" ile başlayan TÜM yollara...
                        .requestMatchers("/api/**")
                        // ... sorgusuz sualsiz İZİN VER.
                        .permitAll()
                        // ... geri kalan TÜM diğer isteklere ise...
                        .anyRequest()
                        // ... kimlik doğrulaması (authentication) zorunlu olsun.
                        .authenticated()
                );

        return http.build();
    }
}