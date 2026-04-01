package tn.esprit.tic.civiAgora.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationRequestStatus;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRequestRepository;
import tn.esprit.tic.civiAgora.dto.organizationRequestDto.OrganizationRequestDto;
import tn.esprit.tic.civiAgora.mappers.organizationRequestMappers.OrganizationRequestMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationRequestService {

    @Autowired
    private OrganizationRequestRepository requestRepository;

    @Autowired
    private OrganizationRequestMapper requestMapper;

    @Autowired
    private OrganizationService organizationService;

    public OrganizationRequestDto createRequest(OrganizationRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Organization request data is required");
        }

        if (requestRepository.findByDesiredSlug(requestDto.getDesiredSlug()).isPresent()) {
            throw new IllegalStateException("Slug already requested");
        }

        OrganizationRequest request = requestMapper.toEntity(requestDto);
        request.setRequestStatus(OrganizationRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        OrganizationRequest saved = requestRepository.save(request);
        return requestMapper.toDto(saved);
    }

    public List<OrganizationRequestDto> getAllRequests() {
        return requestRepository.findAll().stream().map(requestMapper::toDto).toList();
    }

    public List<OrganizationRequestDto> getRequestsByStatus(OrganizationRequestStatus status) {
        return requestRepository.findByRequestStatus(status).stream().map(requestMapper::toDto).toList();
    }

    public OrganizationRequestDto getRequestById(Integer id) {
        OrganizationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OrganizationRequest not found with ID: " + id));
        return requestMapper.toDto(request);
    }

    public OrganizationRequestDto approveRequest(Integer id, String reviewer, String reviewComment) {
        OrganizationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OrganizationRequest not found with ID: " + id));

        if (request.getRequestStatus() != OrganizationRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        request.setRequestStatus(OrganizationRequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewComment(reviewComment);
        request.setReviewedBy(reviewer);

        // Provision organization and admin user
        organizationService.createOrganizationFromRequest(request);

        OrganizationRequest saved = requestRepository.save(request);
        return requestMapper.toDto(saved);
    }

    public OrganizationRequestDto rejectRequest(Integer id, String reviewer, String reviewComment) {
        OrganizationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OrganizationRequest not found with ID: " + id));

        if (request.getRequestStatus() != OrganizationRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }

        request.setRequestStatus(OrganizationRequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewComment(reviewComment);
        request.setReviewedBy(reviewer);

        OrganizationRequest saved = requestRepository.save(request);
        return requestMapper.toDto(saved);
    }
}
