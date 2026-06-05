package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Organization;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleNotificationEmailService {

    private final EmailService emailService;

    @Value("${civox.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${civox.tenant.base-url-template:http://{slug}.lvh.me:5173}")
    private String tenantBaseUrlTemplate;

    public void sendModuleGrantedNotification(
            Organization organization,
            String moduleName,
            String moduleCode
    ) {
        String status = "GRANTED";
        String subject = "CIVOX module activated: " + safeModuleName(moduleName, moduleCode);
        String workspaceUrl = buildTenantUrl(organization.getSlug());
        String body = wrap(
                "Module activated",
                "Your organization now has access to a new CIVOX module",
                "<p>Hello " + escape(organization.getName()) + " team,</p>" +
                        "<p>Your module access has been updated successfully.</p>" +
                        buildSummary(organization, moduleName, moduleCode, status) +
                        "<p><strong>Next steps</strong></p>" +
                        "<ul style=\"margin:8px 0 0 18px;padding:0;color:#334155;line-height:1.7\">" +
                        "<li>Sign in to your tenant workspace.</li>" +
                        "<li>Open Back-office > Content Management and enable visibility if needed.</li>" +
                        "<li>Publish your first content to activate citizen-facing usage.</li>" +
                        "</ul>" +
                        "<p style=\"margin-top:18px;color:#5f6b7a;font-size:13px\">Workspace link: <a href=\"" + escape(workspaceUrl) + "\">" + escape(workspaceUrl) + "</a></p>",
                "Open Workspace",
                workspaceUrl
        );

        send(organization, subject, body);
    }

    public void sendModuleRequestRejectedNotification(
            Organization organization,
            String moduleName,
            String moduleCode,
            String reviewerComment
    ) {
        String status = "REJECTED";
        String subject = "CIVOX module request update: " + safeModuleName(moduleName, moduleCode);
        String requestPageUrl = normalizeBaseUrl(frontendBaseUrl) + "/backoffice/module-requests";
        String body = wrap(
                "Module request review",
                "Your module request was reviewed by the CIVOX platform team",
                "<p>Hello " + escape(organization.getName()) + " team,</p>" +
                        "<p>After review, this module request was not approved at this time.</p>" +
                        buildSummary(organization, moduleName, moduleCode, status) +
                        optionalReviewerComment(reviewerComment) +
                        "<p><strong>Next steps</strong></p>" +
                        "<ul style=\"margin:8px 0 0 18px;padding:0;color:#334155;line-height:1.7\">" +
                        "<li>Review the request context and business need.</li>" +
                        "<li>Adjust the scope or additional details in your next request.</li>" +
                        "<li>Submit a new module request from your tenant back-office.</li>" +
                        "</ul>" +
                        "<p style=\"margin-top:18px;color:#5f6b7a;font-size:13px\">Request follow-up page: <a href=\"" + escape(requestPageUrl) + "\">" + escape(requestPageUrl) + "</a></p>",
                "Open Module Requests",
                requestPageUrl
        );

        send(organization, subject, body);
    }

    private void send(Organization organization, String subject, String htmlBody) {
        String recipient = organization == null ? null : organization.getEmail();
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalStateException("Organization email is missing. Cannot send module notification.");
        }

        try {
            emailService.sendHtmlMessage(recipient, subject, htmlBody);
        } catch (Exception exception) {
            log.error(
                    "Failed to send module notification '{}' to organization '{}' ({})",
                    subject,
                    organization.getName(),
                    recipient,
                    exception
            );
            throw new IllegalStateException(
                    "Module notification email could not be sent to " + recipient + ". Check SMTP configuration.",
                    exception
            );
        }
    }

    private String wrap(String eyebrow, String title, String body, String buttonLabel, String buttonUrl) {
        String button = "";
        if (buttonLabel != null && buttonUrl != null) {
            button = "<p style=\"margin:24px 0 4px\"><a href=\"" + escape(buttonUrl) + "\" style=\"display:inline-block;background:#5A189A;color:#ffffff;text-decoration:none;border-radius:8px;padding:12px 18px;font-weight:700\">" +
                    escape(buttonLabel) +
                    "</a></p>";
        }

        return "<!doctype html><html><body style=\"margin:0;background:#f5f7fb;font-family:Inter,Arial,sans-serif;color:#17202a\">" +
                "<div style=\"max-width:640px;margin:0 auto;padding:28px 16px\">" +
                "<div style=\"background:#ffffff;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden\">" +
                "<div style=\"background:linear-gradient(135deg,#5A189A,#FF6B35);padding:24px;color:#ffffff\">" +
                "<p style=\"margin:0 0 8px;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.03em\">" + escape(eyebrow) + "</p>" +
                "<h1 style=\"margin:0;font-size:27px;line-height:1.24\">" + escape(title) + "</h1>" +
                "</div>" +
                "<div style=\"padding:26px;line-height:1.7;font-size:15px\">" +
                body +
                button +
                "<p style=\"margin:24px 0 0;color:#64748b;font-size:13px\">CIVOX Platform Operations</p>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    private String buildSummary(Organization organization, String moduleName, String moduleCode, String status) {
        return "<div style=\"border:1px solid #ece8f5;border-radius:8px;background:#faf9fd;padding:15px 16px;margin:16px 0\">" +
                row("Organization", escape(organization.getName())) +
                row("Module", escape(safeModuleName(moduleName, moduleCode))) +
                row("Request Status", "<strong>" + escape(status) + "</strong>") +
                "</div>";
    }

    private String row(String label, String value) {
        return "<p style=\"margin:6px 0;color:#1f2937\"><span style=\"display:inline-block;width:155px;color:#64748b\">" +
                label +
                "</span>" +
                value +
                "</p>";
    }

    private String optionalReviewerComment(String reviewerComment) {
        if (reviewerComment == null || reviewerComment.isBlank()) {
            return "";
        }
        return "<p><strong>Reviewer note:</strong> " + escape(reviewerComment.trim()) + "</p>";
    }

    private String safeModuleName(String moduleName, String moduleCode) {
        if (moduleName != null && !moduleName.isBlank()) {
            return moduleName.trim();
        }
        return moduleCode == null || moduleCode.isBlank()
                ? "Unknown module"
                : moduleCode.trim().toUpperCase(Locale.ROOT);
    }

    private String buildTenantUrl(String slug) {
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (normalizedSlug.isBlank()) {
            return normalizeBaseUrl(frontendBaseUrl);
        }
        if (tenantBaseUrlTemplate != null && !tenantBaseUrlTemplate.isBlank() && tenantBaseUrlTemplate.contains("{slug}")) {
            return tenantBaseUrlTemplate.replace("{slug}", normalizedSlug).replaceAll("/+$", "");
        }
        return normalizeBaseUrl(frontendBaseUrl);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://localhost:5173" : baseUrl.trim();
        return value.replaceAll("/+$", "");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
