package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentItem;
import tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationContentType;

import java.util.List;
import java.util.Optional;

public interface OrganizationContentItemRepository extends JpaRepository<OrganizationContentItem, Long> {
    List<OrganizationContentItem> findByOrganizationIdAndTypeOrderByCreatedAtDesc(
            Integer organizationId,
            OrganizationContentType type
    );

    List<OrganizationContentItem> findByOrganizationIdAndTypeAndPublishedTrueOrderByCreatedAtDesc(
            Integer organizationId,
            OrganizationContentType type
    );

    Optional<OrganizationContentItem> findByIdAndOrganizationIdAndType(
            Long id,
            Integer organizationId,
            OrganizationContentType type
    );
}
