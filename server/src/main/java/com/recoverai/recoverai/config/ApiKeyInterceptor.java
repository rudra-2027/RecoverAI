package com.recoverai.recoverai.config;

import com.recoverai.recoverai.service.RuntimeSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInterceptor implements HandlerInterceptor {
    private static final String HEADER = "X-API-Key";

    private final RuntimeSettingsService runtimeSettingsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String configuredKey = runtimeSettingsService.apiKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            log.debug("API key validation skipped because no runtime API key is configured");
            return true;
        }
        String providedKey = request.getHeader(HEADER);
        if (configuredKey.equals(providedKey)) {
            return true;
        }
        log.warn("Unauthorized request rejected for method={}, uri={}", request.getMethod(), request.getRequestURI());
        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid API key");
        return false;
    }
}
