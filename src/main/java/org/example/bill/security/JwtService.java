package org.example.bill.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.example.bill.domain.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String createToken(AppUser user) {
        var authorities =
                user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(p -> "PERM_" + p.getCode())
                        .collect(Collectors.toSet());
        authorities.addAll(
                user.getRoles().stream().map(r -> "ROLE_" + r.getCode()).collect(Collectors.toSet()));

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("authorities", String.join(",", authorities))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
