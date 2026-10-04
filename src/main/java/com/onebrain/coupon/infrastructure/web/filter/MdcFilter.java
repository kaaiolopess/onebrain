package com.onebrain.coupon.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Preenche o MDC de cada requisição para que todos os logs dela saiam com o mesmo correlationId.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MdcFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID = "correlationId";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String PATH = "path";
    public static final String COUPON_ID = "couponId";

    // evita que um header malicioso injete conteúdo arbitrário nos logs
    private static final Pattern CORRELATION_ID_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/coupon");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        String correlationId = resolverCorrelationId(request);

        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(HTTP_METHOD, request.getMethod());
        MDC.put(PATH, request.getRequestURI());
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("m=doFilterInternal, stg=END, msg=Requisição finalizada: {} {}, status={}, duracaoMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(),
                    System.currentTimeMillis() - inicio);
            MDC.clear();
        }
    }

    private String resolverCorrelationId(HttpServletRequest request) {
        String recebido = request.getHeader(CORRELATION_ID_HEADER);
        if (recebido != null && CORRELATION_ID_VALIDO.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }
}
