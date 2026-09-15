package com.example.lab.auth.infra.security;

import com.example.lab.global.web.ApiErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtProperties properties;
    private final JwtParser jwtParser;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtProperties properties, JwtParser jwtParser, ObjectMapper objectMapper) {
        this.properties = properties;
        this.jwtParser = jwtParser;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String accessToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (properties.accessTokenCookieName().equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }

        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var signedClaims = jwtParser.parseSignedClaims(accessToken);
            if (!Jwts.SIG.HS256.getId().equals(signedClaims.getHeader().getAlgorithm())) {
                throw new JwtException("signing algorithm must be HS256");
            }

            var claims = signedClaims.getPayload();
            String accountId = claims.getSubject();
            String role = claims.get("role", String.class);
            String tokenType = claims.get("token_type", String.class);

            if (accountId == null || accountId.isBlank()) {
                throw new JwtException("subject claim is required");
            }
            if (role == null || role.isBlank()) {
                throw new JwtException("role claim is required");
            }
            if (!"access".equals(tokenType)) {
                throw new JwtException("token_type claim must be access");
            }
            if (claims.getId() == null || claims.getId().isBlank()) {
                throw new JwtException("jti claim is required");
            }
            if (claims.getIssuedAt() == null) {
                throw new JwtException("iat claim is required");
            }
            if (claims.getNotBefore() == null) {
                throw new JwtException("nbf claim is required");
            }
            if (claims.getExpiration() == null) {
                throw new JwtException("exp claim is required");
            }

            Long.parseLong(accountId);
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(accountId, null, List.of(authority));
            var securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
        } catch (ExpiredJwtException exception) {
            SecurityContextHolder.clearContext();
            log.debug("access_token_expired");
            var errorCode = SecurityErrorCode.INVALID_ACCESS_TOKEN;
            response.setStatus(errorCode.status());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(errorCode));
            return;
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            log.debug("access_token_invalid reason={}", exception.getClass().getSimpleName());
            var errorCode = SecurityErrorCode.INVALID_ACCESS_TOKEN;
            response.setStatus(errorCode.status());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(errorCode));
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
