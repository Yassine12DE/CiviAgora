package tn.esprit.tic.civiAgora.event;

import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestPaymentEventDto;

public record OrganizationRequestPaymentConfirmedEvent(OrganizationRequestPaymentEventDto payload) {
}
