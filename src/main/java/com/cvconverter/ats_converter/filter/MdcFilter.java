package com.cvconverter.ats_converter.filter; // KENDİ PAKET YOLUNA GÖRE DÜZELT

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Her istek için benzersiz bir ID oluştur veya header'dan al
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        // MDC'ye bu ID'yi koy. Bu andan itibaren, bu thread'de atılan BÜTÜN loglarda bu ID olacak.
        MDC.put("requestId", requestId);

        try {
            // İsteğin, zincirdeki bir sonraki filtreye veya Controller'a devam etmesini sağla
            filterChain.doFilter(request, response);
        } finally {
            // İstek bittiğinde, bu thread başka bir istek için kullanılmadan önce MDC'yi temizle. BU ÇOK ÖNEMLİ!
            MDC.clear();
        }
    }
}