package com.tutorial.mcpserver.oauth;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for OAuth2 artifacts: registered clients, authorization codes, tokens.
 * For PoC only - production should use a persistent store.
 */
public class OAuthStore {

    // --- Registered OAuth2 Clients (Dynamic Client Registration) ---
    public record RegisteredClient(
            String clientId,
            String clientName,
            List<String> redirectUris,
            List<String> grantTypes,
            List<String> responseTypes,
            Instant createdAt
    ) {}

    private static final Map<String, RegisteredClient> clients = new ConcurrentHashMap<>();

    public static void saveClient(RegisteredClient client) {
        clients.put(client.clientId(), client);
    }

    public static RegisteredClient getClient(String clientId) {
        return clients.get(clientId);
    }

    // --- Authorization Codes ---
    public record AuthCode(
            String code,
            String clientId,
            String redirectUri,
            String scope,
            String codeChallenge,
            String codeChallengeMethod,
            String subject, // authenticated user email
            Instant createdAt
    ) {}

    private static final Map<String, AuthCode> authCodes = new ConcurrentHashMap<>();

    public static void saveAuthCode(AuthCode authCode) {
        authCodes.put(authCode.code(), authCode);
    }

    public static AuthCode consumeAuthCode(String code) {
        return authCodes.remove(code);
    }

    // --- Refresh Tokens ---
    public record RefreshTokenEntry(
            String refreshToken,
            String clientId,
            String subject,
            String scope,
            Instant createdAt
    ) {}

    private static final Map<String, RefreshTokenEntry> refreshTokens = new ConcurrentHashMap<>();

    public static void saveRefreshToken(RefreshTokenEntry entry) {
        refreshTokens.put(entry.refreshToken(), entry);
    }

    public static RefreshTokenEntry consumeRefreshToken(String token) {
        return refreshTokens.remove(token);
    }
}
