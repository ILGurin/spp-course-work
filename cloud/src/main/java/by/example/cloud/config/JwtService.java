package by.example.cloud.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtService {
    private final long JWT_EXPIRATION_TIME = 1000 * 60 * 30; //30m
    private final long REFRESH_EXPIRATION_TIME = 1000 * 60 * 30;

    private final String SECRET_KEY = "";
    private final String SECRET_REFRESH_KEY = "";

    private final Key jwtAccessSecret;
    private final Key jwtRefreshSecret;


    public JwtService(
            @Value("${jwt.secret.access}") String jwtAccessString,
            @Value("${jwt.secret.refresh}") String jwtRefreshString
    ) {
        jwtAccessSecret = getSigninKey(jwtAccessString);
        jwtRefreshSecret = getSigninKey(jwtRefreshString);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // Генерация ключа из строки
        SecretKey secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        // Создаём парсер с ключом для проверки подписи
        JwtParser jwtParser = Jwts.parser().verifyWith(secretKey).build();

        // Парсим и получаем Claims из JWS
        Jwt<?, Claims> jwt = jwtParser.parseSignedClaims(token);
        return jwt.getPayload();
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, TokenType tokenType) {
        return buildToken(
                extraClaims,
                userDetails,
                tokenType == TokenType.ACCESS ? JWT_EXPIRATION_TIME : REFRESH_EXPIRATION_TIME,
                tokenType == TokenType.ACCESS ? jwtAccessSecret : jwtRefreshSecret
        );
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationTime, Key sign) {
        return Jwts
                    .builder()
                    .claims()
                        .subject(userDetails.getUsername())
                        .issuedAt(new Date(System.currentTimeMillis()))
                        .expiration(new Date(System.currentTimeMillis() + expirationTime))
                        .and()
                    .claims(extraClaims)
                    .signWith(sign)
                    .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));

    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Key getSigninKey(String sign) {
        byte[] keyBytes = Decoders.BASE64.decode(sign);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
