package tn.esprit.tic.civiAgora.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationContentService {

    private final OrganizationContentItemRepository contentRepository;
    private final OrganizationContentResponseRepository responseRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationModuleRepository organizationModuleRepository;
    private final TenantAccessService tenantAccessService;

    @Transactional(readOnly = true)
    public List<OrganizationContentDto> getContent(Integer organizationId, OrganizationContentType type) {
        List<OrganizationContentItem> items =
                contentRepository.findByOrganizationIdAndTypeOrderByCreatedAtDesc(organizationId, type);
        Map<Long, ResponseSummary> summaries = buildResponseSummaries(items);

        return items.stream()
                .map(item -> toDto(item, null, summaries.get(item.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationContentDto> getCurrentOrganizationPublicContent(OrganizationContentType type) {
        Integer organizationId = tenantAccessService.getResolvedOrganizationOrThrow().getId();
        requireModuleEnabled(organizationId, type);

        List<OrganizationContentItem> items = contentRepository
                .findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(organizationId, type);
        Map<Long, ResponseSummary> summaries = buildResponseSummaries(items);

        return items.stream()
                .map(item -> toDto(item, null, summaries.get(item.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationContentDto> getVisibleContentForCurrentUser(
            Integer organizationId,
            OrganizationContentType type,
            User currentUser
    ) {
        requireModuleEnabled(organizationId, type);

        List<OrganizationContentItem> items = contentRepository
                .findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(organizationId, type);

        if (items.isEmpty() || currentUser == null || currentUser.getId() == null) {
            Map<Long, ResponseSummary> summaries = buildResponseSummaries(items);
            return items.stream()
                    .map(item -> toDto(item, null, summaries.get(item.getId())))
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
                        Function.identity(),
                        (left, right) -> {
                            log.warn("Duplicate content responses detected for organizationId={}, contentId={}, userId={}. Keeping most recent row.",
                                    organizationId,
                                    left.getContentItem() != null ? left.getContentItem().getId() : null,
                                    currentUser.getId());
                            return isRightResponseNewer(left, right) ? right : left;
                        }
                ));
        Map<Long, ResponseSummary> summaries = buildResponseSummaries(items);

        return items.stream()
                .map(item -> toDto(item, responsesByContentId.get(item.getId()), summaries.get(item.getId())))
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

        OrganizationContentItem saved = contentRepository.save(item);
        log.debug("Organization content persisted: organizationId={}, moduleCode={}, contentType={}, contentId={}, createdByUserId={}",
                organizationId, type.getModuleCode(), type.name(), saved.getId(), createdBy != null ? createdBy.getId() : null);
        return toDto(saved);
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

        responseRepository.save(response);
        ResponseSummary summary = buildResponseSummary(contentId);
        return toDto(item, response, summary);
    }

    public OrganizationContentDto updateContentPublicationStatus(
            Integer organizationId,
            OrganizationContentType type,
            Long contentId,
            Boolean published
    ) {
        if (published == null) {
            throw new IllegalArgumentException("Published state is required");
        }

        requireModuleGranted(organizationId, type);
        OrganizationContentItem item = contentRepository
                .findByIdAndOrganizationIdAndType(contentId, organizationId, type)
                .orElseThrow(() -> new IllegalArgumentException("Content not found for this organization"));

        item.setPublished(published);
        OrganizationContentItem updated = contentRepository.save(item);
        ResponseSummary summary = buildResponseSummary(contentId);
        return toDto(updated, null, summary);
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

    private void requireModuleGranted(Integer organizationId, OrganizationContentType type) {
        OrganizationModule organizationModule = organizationModuleRepository
                .findByOrganizationIdAndModuleCode(organizationId, type.getModuleCode())
                .orElseThrow(() -> new AccessDeniedException("This module is not granted to the organization"));
        if (!Boolean.TRUE.equals(organizationModule.getGrantedBySaas())) {
            throw new AccessDeniedException("This module is not granted to the organization");
        }
    }

    private OrganizationContentDto toDto(OrganizationContentItem item) {
        return toDto(item, null, null);
    }

    private OrganizationContentDto toDto(
            OrganizationContentItem item,
            OrganizationContentResponse response,
            ResponseSummary summary
    ) {
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
                .totalResponses(summary != null ? summary.totalResponses() : 0L)
                .responseBreakdown(summary != null ? summary.breakdown() : Map.of())
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

    private Map<Long, ResponseSummary> buildResponseSummaries(List<OrganizationContentItem> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }

        List<Long> contentIds = items.stream()
                .map(OrganizationContentItem::getId)
                .toList();

        Map<Long, List<OrganizationContentResponse>> responsesByContentId = responseRepository
                .findByContentItemIdIn(contentIds)
                .stream()
                .collect(Collectors.groupingBy(response -> response.getContentItem().getId()));

        Map<Long, ResponseSummary> summaries = new HashMap<>();
        for (OrganizationContentItem item : items) {
            List<OrganizationContentResponse> responses =
                    responsesByContentId.getOrDefault(item.getId(), List.of());
            summaries.put(item.getId(), summarizeResponses(item, responses));
        }
        return summaries;
    }

    private ResponseSummary buildResponseSummary(Long contentId) {
        List<OrganizationContentResponse> responses = responseRepository.findByContentItemIdIn(List.of(contentId));
        return summarizeResponsesForContentId(contentId, responses);
    }

    private ResponseSummary summarizeResponsesForContentId(
            Long contentId,
            List<OrganizationContentResponse> responses
    ) {
        if (responses == null || responses.isEmpty()) {
            return new ResponseSummary(0L, Map.of());
        }

        OrganizationContentItem item = responses.get(0).getContentItem();
        if (item == null || !contentId.equals(item.getId())) {
            return new ResponseSummary(0L, Map.of());
        }

        return summarizeResponses(item, responses);
    }

    private ResponseSummary summarizeResponses(
            OrganizationContentItem item,
            List<OrganizationContentResponse> responses
    ) {
        if (responses == null || responses.isEmpty()) {
            return new ResponseSummary(0L, Map.of());
        }

        Map<String, Long> breakdown = new LinkedHashMap<>();

        OrganizationContentType safeType = resolveType(item, responses);
        if (safeType == null) {
            log.warn("Skipping response breakdown for contentId={} because content type is null or invalid. Falling back to empty breakdown.",
                    item != null ? item.getId() : null);
            return new ResponseSummary((long) responses.size(), Map.of());
        }

        switch (safeType) {
            case VOTE -> {
                List<String> options = toOptionsList(item.getOptionsText());
                for (String option : options) {
                    breakdown.put(option, 0L);
                }

                for (OrganizationContentResponse response : responses) {
                    String answer = response.getAnswer();
                    if (answer == null || answer.isBlank()) {
                        continue;
                    }

                    String matchingKey = breakdown.keySet()
                            .stream()
                            .filter(option -> option.equalsIgnoreCase(answer.trim()))
                            .findFirst()
                            .orElse(answer.trim());

                    breakdown.merge(matchingKey, 1L, Long::sum);
                }
            }
            case CONCERTATION -> {
                long participating = responses.stream()
                        .filter(response -> Boolean.TRUE.equals(response.getParticipating()))
                        .count();
                long notParticipating = responses.size() - participating;
                breakdown.put("participating", participating);
                breakdown.put("notParticipating", Math.max(notParticipating, 0L));
            }
            case YOUTH_NEWS -> {
                long reacted = responses.stream()
                        .filter(response -> response.getReaction() != null && !response.getReaction().isBlank())
                        .count();
                long followed = responses.size() - reacted;
                breakdown.put("reacted", reacted);
                breakdown.put("follow", Math.max(followed, 0L));
            }
        }

        return new ResponseSummary((long) responses.size(), breakdown);
    }

    private OrganizationContentType resolveType(
            OrganizationContentItem item,
            List<OrganizationContentResponse> responses
    ) {
        if (item != null && item.getType() != null) {
            return item.getType();
        }
        if (responses == null || responses.isEmpty()) {
            return null;
        }
        OrganizationContentResponse first = responses.get(0);
        return first != null ? first.getType() : null;
    }

    private boolean isRightResponseNewer(OrganizationContentResponse left, OrganizationContentResponse right) {
        if (left == null) {
            return true;
        }
        if (right == null) {
            return false;
        }
        if (left.getUpdatedAt() == null && right.getUpdatedAt() != null) {
            return true;
        }
        if (left.getUpdatedAt() != null && right.getUpdatedAt() == null) {
            return false;
        }
        if (left.getUpdatedAt() != null && right.getUpdatedAt() != null) {
            return right.getUpdatedAt().isAfter(left.getUpdatedAt());
        }
        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId == null) {
            return true;
        }
        if (rightId == null) {
            return false;
        }
        return rightId > leftId;
    }

    private record ResponseSummary(Long totalResponses, Map<String, Long> breakdown) {
    }
}
