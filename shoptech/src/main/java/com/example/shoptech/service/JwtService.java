package com.example.shoptech.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long validityMinutes;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${shoptech.security.jwt.validity-minutes:20}") long validityMinutes) {
        if (validityMinutes < 1 || validityMinutes > 1440) {
            throw new IllegalArgumentException("JWT validity must be between 1 and 1440 minutes");
        }
        this.jwtEncoder = jwtEncoder;
        this.validityMinutes = validityMinutes;
    }

    public String createToken(String username) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shoptech")
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(validityMinutes, ChronoUnit.MINUTES))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
    }

    public long getValiditySeconds() {
        return validityMinutes * 60;
    }
}
