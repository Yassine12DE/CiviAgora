package tn.esprit.tic.civiAgora.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.tic.civiAgora.dao.repository.TokenRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/auth/register")
                || path.startsWith("/auth/verifyMail/")
                || path.startsWith("/auth/forgot-password/")
                || path.equals("/auth/reset-password");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (requiresResolvedTenant(request) && TenantContext.getResolvedOrganizationId() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant could not be resolved for this request");
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (request.getHeader("Upgrade") != null &&
                "websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            authHeader = request.getParameter("Authorization");
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String userEmail;
        Integer tokenOrganizationId;
        String tokenOrganizationSlug;

        try {
            userEmail = jwtService.extractUsername(jwt);
            tokenOrganizationId = jwtService.extractOrganizationId(jwt);
            tokenOrganizationSlug = jwtService.extractOrganizationSlug(jwt);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (userEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(userEmail);

            boolean isTokenValidInDb = tokenRepository.findByToken(jwt)
                    .map(t -> !t.isExpired() && !t.isRevoked())
                    .orElse(false);

            if (jwtService.isTokenValid(jwt, userDetails) && isTokenValidInDb) {
                TenantContext.setAuthenticatedOrganization(tokenOrganizationId, tokenOrganizationSlug);
                boolean isSuperAdmin = userDetails.getAuthorities()
                        .stream()
                        .anyMatch(authority -> "SUPER_ADMIN".equals(authority.getAuthority()));

                if (requiresStrictTenantMatch(request) && !isSuperAdmin) {
                    Integer resolvedOrganizationId = TenantContext.getResolvedOrganizationId();
                    String resolvedOrganizationSlug = TenantContext.getResolvedOrganizationSlug();

                    if (resolvedOrganizationId == null || resolvedOrganizationSlug == null || resolvedOrganizationSlug.isBlank()) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant could not be resolved for this request");
                        return;
                    }

                    if (tokenOrganizationId == null
                            || tokenOrganizationSlug == null
                            || !resolvedOrganizationId.equals(tokenOrganizationId)
                            || !resolvedOrganizationSlug.equalsIgnoreCase(tokenOrganizationSlug)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant mismatch");
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresResolvedTenant(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/auth/login")
                || path.equals("/auth/me")
                || path.equals("/auth/refresh-token")
                || path.startsWith("/modules/")
                || path.startsWith("/org/")
                || isCurrentTenantPublicOrganizationEndpoint(path);
    }

    private boolean requiresStrictTenantMatch(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/auth/me")
                || path.equals("/auth/refresh-token")
                || path.startsWith("/modules/")
                || path.startsWith("/org/");
    }

    private boolean isCurrentTenantPublicOrganizationEndpoint(String path) {
        return path.equals("/public/organization")
                || path.startsWith("/public/organization/");
    }
}
