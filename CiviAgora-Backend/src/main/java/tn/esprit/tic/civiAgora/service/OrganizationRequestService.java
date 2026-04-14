package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.enums.OnboardingEmailType;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.QuoteStatus;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationAccessRequestCreateDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDto;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.PaymentSummaryDto;
import tn.esprit.tic.civiAgora.mappers.organizationRequestMappers.OrganizationRequestMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationRequestService {

    private final OrganizationRequestRepository requestRepository;
    private final OrganizationRequestMapper requestMapper;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuoteCalculationService quoteCalculationService;
    private final PaymentActivationService paymentActivationService;
    private final OrganizationOnboardingEmailService onboardingEmailService;

    private static final String EMAIL_DELIVERY_WARNING =
            "Email could not be sent through the configured SMTP sender. Check the backend mail configuration, then use resend.";

    private static final List<OrganizationRequestStatus> OPEN_REQUEST_STATUSES = List.of(
            OrganizationRequestStatus.PENDING,
            OrganizationRequestStatus.QUOTE_SENT,
            OrganizationRequestStatus.AWAITING_PAYMENT,
            OrganizationRequestStatus.PAID,
            OrganizationRequestStatus.APPROVED
    );

    @Transactional
    public OrganizationRequestDto createAccessRequest(OrganizationAccessRequestCreateDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Organization request data is required");
        }

        String desiredSlug = normalizeSlug(requestDto.getDesiredSlug());
        String contactEmail = normalizeEmail(requestDto.getContactEmail());
        String adminEmail = normalizeEmail(requestDto.getAdminEmail());
        List<String> requestedModules = normalizeModuleCodes(requestDto.getRequestedModuleCodes());

        validateNewRequest(desiredSlug, contactEmail, adminEmail, requestedModules);

        LocalDateTime now = LocalDateTime.now();
        OrganizationRequest request = OrganizationRequest.builder()
                .organizationName(trim(requestDto.getOrganizationName()))
                .desiredSlug(desiredSlug)
                .contactPersonName(trim(requestDto.getContactPersonName()))
                .contactEmail(contactEmail)
                .phone(trim(requestDto.getPhone()))
                .address(trim(requestDto.getAddress()))
                .description(trim(requestDto.getDescription()))
                .logoUrl(trim(requestDto.getLogoUrl()))
                .adminFirstName(trim(requestDto.getAdminFirstName()))
                .adminLastName(trim(requestDto.getAdminLastName()))
                .adminEmail(adminEmail)
                .adminPasswordHash(passwordEncoder.encode(requestDto.getAdminTemporaryPassword()))
                .expectedNumberOfUsers(requestDto.getExpectedNumberOfUsers())
                .requestedModuleCodes(requestedModules)
                .requestedPrimaryColor(trim(requestDto.getRequestedPrimaryColor()))
                .requestedSecondaryColor(trim(requestDto.getRequestedSecondaryColor()))
                .brandingNotes(trim(requestDto.getBrandingNotes()))
                .additionalNotes(trim(requestDto.getAdditionalNotes()))
                .requestStatus(OrganizationRequestStatus.PENDING)
                .quoteStatus(QuoteStatus.NOT_CREATED)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .publicVisibilityRequested(Boolean.TRUE.equals(requestDto.getPublicVisibilityRequested()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        OrganizationRequest saved = requestRepository.save(request);
        boolean emailSent = onboardingEmailService.sendRequestReceived(saved);
        return toDtoWithEmailWarning(saved, emailSent);
    }

    @Transactional
    public OrganizationRequestDto createRequest(OrganizationRequestDto requestDto) {
        OrganizationAccessRequestCreateDto createDto = OrganizationAccessRequestCreateDto.builder()
                .organizationName(requestDto.getOrganizationName())
                .desiredSlug(requestDto.getDesiredSlug())
                .contactPersonName(requestDto.getContactPersonName())
                .contactEmail(requestDto.getContactEmail())
                .phone(requestDto.getPhone())
                .address(requestDto.getAddress())
                .description(requestDto.getDescription())
                .logoUrl(requestDto.getLogoUrl())
                .adminFirstName(requestDto.getAdminFirstName() != null ? requestDto.getAdminFirstName() : "Admin")
                .adminLastName(requestDto.getAdminLastName() != null ? requestDto.getAdminLastName() : requestDto.getOrganizationName())
                .adminEmail(requestDto.getAdminEmail() != null ? requestDto.getAdminEmail() : requestDto.getContactEmail())
                .adminTemporaryPassword("TempPass@123")
                .expectedNumberOfUsers(requestDto.getExpectedNumberOfUsers() != null ? requestDto.getExpectedNumberOfUsers() : 10)
                .requestedModuleCodes(requestDto.getRequestedModuleCodes())
                .requestedPrimaryColor(requestDto.getRequestedPrimaryColor())
                .requestedSecondaryColor(requestDto.getRequestedSecondaryColor())
                .brandingNotes(requestDto.getBrandingNotes())
                .additionalNotes(requestDto.getAdditionalNotes())
                .publicVisibilityRequested(requestDto.getPublicVisibilityRequested())
                .build();
        return createAccessRequest(createDto);
    }

    public List<OrganizationRequestDto> getAllRequests() {
        return searchRequests(null, null);
    }

    public List<OrganizationRequestDto> getRequestsByStatus(OrganizationRequestStatus status) {
        return searchRequests(status, null);
    }

    public List<OrganizationRequestDto> searchRequests(OrganizationRequestStatus status, String search) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<OrganizationRequest> source = status == null
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findByRequestStatusOrderByCreatedAtDesc(status);

        return source.stream()
                .filter(request -> matchesSearch(request, normalizedSearch))
                .sorted(Comparator.comparing(
                        OrganizationRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(requestMapper::toDto)
                .toList();
    }

    public OrganizationRequestDto getRequestById(Integer id) {
        return requestMapper.toDto(getRequestOrThrow(id));
    }

    @Transactional
    public OrganizationRequestDto generateAndSendQuote(Integer id, String processedBy) {
        OrganizationRequest request = getRequestOrThrow(id);
        ensureNotClosed(request);

        if (request.getRequestStatus() == OrganizationRequestStatus.AWAITING_PAYMENT
                || request.getRequestStatus() == OrganizationRequestStatus.PAID) {
            throw new IllegalStateException("This request is already in payment processing");
        }

        LocalDateTime now = LocalDateTime.now();
        quoteCalculationService.applyQuote(request);
        request.setQuoteStatus(QuoteStatus.SENT);
        request.setRequestStatus(OrganizationRequestStatus.QUOTE_SENT);
        request.setQuoteSentAt(now);
        request.setProcessedBy(processedBy);
        request.setUpdatedAt(now);

        OrganizationRequest saved = requestRepository.save(request);
        boolean emailSent = onboardingEmailService.sendQuoteReady(saved);
        return toDtoWithEmailWarning(saved, emailSent);
    }

    @Transactional
    public OrganizationRequestDto approveRequest(Integer id, String reviewer, String approvalNotes) {
        OrganizationRequest request = getRequestOrThrow(id);
        ensureNotClosed(request);

        if (request.getPaymentStatus() == PaymentStatus.PAID || request.getRequestStatus() == OrganizationRequestStatus.APPROVED) {
            throw new IllegalStateException("This request has already been activated");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getQuoteTotal() == null) {
            quoteCalculationService.applyQuote(request);
        }

        if (request.getQuoteSentAt() == null) {
            request.setQuoteSentAt(now);
        }

        String paymentToken = generatePaymentToken();
        request.setPaymentTokenHash(hashToken(paymentToken));
        request.setPaymentTokenCreatedAt(now);
        request.setRequestStatus(OrganizationRequestStatus.AWAITING_PAYMENT);
        request.setQuoteStatus(QuoteStatus.ACCEPTED);
        request.setPaymentStatus(PaymentStatus.AWAITING_PAYMENT);
        request.setApprovedAt(now);
        request.setReviewedAt(now);
        request.setReviewedBy(reviewer);
        request.setProcessedBy(reviewer);
        request.setApprovalNotes(approvalNotes);
        request.setReviewComment(approvalNotes);
        request.setUpdatedAt(now);

        OrganizationRequest saved = requestRepository.save(request);
        boolean emailSent = onboardingEmailService.sendPaymentLink(saved, paymentToken);
        return toDtoWithPaymentUrl(saved, paymentToken, emailSent);
    }

    @Transactional
    public OrganizationRequestDto rejectRequest(Integer id, String reviewer, String reviewComment) {
        return declineRequest(id, reviewer, reviewComment);
    }

    @Transactional
    public OrganizationRequestDto declineRequest(Integer id, String reviewer, String declineReason) {
        OrganizationRequest request = getRequestOrThrow(id);

        if (request.getRequestStatus() == OrganizationRequestStatus.APPROVED
                || request.getPaymentStatus() == PaymentStatus.PAID
                || request.getOrganizationCreatedId() != null) {
            throw new IllegalStateException("An activated request cannot be declined");
        }

        LocalDateTime now = LocalDateTime.now();
        request.setRequestStatus(OrganizationRequestStatus.DECLINED);
        request.setQuoteStatus(QuoteStatus.DECLINED);
        request.setPaymentStatus(PaymentStatus.CANCELLED);
        request.setDeclineReason(trim(declineReason));
        request.setReviewComment(trim(declineReason));
        request.setReviewedBy(reviewer);
        request.setProcessedBy(reviewer);
        request.setReviewedAt(now);
        request.setDeclinedAt(now);
        request.setPaymentTokenHash(null);
        request.setPaymentTokenCreatedAt(null);
        request.setUpdatedAt(now);

        OrganizationRequest saved = requestRepository.save(request);
        boolean emailSent = onboardingEmailService.sendDecline(saved);
        return toDtoWithEmailWarning(saved, emailSent);
    }

    @Transactional
    public OrganizationRequestDto markPaymentCompleted(Integer id, String processedBy) {
        OrganizationRequest request = getRequestOrThrow(id);
        if (request.getOrganizationCreatedId() == null
                && request.getPaymentStatus() != PaymentStatus.AWAITING_PAYMENT
                && request.getRequestStatus() != OrganizationRequestStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Payment can only be completed after a payment link has been sent");
        }

        boolean alreadyActivated = isAlreadyActivated(request);
        OrganizationRequest saved = paymentActivationService.markPaymentCompleted(request, processedBy);
        boolean emailSent = alreadyActivated || sendWelcomeEmail(saved);
        return toDtoWithEmailWarning(saved, emailSent);
    }

    @Transactional
    public PaymentSummaryDto completePaymentByToken(String token) {
        OrganizationRequest request = getRequestByPaymentToken(token);
        boolean alreadyActivated = isAlreadyActivated(request);
        OrganizationRequest saved = paymentActivationService.markPaymentCompleted(request, "payment-link");
        boolean emailSent = alreadyActivated || sendWelcomeEmail(saved);
        return toPaymentSummary(saved, emailSent);
    }

    public PaymentSummaryDto getPaymentSummaryByToken(String token) {
        OrganizationRequest request = getRequestByPaymentToken(token);
        return toPaymentSummary(request, true);
    }

    @Transactional
    public OrganizationRequestDto resendEmail(Integer id, OnboardingEmailType type) {
        if (type == null) {
            throw new IllegalArgumentException("Email type is required");
        }

        OrganizationRequest request = getRequestOrThrow(id);
        boolean emailSent = true;
        String paymentToken = null;

        switch (type) {
            case REQUEST_RECEIVED -> emailSent = onboardingEmailService.sendRequestReceived(request);
            case QUOTE -> {
                LocalDateTime now = LocalDateTime.now();
                if (request.getQuoteTotal() == null) {
                    quoteCalculationService.applyQuote(request);
                }
                request.setQuoteStatus(QuoteStatus.SENT);
                request.setRequestStatus(OrganizationRequestStatus.QUOTE_SENT);
                request.setQuoteSentAt(now);
                request.setUpdatedAt(now);
                request = requestRepository.save(request);
                emailSent = onboardingEmailService.sendQuoteReady(request);
            }
            case PAYMENT -> {
                if (request.getQuoteTotal() == null) {
                    throw new IllegalStateException("A quote must be generated before resending payment email");
                }
                if (request.getRequestStatus() == OrganizationRequestStatus.APPROVED
                        || request.getRequestStatus() == OrganizationRequestStatus.DECLINED
                        || request.getRequestStatus() == OrganizationRequestStatus.CANCELLED
                        || request.getRequestStatus() == OrganizationRequestStatus.REJECTED) {
                    throw new IllegalStateException("Payment email cannot be sent for an activated, declined, or cancelled request");
                }
                if (request.getPaymentStatus() != PaymentStatus.AWAITING_PAYMENT
                        && request.getRequestStatus() != OrganizationRequestStatus.AWAITING_PAYMENT) {
                    throw new IllegalStateException("Approve the request before sending a payment email");
                }
                paymentToken = generatePaymentToken();
                request.setPaymentTokenHash(hashToken(paymentToken));
                request.setPaymentTokenCreatedAt(LocalDateTime.now());
                request.setPaymentStatus(PaymentStatus.AWAITING_PAYMENT);
                request.setRequestStatus(OrganizationRequestStatus.AWAITING_PAYMENT);
                request.setUpdatedAt(LocalDateTime.now());
                request = requestRepository.save(request);
                emailSent = onboardingEmailService.sendPaymentLink(request, paymentToken);
            }
            case WELCOME -> {
                if (request.getOrganizationCreatedId() == null) {
                    throw new IllegalStateException("Welcome email can only be sent after activation");
                }
                Organization organization = organizationRepository.findById(request.getOrganizationCreatedId())
                        .orElseThrow(() -> new RuntimeException("Created organization is no longer available"));
                emailSent = onboardingEmailService.sendWelcome(request, organization.getSlug());
            }
            case DECLINE -> {
                if (request.getRequestStatus() != OrganizationRequestStatus.DECLINED
                        && request.getRequestStatus() != OrganizationRequestStatus.REJECTED) {
                    throw new IllegalStateException("Decline email can only be sent for declined requests");
                }
                emailSent = onboardingEmailService.sendDecline(request);
            }
        }

        if (paymentToken != null) {
            return toDtoWithPaymentUrl(request, paymentToken, emailSent);
        }
        return toDtoWithEmailWarning(request, emailSent);
    }

    private OrganizationRequest getRequestOrThrow(Integer id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization request not found with ID: " + id));
    }

    private OrganizationRequest getRequestByPaymentToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Payment token is required");
        }

        return requestRepository.findByPaymentTokenHash(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Payment link is invalid or expired"));
    }

    private PaymentSummaryDto toPaymentSummary(OrganizationRequest request, boolean emailSent) {
        String organizationAccessUrl = null;
        if (request.getOrganizationCreatedId() != null) {
            organizationAccessUrl = organizationRepository.findById(request.getOrganizationCreatedId())
                    .map(Organization::getSlug)
                    .map(onboardingEmailService::buildTenantUrl)
                    .orElse(null);
        }

        return PaymentSummaryDto.builder()
                .organizationName(request.getOrganizationName())
                .desiredSlug(request.getDesiredSlug())
                .contactEmail(request.getContactEmail())
                .adminEmail(request.getAdminEmail())
                .expectedNumberOfUsers(request.getExpectedNumberOfUsers())
                .requestedModuleCodes(request.getRequestedModuleCodes())
                .requestStatus(request.getRequestStatus())
                .paymentStatus(request.getPaymentStatus())
                .quoteBaseFee(request.getQuoteBaseFee())
                .quoteUserFee(request.getQuoteUserFee())
                .quoteModuleFee(request.getQuoteModuleFee())
                .quoteSetupFee(request.getQuoteSetupFee())
                .quoteTotal(request.getQuoteTotal())
                .quoteAssumptions(request.getQuoteAssumptions())
                .organizationCreatedId(request.getOrganizationCreatedId())
                .organizationAccessUrl(organizationAccessUrl)
                .emailDeliveryWarning(emailSent ? null : EMAIL_DELIVERY_WARNING)
                .build();
    }

    private boolean sendWelcomeEmail(OrganizationRequest request) {
        if (request.getOrganizationCreatedId() == null) {
            return false;
        }

        Organization organization = organizationRepository.findById(request.getOrganizationCreatedId())
                .orElseThrow(() -> new RuntimeException("Created organization is no longer available"));
        return onboardingEmailService.sendWelcome(request, organization.getSlug());
    }

    private boolean isAlreadyActivated(OrganizationRequest request) {
        return request.getOrganizationCreatedId() != null && request.getPaymentStatus() == PaymentStatus.PAID;
    }

    private OrganizationRequestDto toDtoWithPaymentUrl(OrganizationRequest request, String paymentToken, boolean emailSent) {
        OrganizationRequestDto dto = toDtoWithEmailWarning(request, emailSent);
        dto.setPaymentUrl(onboardingEmailService.buildPaymentUrl(paymentToken));
        return dto;
    }

    private OrganizationRequestDto toDtoWithEmailWarning(OrganizationRequest request, boolean emailSent) {
        OrganizationRequestDto dto = requestMapper.toDto(request);
        if (!emailSent) {
            dto.setEmailDeliveryWarning(EMAIL_DELIVERY_WARNING);
        }
        return dto;
    }

    private void validateNewRequest(String desiredSlug, String contactEmail, String adminEmail, List<String> requestedModules) {
        if (organizationRepository.findBySlug(desiredSlug).isPresent()) {
            throw new IllegalStateException("This slug is already used by an organization");
        }
        if (requestRepository.existsByDesiredSlugIgnoreCaseAndRequestStatusIn(desiredSlug, OPEN_REQUEST_STATUSES)) {
            throw new IllegalStateException("This slug is already attached to an active access request");
        }
        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalStateException("This admin email is already in use");
        }
        if (requestRepository.existsByAdminEmailIgnoreCaseAndRequestStatusIn(adminEmail, OPEN_REQUEST_STATUSES)) {
            throw new IllegalStateException("This admin email is already attached to an active access request");
        }
        if (requestRepository.existsByContactEmailIgnoreCaseAndRequestStatusIn(contactEmail, OPEN_REQUEST_STATUSES)) {
            throw new IllegalStateException("This contact email is already attached to an active access request");
        }
        for (String moduleCode : requestedModules) {
            moduleRepository.findByCode(moduleCode)
                    .filter(module -> Boolean.TRUE.equals(module.getActive()))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown or inactive module code: " + moduleCode));
        }
    }

    private void ensureNotClosed(OrganizationRequest request) {
        if (request.getRequestStatus() == OrganizationRequestStatus.DECLINED
                || request.getRequestStatus() == OrganizationRequestStatus.REJECTED
                || request.getRequestStatus() == OrganizationRequestStatus.CANCELLED
                || request.getRequestStatus() == OrganizationRequestStatus.APPROVED) {
            throw new IllegalStateException("This request can no longer be changed");
        }
    }

    private boolean matchesSearch(OrganizationRequest request, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        return contains(request.getOrganizationName(), search)
                || contains(request.getDesiredSlug(), search)
                || contains(request.getContactEmail(), search)
                || contains(request.getAdminEmail(), search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private List<String> normalizeModuleCodes(List<String> moduleCodes) {
        if (moduleCodes == null) {
            return new ArrayList<>();
        }

        return moduleCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String normalizeSlug(String value) {
        String slug = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        slug = slug.replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (slug.length() < 3) {
            throw new IllegalArgumentException("Slug must contain at least 3 valid characters");
        }
        return slug;
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String generatePaymentToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
