package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentItem;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentResponse;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentItemRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentResponseRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentDto;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentInteractionRequest;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentRequest;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationContentService {

    private final OrganizationContentItemRepository contentRepository;
    private final OrganizationContentResponseRepository responseRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationModuleRepository organizationModuleRepository;

    public List<OrganizationContentDto> getContent(Integer organizationId, OrganizationContentType type) {
        return contentRepository.findByOrganizationIdAndTypeOrderByCreatedAtDesc(organizationId, type)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<OrganizationContentDto> getVisibleContentForCurrentUser(
            Integer organizationId,
            OrganizationContentType type,
            User currentUser
    ) {
        requireModuleEnabled(organizationId, type);

        List<OrganizationContentItem> items = contentRepository
                .findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(organizationId, type);

        if (items.isEmpty() || currentUser == null || currentUser.getId() == null) {
            return items.stream()
                    .map(this::toDto)
                    .toList();
        }

        List<Long> itemIds = items.stream()
                .map(OrganizationContentItem::getId)
                .toList();

        Map<Long, OrganizationContentResponse> responsesByContentId = responseRepository
                .findByOrganizationIdAndUserIdAndContentItemIdIn(organizationId, currentUser.getId(), itemIds)
                .stream()
                .collect(Collectors.toMap(
                        response -> response.getContentItem().getId(),
                        Function.identity()
                ));

        return items.stream()
                .map(item -> toDto(item, responsesByContentId.get(item.getId())))
                .toList();
    }

    public OrganizationContentDto createContent(
            Integer organizationId,
            OrganizationContentType type,
            OrganizationContentRequest request,
            User createdBy
    ) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        requireModuleEnabled(organizationId, type);

        OrganizationContentItem item = OrganizationContentItem.builder()
                .organization(organization)
                .createdBy(createdBy)
                .type(type)
                .title(request.getTitle())
                .body(request.getBody())
                .optionsText(toOptionsText(request.getOptions()))
                .published(request.getPublished() == null ? true : request.getPublished())
                .build();

        return toDto(contentRepository.save(item));
    }

    public OrganizationContentDto saveCurrentUserResponse(
            Integer organizationId,
            OrganizationContentType type,
            Long contentId,
            OrganizationContentInteractionRequest request,
            User currentUser
    ) {
        if (contentId == null) {
            throw new IllegalArgumentException("Content id is required");
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("Authenticated user required");
        }

        requireModuleEnabled(organizationId, type);

        OrganizationContentItem item = contentRepository
                .findByIdAndOrganizationIdAndType(contentId, organizationId, type)
                .orElseThrow(() -> new IllegalArgumentException("Content not found for this organization"));

        if (!Boolean.TRUE.equals(item.getPublished())) {
            throw new AccessDeniedException("This content is not published");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        OrganizationContentResponse response = responseRepository
                .findByOrganizationIdAndContentItemIdAndUserId(organizationId, contentId, currentUser.getId())
                .orElseGet(() -> OrganizationContentResponse.builder()
                        .organization(organization)
                        .contentItem(item)
                        .user(currentUser)
                        .type(type)
                        .build());

        applyResponse(type, item, request, response);

        return toDto(item, responseRepository.save(response));
    }

    private void requireModuleEnabled(Integer organizationId, OrganizationContentType type) {
        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, type.getModuleCode())
                .orElseThrow(() -> new AccessDeniedException("This module is not granted to the organization"));

        if (!Boolean.TRUE.equals(organizationModule.getGrantedBySaas())
                || !Boolean.TRUE.equals(organizationModule.getEnabledByOrganization())) {
            throw new AccessDeniedException("This module is not enabled for the organization");
        }
    }

    private OrganizationContentDto toDto(OrganizationContentItem item) {
        return toDto(item, null);
    }

    private OrganizationContentDto toDto(OrganizationContentItem item, OrganizationContentResponse response) {
        User createdBy = item.getCreatedBy();
        String createdByName = createdBy == null
                ? null
                : ("%s %s".formatted(
                        createdBy.getFirstName() == null ? "" : createdBy.getFirstName(),
                        createdBy.getLastName() == null ? "" : createdBy.getLastName()
                ).trim());

        return OrganizationContentDto.builder()
                .id(item.getId())
                .type(item.getType() != null ? item.getType().name() : null)
                .title(item.getTitle())
                .body(item.getBody())
                .options(toOptionsList(item.getOptionsText()))
                .published(item.getPublished())
                .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null)
                .organizationId(item.getOrganization() != null ? item.getOrganization().getId() : null)
                .createdByUserId(createdBy != null ? createdBy.getId() : null)
                .createdByName(createdByName)
                .myAnswer(response != null ? response.getAnswer() : null)
                .myParticipating(response != null ? response.getParticipating() : null)
                .myReaction(response != null ? response.getReaction() : null)
                .build();
    }

    private void applyResponse(
            OrganizationContentType type,
            OrganizationContentItem item,
            OrganizationContentInteractionRequest request,
            OrganizationContentResponse response
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Response payload is required");
        }

        response.setType(type);
        response.setAnswer(null);
        response.setParticipating(null);
        response.setReaction(null);

        switch (type) {
            case VOTE -> {
                String answer = normalizeRequiredText(request.getAnswer(), "Vote answer is required");
                validateVoteAnswer(item, answer);
                response.setAnswer(answer);
            }
            case CONCERTATION -> {
                if (request.getParticipating() == null) {
                    throw new IllegalArgumentException("Participation answer is required");
                }
                response.setParticipating(request.getParticipating());
            }
            case YOUTH_NEWS -> {
                String reaction = normalizeRequiredText(request.getReaction(), "Reaction is required");
                response.setReaction(reaction);
            }
        }
    }

    private void validateVoteAnswer(OrganizationContentItem item, String answer) {
        List<String> options = toOptionsList(item.getOptionsText());
        if (options.isEmpty()) {
            return;
        }

        boolean optionExists = options.stream()
                .anyMatch(option -> option.equalsIgnoreCase(answer));

        if (!optionExists) {
            throw new IllegalArgumentException("Vote answer must match one of the published options");
        }
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String toOptionsText(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        return String.join("\n", options.stream()
                .map(String::trim)
                .filter(option -> !option.isBlank())
                .toList());
    }

    private List<String> toOptionsList(String optionsText) {
        if (optionsText == null || optionsText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(optionsText.split("\\R"))
                .map(String::trim)
                .filter(option -> !option.isBlank())
                .toList();
    }
}
