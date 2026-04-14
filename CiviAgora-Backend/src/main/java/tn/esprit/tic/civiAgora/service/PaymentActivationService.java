package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.PaymentStatus;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRequestRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentActivationService {

    private final OrganizationRequestRepository requestRepository;
    private final OrganizationProvisioningService provisioningService;

    @Transactional
    public OrganizationRequest markPaymentCompleted(OrganizationRequest request, String processedBy) {
        if (request.getRequestStatus() == OrganizationRequestStatus.DECLINED
                || request.getRequestStatus() == OrganizationRequestStatus.REJECTED
                || request.getRequestStatus() == OrganizationRequestStatus.CANCELLED) {
            throw new IllegalStateException("Payment cannot be completed for a declined or cancelled request");
        }

        if (request.getOrganizationCreatedId() != null && request.getPaymentStatus() == PaymentStatus.PAID) {
            return request;
        }

        if (request.getPaymentStatus() != PaymentStatus.AWAITING_PAYMENT
                && request.getRequestStatus() != OrganizationRequestStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Payment cannot be completed before the payment link step");
        }

        if (request.getQuoteTotal() == null) {
            throw new IllegalStateException("A quote must be generated before payment can be completed");
        }

        LocalDateTime now = LocalDateTime.now();
        request.setPaymentStatus(PaymentStatus.PAID);
        request.setRequestStatus(OrganizationRequestStatus.PAID);
        request.setPaidAt(now);
        request.setProcessedBy(processedBy);
        request.setUpdatedAt(now);

        Organization organization = provisioningService.activateRequest(request);
        request.setRequestStatus(OrganizationRequestStatus.APPROVED);
        request.setActivatedAt(now);
        request.setReviewedAt(now);
        request.setOrganizationCreatedId(organization.getId());
        request.setUpdatedAt(now);

        return requestRepository.save(request);
    }
}
