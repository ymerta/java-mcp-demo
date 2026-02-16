package com.tutorial.mcpserver.oauth;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.DispatcherType;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter that validates Bearer tokens on the MCP endpoint.
 * Returns 401 with WWW-Authenticate header pointing to Protected Resource Metadata
 * when no valid token is present (per MCP spec).
 */
@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);

    private final JwtUtil jwtUtil;

    @Value("${oauth2.issuer}")
    private String issuer;

    public BearerTokenFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token - return 401 with resource_metadata per MCP spec
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate",
                    "Bearer realm=\"mcp\", resource_metadata=\"" + issuer + "/.well-known/oauth-protected-resource\"");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"unauthorized\",\"error_description\":\"Bearer token required\"}");
            return;
        }

        String token = authHeader.substring(7);

        try {
            JWTClaimsSet claims = jwtUtil.validateToken(token);

            // Verify audience matches our server
            List<String> audiences = claims.getAudience();
            String normalizedIssuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
            boolean audienceValid = audiences != null && audiences.stream()
                    .anyMatch(aud -> {
                        String normalizedAud = aud.endsWith("/") ? aud.substring(0, aud.length() - 1) : aud;
                        return normalizedAud.equals(normalizedIssuer);
                    });

            if (!audienceValid) {
                log.warn("Token audience mismatch. Expected: {}, Got: {}", issuer, audiences);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"invalid_token\",\"error_description\":\"Invalid audience\"}");
                return;
            }

            // Set authentication context
            String subject = claims.getSubject();
            String scope = (String) claims.getClaim("scope");
            List<SimpleGrantedAuthority> authorities = scope != null
                    ? Arrays.stream(scope.split(" "))
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                    .toList()
                    : List.of();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(subject, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("Token validated for user: {}, scopes: {}", subject, scope);

        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_token\",\"error_description\":\"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip async dispatches - these are SSE continuations where auth context is already set
        if (request.getDispatcherType() == DispatcherType.ASYNC
                || request.getDispatcherType() == DispatcherType.ERROR) {
            return true;
        }

        String path = request.getRequestURI();
        // Don't filter OAuth endpoints, metadata, login, static resources
        return path.startsWith("/oauth2/")
                || path.startsWith("/.well-known/")
                || path.equals("/login")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/error");
    }
}
