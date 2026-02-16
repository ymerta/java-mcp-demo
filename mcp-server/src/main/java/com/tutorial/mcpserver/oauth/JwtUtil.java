package com.tutorial.mcpserver.oauth;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.*;

/**
 * JWT utility: generates and validates access tokens using RSA256.
 * Holds an in-memory RSA key pair (PoC - regenerated on each restart).
 */
@Component
public class JwtUtil {

    private final RSAKey rsaKey;
    private final JWSSigner signer;
    private final JWSVerifier verifier;

    public JwtUtil() {
        try {
            this.rsaKey = new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
            this.signer = new RSASSASigner(rsaKey);
            this.verifier = new RSASSAVerifier(rsaKey.toPublicJWK());
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Generate JWT access token.
     */
    public String generateAccessToken(String issuer, String subject, String audience,
                                       String scope, String clientId, long expiresInSeconds) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .audience(audience)
                    .claim("scope", scope)
                    .claim("client_id", clientId)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expiresInSeconds)))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(rsaKey.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    /**
     * Validate JWT and return claims. Throws if invalid.
     */
    public JWTClaimsSet validateToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        if (!signedJWT.verify(verifier)) {
            throw new JOSEException("Invalid JWT signature");
        }
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        if (claims.getExpirationTime().before(new Date())) {
            throw new JOSEException("Token expired");
        }
        return claims;
    }

    /**
     * Return JWKS (public key) for token verification.
     */
    public Map<String, Object> getJwks() {
        JWKSet jwkSet = new JWKSet(rsaKey.toPublicJWK());
        return jwkSet.toJSONObject();
    }
}
