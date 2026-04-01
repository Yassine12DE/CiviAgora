package tn.esprit.tic.civiAgora.mappers.organizationRequestMappers;

import org.springframework.stereotype.Component;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDto;

@Component
public class OrganizationRequestMapper {

    public OrganizationRequestDto toDto(OrganizationRequest request) {
        if (request == null) return null;
        return OrganizationRequestDto.builder()
                .id(request.getId())
                .organizationName(request.getOrganizationName())
                .desiredSlug(request.getDesiredSlug())
                .contactPersonName(request.getContactPersonName())
                .contactEmail(request.getContactEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .requestStatus(request.getRequestStatus())
                .publicVisibilityRequested(request.getPublicVisibilityRequested())
                .createdAt(request.getCreatedAt())
                .reviewedAt(request.getReviewedAt())
                .reviewComment(request.getReviewComment())
                .reviewedBy(request.getReviewedBy())
                .build();
    }

    public OrganizationRequest toEntity(OrganizationRequestDto requestDto) {
        if (requestDto == null) return null;
        return OrganizationRequest.builder()
                .id(requestDto.getId())
                .organizationName(requestDto.getOrganizationName())
                .desiredSlug(requestDto.getDesiredSlug())
                .contactPersonName(requestDto.getContactPersonName())
                .contactEmail(requestDto.getContactEmail())
                .phone(requestDto.getPhone())
                .address(requestDto.getAddress())
                .description(requestDto.getDescription())
                .logoUrl(requestDto.getLogoUrl())
                .requestStatus(requestDto.getRequestStatus())
                .publicVisibilityRequested(requestDto.getPublicVisibilityRequested())
                .createdAt(requestDto.getCreatedAt())
                .reviewedAt(requestDto.getReviewedAt())
                .reviewComment(requestDto.getReviewComment())
                .reviewedBy(requestDto.getReviewedBy())
                .build();
    }
}
