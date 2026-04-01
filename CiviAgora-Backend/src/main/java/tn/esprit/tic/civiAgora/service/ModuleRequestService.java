package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.ModuleRequest;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.enums.ModuleRequestStatus;
import tn.esprit.tic.civiAgora.dao.repository.ModuleRequestRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.moduleRequestDto.ModuleRequestDto;
import tn.esprit.tic.civiAgora.mappers.moduleRequestMappers.ModuleRequestMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleRequestService {

    private final ModuleRequestRepository moduleRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final ModuleService moduleService;
    private final OrganizationModuleService organizationModuleService;
    private final ModuleRequestMapper moduleRequestMapper;

    public List<ModuleRequestDto> getAllRequests() {
        return moduleRequestRepository.findAll()
                .stream()
                .map(moduleRequestMapper::toDto)
                .toList();
    }

    public List<ModuleRequestDto> getRequestsByOrganization(Integer organizationId) {
        return moduleRequestRepository.findByOrganizationId(organizationId)
                .stream()
                .map(moduleRequestMapper::toDto)
                .toList();
    }

    public List<ModuleRequestDto> getRequestsByStatus(ModuleRequestStatus status) {
        return moduleRequestRepository.findByStatus(status)
                .stream()
                .map(moduleRequestMapper::toDto)
                .toList();
    }

    public ModuleRequestDto createRequest(Integer organizationId, String moduleCode, String comment) {
        if (moduleRequestRepository.existsByOrganizationIdAndModuleCodeAndStatus(
                organizationId, moduleCode, ModuleRequestStatus.PENDING)) {
            throw new RuntimeException("A pending request already exists for this module");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        Module module = moduleService.getModuleByCode(moduleCode);

        ModuleRequest request = ModuleRequest.builder()
                .organization(organization)
                .module(module)
                .status(ModuleRequestStatus.PENDING)
                .requestDate(LocalDateTime.now())
                .comment(comment)
                .build();

        return moduleRequestMapper.toDto(moduleRequestRepository.save(request));
    }

    public ModuleRequestDto approveRequest(Long requestId, String comment) {
        ModuleRequest request = moduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Module request not found"));

        if (request.getStatus() != ModuleRequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be approved");
        }

        request.setStatus(ModuleRequestStatus.APPROVED);
        request.setReviewedDate(LocalDateTime.now());
        request.setComment(comment);

        organizationModuleService.grantModuleToOrganization(
                request.getOrganization().getId(),
                request.getModule().getCode(),
                null
        );

        return moduleRequestMapper.toDto(moduleRequestRepository.save(request));
    }

    public ModuleRequestDto rejectRequest(Long requestId, String comment) {
        ModuleRequest request = moduleRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Module request not found"));

        if (request.getStatus() != ModuleRequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be rejected");
        }

        request.setStatus(ModuleRequestStatus.REJECTED);
        request.setReviewedDate(LocalDateTime.now());
        request.setComment(comment);

        return moduleRequestMapper.toDto(moduleRequestRepository.save(request));
    }
}