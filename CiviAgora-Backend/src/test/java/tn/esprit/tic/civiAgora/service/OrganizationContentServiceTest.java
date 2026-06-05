package tn.esprit.tic.civiAgora.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import tn.esprit.tic.civiAgora.dto.contentDto.OrganizationContentRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private OrganizationContentService service;

    @Test
    void postCreatedContent_isReadableImmediatelyByGet() {
        Integer organizationId = 5;
        User actor = User.builder().id(5).firstName("User").lastName("Five").build();
        Organization organization = new Organization();
        organization.setId(organizationId);

        OrganizationModule grantedModule = OrganizationModule.builder()
                .id(11L)
                .grantedBySaas(true)
                .enabledByOrganization(true)
                .build();

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

        User actor = User.builder().id(userId).firstName("User").lastName("Five").build();
        OrganizationModule grantedModule = OrganizationModule.builder()
                .id(11L)
                .grantedBySaas(true)
                .enabledByOrganization(true)
                .build();

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
}
