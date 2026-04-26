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

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.validateAndExtractClaims(token);

            UUID   userId   = UUID.fromString(claims.getSubject());
            String schoolId = claims.get("schoolId", String.class);

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();

            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            auth.setDetails(claims);

            SecurityContextHolder.getContext().setAuthentication(auth);

            // Set tenant context for this request thread
            if (schoolId != null) {
                TenantContext.setTenantId(UUID.fromString(schoolId));
            }

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT filter error: {}", e.getMessage());
            // Don't throw — let Spring Security handle 401
        } finally {
            chain.doFilter(request, response);
            TenantContext.clear(); // ALWAYS clear after request
        }
    }
}