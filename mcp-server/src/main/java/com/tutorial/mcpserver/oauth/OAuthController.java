package com.tutorial.mcpserver.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * OAuth 2.1 Authorization Server endpoints for MCP.
 *
 * Implements:
 * - RFC 8414: Authorization Server Metadata
 * - RFC 7591: Dynamic Client Registration
 * - RFC 9728: Protected Resource Metadata
 * - OAuth 2.1: Authorization Code with PKCE
 */
@Controller
public class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    private final JwtUtil jwtUtil;

    @Value("${oauth2.issuer}")
    private String issuer;

    @Value("${oauth2.access-token-expiry:3600}")
    private long accessTokenExpiry;

    public OAuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // =============================================
    // RFC 9728 - Protected Resource Metadata
    // =============================================
    @GetMapping({"/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/mcp"})
    @ResponseBody
    public Map<String, Object> protectedResourceMetadata() {
        return Map.of(
                "resource", issuer,
                "authorization_servers", List.of(issuer),
                "scopes_supported", List.of("mcp:tools"),
                "bearer_methods_supported", List.of("header"),
                "resource_name", "MCP Tutorial Server"
        );
    }

    // =============================================
    // RFC 8414 - Authorization Server Metadata
    // =============================================
    @GetMapping({"/.well-known/oauth-authorization-server", "/.well-known/oauth-authorization-server/mcp"})
    @ResponseBody
    public Map<String, Object> authorizationServerMetadata() {
        return Map.of(
                "issuer", issuer,
                "authorization_endpoint", issuer + "/oauth2/authorize",
                "token_endpoint", issuer + "/oauth2/token",
                "registration_endpoint", issuer + "/oauth2/register",
                "jwks_uri", issuer + "/oauth2/jwks",
                "scopes_supported", List.of("mcp:tools"),
                "response_types_supported", List.of("code"),
                "grant_types_supported", List.of("authorization_code", "refresh_token"),
                "token_endpoint_auth_methods_supported", List.of("none"),
                "code_challenge_methods_supported", List.of("S256")
        );
    }

    // =============================================
    // JWKS endpoint - public keys for token verification
    // =============================================
    @GetMapping("/oauth2/jwks")
    @ResponseBody
    public Map<String, Object> jwks() {
        return jwtUtil.getJwks();
    }

    // =============================================
    // RFC 7591 - Dynamic Client Registration
    // =============================================
    @PostMapping("/oauth2/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerClient(@RequestBody Map<String, Object> body) {
        String clientId = UUID.randomUUID().toString();
        String clientName = (String) body.getOrDefault("client_name", "Unknown Client");

        @SuppressWarnings("unchecked")
        List<String> redirectUris = (List<String>) body.getOrDefault("redirect_uris", List.of());
        @SuppressWarnings("unchecked")
        List<String> grantTypes = (List<String>) body.getOrDefault("grant_types",
                List.of("authorization_code", "refresh_token"));
        @SuppressWarnings("unchecked")
        List<String> responseTypes = (List<String>) body.getOrDefault("response_types", List.of("code"));

        OAuthStore.RegisteredClient client = new OAuthStore.RegisteredClient(
                clientId, clientName, redirectUris, grantTypes, responseTypes, Instant.now());
        OAuthStore.saveClient(client);

        log.info("Dynamic client registered: {} ({})", clientName, clientId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client_id", clientId);
        response.put("client_name", clientName);
        response.put("redirect_uris", redirectUris);
        response.put("grant_types", grantTypes);
        response.put("response_types", responseTypes);
        response.put("token_endpoint_auth_method", "none");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =============================================
    // Authorization Endpoint (GET) - shows login/consent page
    // =============================================
    @GetMapping("/oauth2/authorize")
    public String authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "scope", required = false, defaultValue = "mcp:tools") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            Authentication authentication,
            Model model,
            HttpServletRequest request) {

        // Validate response_type
        if (!"code".equals(responseType)) {
            return redirectWithError(redirectUri, state, "unsupported_response_type",
                    "Only 'code' response type is supported");
        }

        // Validate client
        OAuthStore.RegisteredClient client = OAuthStore.getClient(clientId);
        if (client == null) {
            model.addAttribute("error", "Unknown client: " + clientId);
            return "login";
        }

        // Validate redirect_uri
        if (redirectUri != null && !client.redirectUris().isEmpty()
                && !client.redirectUris().contains(redirectUri)) {
            model.addAttribute("error", "Invalid redirect_uri");
            return "login";
        }

        // If user is already authenticated, issue code directly
        if (authentication != null && authentication.isAuthenticated()) {
            return issueAuthorizationCode(authentication.getName(), clientId,
                    redirectUri, scope, state, codeChallenge, codeChallengeMethod);
        }

        // Store OAuth params in session for post-login redirect
        request.getSession().setAttribute("oauth_client_id", clientId);
        request.getSession().setAttribute("oauth_redirect_uri", redirectUri);
        request.getSession().setAttribute("oauth_scope", scope);
        request.getSession().setAttribute("oauth_state", state);
        request.getSession().setAttribute("oauth_code_challenge", codeChallenge);
        request.getSession().setAttribute("oauth_code_challenge_method", codeChallengeMethod);

        // Show login page
        model.addAttribute("clientName", client.clientName());
        model.addAttribute("scope", scope);
        return "login";
    }

    // =============================================
    // Token Endpoint (POST) - exchanges code for tokens
    // =============================================
    @PostMapping("/oauth2/token")
    @ResponseBody
    public ResponseEntity<?> token(@RequestParam Map<String, String> params) {
        String grantType = params.get("grant_type");

        if ("authorization_code".equals(grantType)) {
            return handleAuthorizationCodeGrant(params);
        } else if ("refresh_token".equals(grantType)) {
            return handleRefreshTokenGrant(params);
        }

        return errorResponse(HttpStatus.BAD_REQUEST, "unsupported_grant_type",
                "Supported grant types: authorization_code, refresh_token");
    }

    private ResponseEntity<?> handleAuthorizationCodeGrant(Map<String, String> params) {
        String code = params.get("code");
        String clientId = params.get("client_id");
        String redirectUri = params.get("redirect_uri");
        String codeVerifier = params.get("code_verifier");

        if (code == null || clientId == null) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_request", "Missing code or client_id");
        }

        // Consume the authorization code (single-use)
        OAuthStore.AuthCode authCode = OAuthStore.consumeAuthCode(code);
        if (authCode == null) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Invalid or expired authorization code");
        }

        // Verify code hasn't expired (10 min)
        if (authCode.createdAt().plusSeconds(600).isBefore(Instant.now())) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Authorization code expired");
        }

        // Verify client_id matches
        if (!authCode.clientId().equals(clientId)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Client ID mismatch");
        }

        // Verify redirect_uri matches
        if (authCode.redirectUri() != null && !authCode.redirectUri().equals(redirectUri)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Redirect URI mismatch");
        }

        // Verify PKCE code_verifier
        if (authCode.codeChallenge() != null) {
            if (codeVerifier == null) {
                return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Missing code_verifier");
            }
            if (!verifyPkce(codeVerifier, authCode.codeChallenge(), authCode.codeChallengeMethod())) {
                return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Invalid code_verifier");
            }
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
                issuer, authCode.subject(), issuer, authCode.scope(), clientId, accessTokenExpiry);

        String refreshToken = UUID.randomUUID().toString();
        OAuthStore.saveRefreshToken(new OAuthStore.RefreshTokenEntry(
                refreshToken, clientId, authCode.subject(), authCode.scope(), Instant.now()));

        log.info("Token issued for user: {}, client: {}", authCode.subject(), clientId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", accessTokenExpiry);
        response.put("scope", authCode.scope());
        response.put("refresh_token", refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    private ResponseEntity<?> handleRefreshTokenGrant(Map<String, String> params) {
        String refreshToken = params.get("refresh_token");
        String clientId = params.get("client_id");

        if (refreshToken == null || clientId == null) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_request", "Missing refresh_token or client_id");
        }

        OAuthStore.RefreshTokenEntry entry = OAuthStore.consumeRefreshToken(refreshToken);
        if (entry == null) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Invalid refresh token");
        }

        if (!entry.clientId().equals(clientId)) {
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid_grant", "Client ID mismatch");
        }

        String accessToken = jwtUtil.generateAccessToken(
                issuer, entry.subject(), issuer, entry.scope(), clientId, accessTokenExpiry);

        String newRefreshToken = UUID.randomUUID().toString();
        OAuthStore.saveRefreshToken(new OAuthStore.RefreshTokenEntry(
                newRefreshToken, clientId, entry.subject(), entry.scope(), Instant.now()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", accessTokenExpiry);
        response.put("scope", entry.scope());
        response.put("refresh_token", newRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    // =============================================
    // OAuth2 callback after successful login
    // =============================================
    @GetMapping("/oauth2/callback")
    public String oauthCallback(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String clientId = (String) request.getSession().getAttribute("oauth_client_id");
        String redirectUri = (String) request.getSession().getAttribute("oauth_redirect_uri");
        String scope = (String) request.getSession().getAttribute("oauth_scope");
        String state = (String) request.getSession().getAttribute("oauth_state");
        String codeChallenge = (String) request.getSession().getAttribute("oauth_code_challenge");
        String codeChallengeMethod = (String) request.getSession().getAttribute("oauth_code_challenge_method");

        // Clean session
        request.getSession().removeAttribute("oauth_client_id");
        request.getSession().removeAttribute("oauth_redirect_uri");
        request.getSession().removeAttribute("oauth_scope");
        request.getSession().removeAttribute("oauth_state");
        request.getSession().removeAttribute("oauth_code_challenge");
        request.getSession().removeAttribute("oauth_code_challenge_method");

        if (clientId == null) {
            return "redirect:/login?error=no_oauth_session";
        }

        return issueAuthorizationCode(authentication.getName(), clientId,
                redirectUri, scope, state, codeChallenge, codeChallengeMethod);
    }

    // =============================================
    // Helper methods
    // =============================================

    private String issueAuthorizationCode(String subject, String clientId, String redirectUri,
                                           String scope, String state,
                                           String codeChallenge, String codeChallengeMethod) {
        String code = UUID.randomUUID().toString();

        OAuthStore.saveAuthCode(new OAuthStore.AuthCode(
                code, clientId, redirectUri, scope,
                codeChallenge, codeChallengeMethod, subject, Instant.now()));

        log.info("Authorization code issued for user: {}, client: {}", subject, clientId);

        StringBuilder redirect = new StringBuilder();
        if (redirectUri != null) {
            redirect.append(redirectUri);
        } else {
            // Fallback - shouldn't happen with proper clients
            redirect.append(issuer).append("/oauth2/callback");
        }
        redirect.append(redirectUri != null && redirectUri.contains("?") ? "&" : "?");
        redirect.append("code=").append(code);
        if (state != null) {
            redirect.append("&state=").append(state);
        }

        return "redirect:" + redirect;
    }

    private boolean verifyPkce(String codeVerifier, String codeChallenge, String method) {
        if ("S256".equalsIgnoreCase(method) || method == null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
                return computed.equals(codeChallenge);
            } catch (Exception e) {
                return false;
            }
        } else if ("plain".equalsIgnoreCase(method)) {
            return codeVerifier.equals(codeChallenge);
        }
        return false;
    }

    private String redirectWithError(String redirectUri, String state, String error, String description) {
        if (redirectUri == null) {
            return "redirect:/login?error=" + error;
        }
        StringBuilder sb = new StringBuilder("redirect:").append(redirectUri);
        sb.append(redirectUri.contains("?") ? "&" : "?");
        sb.append("error=").append(error);
        sb.append("&error_description=").append(description);
        if (state != null) {
            sb.append("&state=").append(state);
        }
        return sb.toString();
    }

    private ResponseEntity<Map<String, String>> errorResponse(HttpStatus status, String error, String description) {
        return ResponseEntity.status(status).body(Map.of("error", error, "error_description", description));
    }
}
