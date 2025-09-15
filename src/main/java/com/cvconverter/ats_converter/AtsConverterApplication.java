package com.cvconverter.ats_converter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

// BU SATIR ÇOK ÖNEMLİ!
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableAsync
public class AtsConverterApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtsConverterApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}