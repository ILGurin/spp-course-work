package by.cloud.api.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import by.cloud.api.service.TokenBlacklistService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistFilter implements WebFilter {
    
    private final TokenBlacklistService tokenBlacklistService;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        return ReactiveSecurityContextHolder.getContext()
                .cast(org.springframework.security.core.context.SecurityContext.class)
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .cast(Authentication.class)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuth -> {
                    Jwt jwt = jwtAuth.getToken();
                    String tokenId = jwt.getId(); // jti claim
                    
                    if (tokenId == null || tokenId.isEmpty()) {
                        // Если токен не имеет jti, пропускаем (может быть старый формат)
                        log.warn("Token without jti claim detected");
                        return chain.filter(exchange);
                    }
                    
                    return tokenBlacklistService.isBlacklisted(tokenId)
                            .flatMap(isBlacklisted -> {
                                if (isBlacklisted) {
                                    log.warn("Blocked request with blacklisted token: {}", tokenId);
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete();
                                }
                                return chain.filter(exchange);
                            });
                })
                .switchIfEmpty(chain.filter(exchange)); // Если нет аутентификации, пропускаем (SecurityConfig обработает)
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/v1/auth/registration") ||
               path.equals("/v1/auth/login") ||
               path.equals("/v1/auth/refresh-token");
        // /v1/auth/logout НЕ должен быть публичным - требуется аутентификация для получения токена
    }
}

