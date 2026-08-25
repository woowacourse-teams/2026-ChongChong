package withoutc.chongchong.auth.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;
import withoutc.chongchong.auth.security.AuthenticatedUserJwtAuthenticationConverter;
import withoutc.chongchong.auth.security.RestAccessDeniedHandler;
import withoutc.chongchong.auth.security.RestAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final RequestMatcher WEB_AUTH_CSRF_REQUEST_MATCHER = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/login"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/refresh"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/auth/logout")
    );

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            AuthenticatedUserJwtAuthenticationConverter jwtAuthenticationConverter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .requireCsrfProtectionMatcher(WEB_AUTH_CSRF_REQUEST_MATCHER)
                )
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout",
                                "/auth/csrf",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${frontend.base-url}") String frontendBaseUrl
    ) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowedOrigins(List.of(frontendBaseUrl));
        corsConfiguration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        corsConfiguration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "X-XSRF-TOKEN")
        );
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository(WebRefreshCookieProperties refreshCookieProperties) {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieName(CSRF_COOKIE_NAME);
        repository.setHeaderName(CSRF_HEADER_NAME);
        repository.setCookieCustomizer(cookie -> cookie
                .path(refreshCookieProperties.path())
                .secure(refreshCookieProperties.secure())
                .httpOnly(true)
                .sameSite(refreshCookieProperties.sameSite())
        );
        return repository;
    }

    @Bean
    AuthenticatedUserJwtAuthenticationConverter jwtAuthenticationConverter() {
        return new AuthenticatedUserJwtAuthenticationConverter();
    }

    @Bean
    RestAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    RestAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }
}
