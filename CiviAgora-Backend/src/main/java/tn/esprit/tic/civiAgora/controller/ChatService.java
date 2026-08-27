package tn.esprit.tic.civiAgora.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.config.TenantContext;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatRequest;
import tn.esprit.tic.civiAgora.dto.chatDto.ChatResponse;
import tn.esprit.tic.civiAgora.dto.chatDto.OllamaChatResult;
import tn.esprit.tic.civiAgora.service.ModuleAccessService;
import tn.esprit.tic.civiAgora.service.TenantAccessService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_MESSAGE_CHARS = 4_000;

    private final OllamaClient ollamaClient;
    private final TenantAccessService tenantAccessService;
    private final ModuleAccessService moduleAccessService;
    private final ChatUiPayloadFactory chatUiPayloadFactory;

    public ChatResponse chat(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return new ChatResponse("Please enter a question.", isAuthenticated(), TenantContext.getResolvedOrganizationSlug());
        }
        if (request.message().length() > MAX_MESSAGE_CHARS) {
            return new ChatResponse("That message is too long. Please shorten it and try again.",
                    isAuthenticated(), TenantContext.getResolvedOrganizationSlug());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            return chatAuthenticated(request, user);
        }
        return chatAnonymous(request);
    }

    private ChatResponse chatAnonymous(ChatRequest request) {
        Organization organization = null;
        try {
            organization = tenantAccessService.getResolvedOrganizationFromRequestContext();
        } catch (RuntimeException ignored) {
            // A public assistant remains available without a resolved tenant.
        }
        String organizationName = organization == null ? "CIVOX" : organization.getName();
        String slug = organization == null ? TenantContext.getResolvedOrganizationSlug() : organization.getSlug();

        String systemPrompt = """
                You are CIVOX Assistant for %s. CIVOX is a civic engagement SaaS platform for surveys,
                voting, consultations, Youth News, and organization participation.

                SECURITY AND ACCURACY RULES:
                - This user is anonymous. You have no tools and no permission to access private application data.
                - Never provide profiles, personal participation, user lists, private analytics, private results,
                  management data, settings, or administrative information.
                - If asked about "my" profile, answers, votes, or participation, explain that sign-in is required.
                - Only describe general CIVOX concepts and public information explicitly present in this context.
                - Events and Complaints are placeholders; general News is incomplete. Do not claim they are operational.
                - Never invent organization facts, activities, routes, counts, percentages, or functionality.
                - Answer in the same language as the user, concisely and helpfully.
                """.formatted(organizationName);

        OllamaChatResult result = ollamaClient.chat(systemPrompt, buildMessages(request), false, request.message());
        return new ChatResponse(result.reply(), false, slug);
    }

    private ChatResponse chatAuthenticated(ChatRequest request, User user) {
        final Organization organization;
        try {
            organization = tenantAccessService.getCurrentOrganizationEntityOrThrow();
            if (!tenantAccessService.isCurrentUserSuperAdmin()
                    && (user.getOrganization() == null
                    || !organization.getId().equals(user.getOrganization().getId()))) {
                throw new IllegalStateException("Authenticated user tenant mismatch");
            }
        } catch (RuntimeException exception) {
            log.warn("Authenticated chat rejected because tenant context was not verified");
            return new ChatResponse(
                    "I couldn't verify your organization context. Open the assistant from your organization's CIVOX address and try again.",
                    true,
                    null
            );
        }

        List<Map<String, Object>> accessibleModules = moduleAccessService.getModulesForCurrentUser();
        String moduleSummary = accessibleModules.isEmpty()
                ? "No front-office modules are currently accessible."
                : accessibleModules.stream()
                .map(module -> module.get("code") + " (" + module.get("name") + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("No front-office modules are currently accessible.");

        String systemPrompt = """
                You are CIVOX Copilot, the authenticated assistant for the current CIVOX tenant.

                CURRENT TRUSTED CONTEXT (informational only; backend tools enforce authorization):
                - User: %s %s
                - Role: %s
                - Organization: %s
                - Organization slug: %s
                - Accessible front-office modules: %s

                SECURITY AND TOOL RULES:
                - Use tools for every current, private, personal, tenant, survey, content, user, module, or analytics fact.
                - The role written here is not an authorization mechanism. Obey tool authorization results exactly.
                - Never ask for or pass a user id or organization id; tools derive both from the authenticated backend context.
                - Never infer another tenant's data. Never reveal data after a tool returns FORBIDDEN or TENANT_UNAVAILABLE.
                - Do not invent records, routes, counts, percentages, results, permissions, or missing tool output.
                - Only present result counts or percentages that appear verbatim in current tool data.
                - Return internal routes only when navigation_help or another tool supplied that route.
                - Events and Complaints are placeholders; general News is incomplete. Do not claim unsupported actions exist.
                - Write tools use a two-turn server-side confirmation workflow. First prepare the exact action and ask
                  the user to confirm. Execute it only after the user explicitly confirms in a later message.
                - Never invent write parameters. If required parameters are missing, ask for them before preparing.
                - Answer in the same language as the user. Be concise, useful, and mention unavailable permissions naturally.
                - When tool data will be rendered as structured UI, give only a short one- or two-sentence conversational summary.
                  Do not repeat every KPI, record, module, or result as Markdown bullets.
                """.formatted(
                safe(user.getFirstName()), safe(user.getLastName()),
                user.getRole() == null ? "UNKNOWN" : user.getRole().name(),
                organization.getName(), organization.getSlug(), moduleSummary
        );

        OllamaChatResult result = ollamaClient.chat(systemPrompt, buildMessages(request), true, request.message());
        return new ChatResponse(result.reply(), true, organization.getSlug(),
                chatUiPayloadFactory.from(result, organization.getName()));
    }

    private List<Map<String, Object>> buildMessages(ChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.history() != null) {
            int from = Math.max(0, request.history().size() - MAX_HISTORY_MESSAGES);
            for (ChatRequest.MessageEntry entry : request.history().subList(from, request.history().size())) {
                if (entry == null || entry.content() == null || entry.content().isBlank()) continue;
                String role = "assistant".equals(entry.role()) ? "assistant" : "user";
                String content = entry.content().length() > MAX_MESSAGE_CHARS
                        ? entry.content().substring(0, MAX_MESSAGE_CHARS)
                        : entry.content();
                messages.add(Map.of("role", role, "content", content));
            }
        }
        messages.add(Map.of("role", "user", "content", request.message().trim()));
        return messages;
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
