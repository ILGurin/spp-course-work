package by.cloud.api.rest;

import lombok.RequiredArgsConstructor;
import by.cloud.api.service.TokenBlacklistService;
import by.cloud.api.service.TokenService;
import by.cloud.api.service.UserService;
import by.cloud.individual.dto.IndividualWriteDto;
import by.cloud.individual.dto.TokenRefreshRequest;
import by.cloud.individual.dto.TokenResponse;
import by.cloud.individual.dto.UserInfoResponse;
import by.cloud.individual.dto.UserLoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/v1/auth")
@CrossOrigin
public class AuthRestControllerV1 {

    private final UserService userService;
    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @GetMapping("/me")
    public Mono<ResponseEntity<UserInfoResponse>> getMe() {
        return userService.getUserInfo().map(ResponseEntity::ok);
    }

    @PostMapping(value = "/login")
    public Mono<ResponseEntity<TokenResponse>> login(@Valid @RequestBody Mono<UserLoginRequest> body) {
        return body.flatMap(tokenService::login).map(ResponseEntity::ok);
    }

    @PostMapping(value = "/refresh-token")
    public Mono<ResponseEntity<TokenResponse>> refreshToken(@Valid @RequestBody Mono<TokenRefreshRequest> body) {
        return body.flatMap(tokenService::refreshToken).map(ResponseEntity::ok);
    }

    @PostMapping(value = "/registration")
    public Mono<ResponseEntity<TokenResponse>> registration(@Valid @RequestBody Mono<IndividualWriteDto> body) {
        return body.flatMap(userService::register)
                .map(t -> ResponseEntity.status(HttpStatus.CREATED).body(t));
    }

    @PostMapping(value = "/logout")
    public Mono<ResponseEntity<Void>> logout() {
        System.out.println("=== LOGOUT REQUEST RECEIVED ===");
        
        return ReactiveSecurityContextHolder.getContext()
                .doOnNext(ctx -> System.out.println("Security context found: " + (ctx != null)))
                .cast(org.springframework.security.core.context.SecurityContext.class)
                .doOnNext(ctx -> System.out.println("Security context cast successful"))
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .doOnNext(auth -> System.out.println("Authentication extracted: " + (auth != null) + ", type: " + (auth != null ? auth.getClass().getName() : "null")))
                .cast(Authentication.class)
                .filter(auth -> {
                    boolean isJwt = auth instanceof JwtAuthenticationToken;
                    System.out.println("Is JWT token: " + isJwt);
                    return isJwt;
                })
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuth -> {
                    System.out.println("Processing JWT authentication");
                    Jwt jwt = jwtAuth.getToken();
                    System.out.println("JWT extracted. Claims: " + jwt.getClaims().keySet());
                    
                    // Пробуем получить jti разными способами
                    String tokenId = jwt.getId(); // jti claim
                    System.out.println("jti from getId(): " + tokenId);
                    
                    // Если jti нет, пробуем получить из claims
                    if (tokenId == null || tokenId.isEmpty()) {
                        tokenId = jwt.getClaimAsString("jti");
                        System.out.println("jti from getClaimAsString(): " + tokenId);
                    }
                    
                    // Если все еще нет, используем комбинацию sub + iat + exp как уникальный идентификатор
                    if (tokenId == null || tokenId.isEmpty()) {
                        String sub = jwt.getSubject();
                        Long iat = jwt.getIssuedAt() != null ? jwt.getIssuedAt().getEpochSecond() : null;
                        Long exp = jwt.getExpiresAt() != null ? jwt.getExpiresAt().getEpochSecond() : null;
                        System.out.println("Fallback: sub=" + sub + ", iat=" + iat + ", exp=" + exp);
                        if (sub != null && iat != null && exp != null) {
                            tokenId = sub + ":" + iat + ":" + exp;
                            System.out.println("Generated tokenId from fallback: " + tokenId);
                        }
                    }
                    
                    // Создаем финальную копию для использования в лямбде
                    final String finalTokenId = tokenId;
                    
                    if (finalTokenId != null && !finalTokenId.isEmpty()) {
                        System.out.println("Adding token to blacklist: " + finalTokenId);
                        return tokenBlacklistService.addToBlacklist(finalTokenId)
                                .doOnSuccess(result -> {
                                    if (result) {
                                        System.out.println("✓ Token successfully added to blacklist: " + finalTokenId);
                                    } else {
                                        System.err.println("✗ Failed to add token to blacklist (result=false): " + finalTokenId);
                                    }
                                })
                                .doOnError(error -> {
                                    System.err.println("✗ Error adding token to blacklist: " + error.getMessage());
                                    error.printStackTrace();
                                })
                                .then(Mono.just(ResponseEntity.ok().<Void>build()));
                    }
                    
                    System.err.println("✗ Cannot extract token ID from JWT. Claims: " + jwt.getClaims().keySet());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<Void>build());
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    System.err.println("✗ No authentication found in security context - request may not be authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).<Void>build();
                }));
    }
}