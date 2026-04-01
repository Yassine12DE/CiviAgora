package tn.esprit.tic.civiAgora.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.service.OrganizationService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final OrganizationService organizationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String host = request.getHeader("Host");
            String orgSlug = null;

            if (host != null) {
                String normalizedHost = host.split(":")[0];
                if (!normalizedHost.equalsIgnoreCase("localhost")
                        && !normalizedHost.equals("127.0.0.1")
                        && !normalizedHost.contains(".") ) {
                    // single-label host (dev custom) proceed
                    orgSlug = normalizedHost;
                } else if (normalizedHost.contains(".")) {
                    String[] parts = normalizedHost.split("\\.");
                    if (parts.length > 2) {
                        orgSlug = parts[0];
                    }
                }
            }

            // Also support query param fallback for local development
            if ((orgSlug == null || orgSlug.isBlank()) && request.getParameter("orgSlug") != null) {
                orgSlug = request.getParameter("orgSlug");
            }

            if (orgSlug != null && !orgSlug.isBlank()) {
                try {
                    Organization organization = organizationService.getOrganizationBySlug(orgSlug);
                    if (organization != null) {
                        TenantContext.setCurrentOrganizationSlug(organization.getSlug());
                    } else {
                        TenantContext.clear();
                    }
                } catch (Exception e) {
                    TenantContext.clear();
                }
            } else {
                TenantContext.clear();
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // public and auth endpoints don't need tenant evaluation
        String path = request.getServletPath();
        return path.startsWith("/auth") || path.startsWith("/public");
    }
}
