package com.tutorial.mcpserver.config;

import com.tutorial.mcpserver.repository.UserRepository;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Filter chain 1: Authorization Server endpoints
     * OAuth2 token, authorize, jwks, dynamic client registration, well-known metadata
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/.well-known/**", "/login", "/login/**", "/error")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .with(McpAuthorizationServerConfigurer.mcpAuthorizationServer(), configurer -> {})
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Filter chain 2: MCP Resource Server endpoints (JWT-protected /mcp/**)
     * Lazy JwtDecoder — issuer'a ilk request geldiginde baglanir, startup'ta degil.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/mcp", "/mcp/**")
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(lazyJwtDecoder()))
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Lazy JwtDecoder: startup'ta issuer'a baglanmaz.
     * Ilk JWT dogrulama istegi geldiginde JWKS endpoint'ini kesfeder ve cache'ler.
     * Ayni sunucuda Auth Server + Resource Server oldugu icin bu gerekli.
     */
    private JwtDecoder lazyJwtDecoder() {
        return new JwtDecoder() {
            private volatile JwtDecoder delegate;

            @Override
            public org.springframework.security.oauth2.jwt.Jwt decode(String token) {
                if (delegate == null) {
                    synchronized (this) {
                        if (delegate == null) {
                            delegate = JwtDecoders.fromIssuerLocation(issuerUri);
                        }
                    }
                }
                return delegate.decode(token);
            }
        };
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            var user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            return User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .roles("USER")
                    .build();
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Mcp-Session-Id", "WWW-Authenticate"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
