package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.tic.civiAgora.dao.entity.Module;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentItem;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentResponse;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationModule;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus;
import tn.esprit.tic.civiAgora.dao.entity.enums.SubscriptionStatus;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentItemRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationContentResponseRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationModuleRepository;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentDto;
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationContentServiceTest {

    @Mock
    private OrganizationContentItemRepository contentRepository;
    @Mock
    private OrganizationContentResponseRepository responseRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationModuleRepository organizationModuleRepository;
    @Mock
    private TenantAccessService tenantAccessService;
    @Spy
    private OrganizationSubscriptionAccessPolicy subscriptionAccessPolicy =
            new OrganizationSubscriptionAccessPolicy();

    @InjectMocks
    private OrganizationContentService service;

    @Test
    void postCreatedContent_isReadableImmediatelyByGet() {
        Integer organizationId = 5;
        User actor = User.builder().id(5).firstName("User").lastName("Five").build();
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setStatus(OrganizationStatus.ACTIVE);

        OrganizationModule grantedModule = grantedModule("CONFERENCE");

        OrganizationContentRequest request = OrganizationContentRequest.builder()
                .title("Concertation test")
                .body("body")
                .published(true)
                .build();

        OrganizationContentItem saved = OrganizationContentItem.builder()
                .id(101L)
                .organization(organization)
                .createdBy(actor)
                .type(OrganizationContentType.CONCERTATION)
                .title("Concertation test")
                .body("body")
                .published(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(organizationId, "CONFERENCE"))
                .thenReturn(Optional.of(grantedModule));
        when(contentRepository.save(any(OrganizationContentItem.class))).thenReturn(saved);
        when(contentRepository.findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(
                organizationId, OrganizationContentType.CONCERTATION
        )).thenReturn(List.of(saved));
        when(responseRepository.findByContentItemIdIn(List.of(101L))).thenReturn(List.of());
        when(responseRepository.findByOrganizationIdAndUserIdAndContentItemIdIn(organizationId, actor.getId(), List.of(101L)))
                .thenReturn(List.of());

        OrganizationContentDto created = service.createContent(
                organizationId,
                OrganizationContentType.CONCERTATION,
                request,
                actor
        );
        List<OrganizationContentDto> listed = assertDoesNotThrow(() ->
                service.getVisibleContentForCurrentUser(organizationId, OrganizationContentType.CONCERTATION, actor)
        );

        assertEquals(101L, created.getId());
        assertEquals(1, listed.size());
        assertEquals(101L, listed.get(0).getId());
        assertEquals("CONCERTATION", listed.get(0).getType());
    }

    @Test
    void getContent_doesNotThrow_whenDuplicateResponsesExistForSameContent() {
        Integer organizationId = 5;
        Integer userId = 5;
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setStatus(OrganizationStatus.ACTIVE);

        User actor = User.builder().id(userId).firstName("User").lastName("Five").build();
        OrganizationModule grantedModule = grantedModule("CONFERENCE");

        OrganizationContentItem item = OrganizationContentItem.builder()
                .id(101L)
                .organization(organization)
                .createdBy(actor)
                .type(OrganizationContentType.CONCERTATION)
                .title("t")
                .published(true)
                .createdAt(LocalDateTime.now())
                .build();

        OrganizationContentResponse r1 = OrganizationContentResponse.builder()
                .id(1L)
                .organization(organization)
                .contentItem(item)
                .user(actor)
                .type(OrganizationContentType.CONCERTATION)
                .participating(true)
                .updatedAt(LocalDateTime.now().minusMinutes(2))
                .build();
        OrganizationContentResponse r2 = OrganizationContentResponse.builder()
                .id(2L)
                .organization(organization)
                .contentItem(item)
                .user(actor)
                .type(OrganizationContentType.CONCERTATION)
                .participating(false)
                .updatedAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(organizationId, "CONFERENCE"))
                .thenReturn(Optional.of(grantedModule));
        when(contentRepository.findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(
                organizationId, OrganizationContentType.CONCERTATION
        )).thenReturn(List.of(item));
        when(responseRepository.findByOrganizationIdAndUserIdAndContentItemIdIn(organizationId, userId, List.of(101L)))
                .thenReturn(List.of(r1, r2));
        when(responseRepository.findByContentItemIdIn(List.of(101L))).thenReturn(List.of(r1, r2));

        List<OrganizationContentDto> result = assertDoesNotThrow(() ->
                service.getVisibleContentForCurrentUser(organizationId, OrganizationContentType.CONCERTATION, actor)
        );
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getTotalResponses());
    }

    @Test
    void legacyActiveOrganizationCanReadAndCreateVoteContent() {
        Integer organizationId = 1;
        Organization organization = legacyActiveOrganization(organizationId);
        User actor = User.builder().id(10).build();
        OrganizationContentRequest request = OrganizationContentRequest.builder()
                .title("Legacy vote")
                .published(true)
                .build();
        OrganizationContentItem saved = OrganizationContentItem.builder()
                .id(201L)
                .organization(organization)
                .createdBy(actor)
                .type(OrganizationContentType.VOTE)
                .title("Legacy vote")
                .published(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(organizationId, "VOTE"))
                .thenReturn(Optional.of(grantedModule("VOTE")));
        when(contentRepository.save(any(OrganizationContentItem.class))).thenReturn(saved);
        when(contentRepository.findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(
                organizationId, OrganizationContentType.VOTE
        )).thenReturn(List.of(saved));
        when(responseRepository.findByOrganizationIdAndUserIdAndContentItemIdIn(
                organizationId, actor.getId(), List.of(201L)
        )).thenReturn(List.of());
        when(responseRepository.findByContentItemIdIn(List.of(201L))).thenReturn(List.of());

        OrganizationContentDto created = assertDoesNotThrow(() -> service.createContent(
                organizationId, OrganizationContentType.VOTE, request, actor
        ));
        List<OrganizationContentDto> visible = assertDoesNotThrow(() ->
                service.getVisibleContentForCurrentUser(organizationId, OrganizationContentType.VOTE, actor)
        );

        assertEquals(201L, created.getId());
        assertEquals(1, visible.size());
    }

    @Test
    void suspendedSubscriptionCannotReadVoteContent() {
        assertSubscriptionStateBlocksRead(SubscriptionStatus.SUSPENDED, LocalDateTime.now().plusDays(30));
    }

    @Test
    void canceledSubscriptionCannotReadVoteContent() {
        assertSubscriptionStateBlocksRead(SubscriptionStatus.CANCELED, LocalDateTime.now().plusDays(30));
    }

    @Test
    void explicitlyExpiredSubscriptionCannotReadVoteContent() {
        assertSubscriptionStateBlocksRead(SubscriptionStatus.EXPIRED, LocalDateTime.now().plusDays(30));
    }

    @Test
    void missingModuleGrantCannotReadVoteContent() {
        Integer organizationId = 1;
        Organization organization = legacyActiveOrganization(organizationId);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(organizationId, "VOTE"))
                .thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                service.getVisibleContentForCurrentUser(organizationId, OrganizationContentType.VOTE, null)
        );

        assertEquals("This module is not granted to the organization", exception.getMessage());
        verify(contentRepository, never())
                .findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void publicContentUsesResolvedOrganizationWithoutAuthenticatedTenantAuthorization() {
        Organization organization = legacyActiveOrganization(1);
        when(tenantAccessService.getResolvedOrganizationOrThrow()).thenReturn(organization);
        when(organizationModuleRepository.findByOrganizationIdAndModuleCode(1, "VOTE"))
                .thenReturn(Optional.of(grantedModule("VOTE")));
        when(contentRepository.findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(
                1, OrganizationContentType.VOTE
        )).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getCurrentOrganizationPublicContent(OrganizationContentType.VOTE));

        verify(tenantAccessService, never()).assertOrganizationAccessOrThrow(any());
    }

    private void assertSubscriptionStateBlocksRead(SubscriptionStatus status, LocalDateTime endAt) {
        Integer organizationId = 1;
        Organization organization = legacyActiveOrganization(organizationId);
        organization.setSubscriptionStatus(status);
        organization.setSubscriptionEndAt(endAt);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                service.getVisibleContentForCurrentUser(organizationId, OrganizationContentType.VOTE, null)
        );

        assertEquals("This organization subscription is inactive", exception.getMessage());
        verify(organizationModuleRepository, never())
                .findByOrganizationIdAndModuleCode(any(), any());
    }

    private Organization legacyActiveOrganization(Integer id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setStatus(OrganizationStatus.ACTIVE);
        return organization;
    }

    private OrganizationModule grantedModule(String code) {
        Module module = new Module();
        module.setCode(code);
        module.setActive(true);
        return OrganizationModule.builder()
                .id(11L)
                .module(module)
                .grantedBySaas(true)
                .enabledByOrganization(true)
                .build();
    }
}
