package com.schoolmanagement.common.filter;

import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    // Skip JWT validation on public endpoints (auth, health, docs)
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/",
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs",
            "/swagger-ui/",
            "/swagger-ui.html"
    );

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        return PUBLIC_PATH_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                chain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            Claims claims = jwtUtil.validateAndExtractClaims(token);

            UUID userId = UUID.fromString(claims.getSubject());
            String schoolId = claims.get("schoolId", String.class);

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(claims);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Set tenant context for this request thread
            if (schoolId != null) {
                TenantContext.setTenantId(UUID.fromString(schoolId));
            }

            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT filter error: {}", e.getMessage());
            // Don't throw — let Spring Security handle 401
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // ALWAYS clear after request
        }
    }
}
