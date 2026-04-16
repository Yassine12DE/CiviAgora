package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationOnboardingEmailService {

    private final EmailService emailService;

    @Value("${civox.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${civox.tenant.base-url-template:http://{slug}.lvh.me:5173}")
    private String tenantBaseUrlTemplate;

    @Value("${civox.mail.fail-on-error:true}")
    private boolean failOnError;

    public boolean sendRequestReceived(OrganizationRequest request) {
        return send(
                request.getContactEmail(),
                "We received your Civox access request",
                wrap(
                        "Request received",
                        "Thank you for choosing Civox",
                        "<p>Hello " + escape(request.getContactPersonName()) + ",</p>" +
                                "<p>We received the request for <strong>" + escape(request.getOrganizationName()) + "</strong>. Our team will review your information and prepare the next step.</p>" +
                                "<p>You do not need to do anything else right now. We will contact you when the quote and approval decision are ready.</p>" +
                                "<p style=\"margin-top:18px;color:#5f6b7a\">Reference slug: <strong>" + escape(request.getDesiredSlug()) + "</strong></p>",
                        null,
                        null
                )
        );
    }

    public boolean sendQuoteReady(OrganizationRequest request) {
        return send(
                request.getContactEmail(),
                "Your Civox quote is ready",
                wrap(
                        "Quote prepared",
                        "Your Civox onboarding quote",
                        quoteSummary(request) +
                                "<p>Our platform team has prepared the quote for your requested scope. Once approved, you will receive a secure payment link to activate your organization workspace.</p>" +
                                "<p>If your scope changes before payment, reply to this email and our team will update the quote before activation.</p>",
                        null,
                        null
                )
        );
    }

    public boolean sendPaymentLink(OrganizationRequest request, String paymentToken) {
        String paymentUrl = buildPaymentUrl(paymentToken);

        return send(
                request.getContactEmail(),
                "Approve your Civox quote and complete payment",
                wrap(
                        "Payment step",
                        "Your Civox platform is ready for activation",
                        "<p>Hello " + escape(request.getContactPersonName()) + ",</p>" +
                                "<p>Your access request for <strong>" + escape(request.getOrganizationName()) + "</strong> has been approved for payment.</p>" +
                                quoteSummary(request) +
                                "<p>Use the secure button below to review the quote and complete payment. After payment succeeds, your organization workspace and initial admin account will be created automatically.</p>" +
                                "<p style=\"color:#5f6b7a;font-size:13px\">If the button does not open, copy this link into your browser:<br><a href=\"" + escape(paymentUrl) + "\">" + escape(paymentUrl) + "</a></p>",
                        "Review and pay",
                        paymentUrl
                )
        );
    }

    public boolean sendWelcome(OrganizationRequest request, String organizationSlug) {
        String platformUrl = buildTenantUrl(organizationSlug);

        return send(
                request.getAdminEmail(),
                "Welcome to Civox",
                wrap(
                        "Welcome aboard",
                        "Your Civox platform is active",
                        "<p>Hello " + escape(request.getAdminFirstName()) + ",</p>" +
                                "<p><strong>" + escape(request.getOrganizationName()) + "</strong> is now active on Civox. Your admin workspace is ready and your selected modules have been assigned.</p>" +
                                "<p>Admin account: <strong>" + escape(request.getAdminEmail()) + "</strong></p>" +
                                "<p>You can sign in with the temporary password provided during the request. We recommend updating it after your first login.</p>" +
                                "<p style=\"color:#5f6b7a;font-size:13px\">Workspace link:<br><a href=\"" + escape(platformUrl) + "\">" + escape(platformUrl) + "</a></p>",
                        "Access your Civox platform",
                        platformUrl
                )
        );
    }

    public boolean sendDecline(OrganizationRequest request) {
        String reason = request.getDeclineReason() == null || request.getDeclineReason().isBlank()
                ? "No additional reason was provided."
                : escape(request.getDeclineReason());

        return send(
                request.getContactEmail(),
                "Update on your Civox access request",
                wrap(
                        "Request update",
                        "Thank you for considering Civox",
                        "<p>Hello " + escape(request.getContactPersonName()) + ",</p>" +
                                "<p>After review, we are sorry to let you know that we cannot approve the access request for <strong>" + escape(request.getOrganizationName()) + "</strong> at this time.</p>" +
                                "<p><strong>Reason:</strong> " + reason + "</p>" +
                                "<p>We appreciate the time you spent preparing the request and wish your team continued success.</p>",
                        null,
                        null
                )
        );
    }

    private boolean send(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            String message = "Onboarding email recipient is missing for: " + subject;
            if (failOnError) {
                throw new IllegalStateException(message);
            }
            log.warn(message);
            return false;
        }

        try {
            emailService.sendHtmlMessage(to, subject, html);
            return true;
        } catch (Exception exception) {
            log.error(
                    "Could not send onboarding email '{}' to {} through the configured SMTP sender: {}",
                    subject,
                    to,
                    rootMessage(exception),
                    exception
            );
            if (failOnError) {
                throw new IllegalStateException("Onboarding email could not be sent to " + to + ". Check the configured SMTP sender.", exception);
            }
            return false;
        }
    }

    public String buildTenantUrl(String slug) {
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (normalizedSlug.isBlank()) {
            return normalizeBaseUrl(frontendBaseUrl);
        }
        if (tenantBaseUrlTemplate != null && !tenantBaseUrlTemplate.isBlank()) {
            if (tenantBaseUrlTemplate.contains("{slug}")) {
                return tenantBaseUrlTemplate.replace("{slug}", normalizedSlug).replaceAll("/+$", "");
            }
            return normalizeBaseUrl(tenantBaseUrlTemplate);
        }
        return normalizeBaseUrl(frontendBaseUrl);
    }

    public String buildPaymentUrl(String paymentToken) {
        if (paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("Payment token is required to build payment URL");
        }
        return normalizeBaseUrl(frontendBaseUrl) + "/payment/" + paymentToken.trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://localhost:5173" : baseUrl.trim();
        return value.replaceAll("/+$", "");
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }

    private String quoteSummary(OrganizationRequest request) {
        List<String> moduleCodes = request.getRequestedModuleCodes() == null
                ? List.of()
                : request.getRequestedModuleCodes();

        return "<div style=\"border:1px solid #eadff4;border-radius:8px;padding:16px;margin:18px 0;background:#fbf9fd\">" +
                "<p style=\"margin:0 0 10px;color:#125c54;font-weight:800\">Quote summary</p>" +
                row("Expected users", String.valueOf(request.getExpectedNumberOfUsers())) +
                row("Requested modules", escape(moduleCodes.isEmpty() ? "Default Civox module set" : String.join(", ", moduleCodes))) +
                row("Base platform fee", money(request.getQuoteBaseFee())) +
                row("User fee", money(request.getQuoteUserFee())) +
                row("Module fee", money(request.getQuoteModuleFee())) +
                row("Setup fee", money(request.getQuoteSetupFee())) +
                "<div style=\"height:1px;background:#eadff4;margin:12px 0\"></div>" +
                row("Total", "<strong>" + money(request.getQuoteTotal()) + "</strong>") +
                quoteAssumptions(request) +
                "</div>";
    }

    private String row(String label, String value) {
        return "<p style=\"margin:6px 0;color:#17202a\"><span style=\"display:inline-block;width:170px;color:#5f6b7a\">" +
                label +
                "</span>" +
                value +
                "</p>";
    }

    private String quoteAssumptions(OrganizationRequest request) {
        if (request.getQuoteAssumptions() == null || request.getQuoteAssumptions().isBlank()) {
            return "";
        }

        return "<p style=\"margin:12px 0 0;color:#5f6b7a;font-size:13px;line-height:1.6\">" +
                escape(request.getQuoteAssumptions()) +
                "</p>";
    }

    private String wrap(String eyebrow, String title, String body, String buttonLabel, String buttonUrl) {
        String button = "";
        if (buttonLabel != null && buttonUrl != null) {
            button = "<p style=\"margin:24px 0 4px\"><a href=\"" + escape(buttonUrl) + "\" style=\"display:inline-block;background:#125c54;color:#ffffff;text-decoration:none;border-radius:8px;padding:13px 18px;font-weight:800\">" +
                    escape(buttonLabel) +
                    "</a></p>";
        }

        return "<!doctype html><html><body style=\"margin:0;background:#f5f7f8;font-family:Inter,Arial,sans-serif;color:#17202a\">" +
                "<div style=\"max-width:640px;margin:0 auto;padding:28px 16px\">" +
                "<div style=\"background:#ffffff;border:1px solid #d9e2e7;border-radius:8px;overflow:hidden\">" +
                "<div style=\"background:#102a43;padding:24px;color:#ffffff\">" +
                "<p style=\"margin:0 0 8px;font-size:12px;font-weight:800;text-transform:uppercase\">" + escape(eyebrow) + "</p>" +
                "<h1 style=\"margin:0;font-size:28px;line-height:1.2\">" + escape(title) + "</h1>" +
                "</div>" +
                "<div style=\"padding:26px;line-height:1.7;font-size:15px\">" +
                body +
                button +
                "<p style=\"margin:24px 0 0;color:#5f6b7a;font-size:13px\">The Civox team</p>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return "Not calculated";
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
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
