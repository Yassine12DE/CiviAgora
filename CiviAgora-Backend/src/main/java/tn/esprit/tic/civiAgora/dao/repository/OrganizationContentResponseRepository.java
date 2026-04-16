package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationContentResponse;

import java.util.List;
import java.util.Optional;

public interface OrganizationContentResponseRepository extends JpaRepository<OrganizationContentResponse, Long> {
    Optional<OrganizationContentResponse> findByOrganizationIdAndContentItemIdAndUserId(
            Integer organizationId,
            Long contentItemId,
            Integer userId
    );

    List<OrganizationContentResponse> findByOrganizationIdAndUserIdAndContentItemIdIn(
            Integer organizationId,
            Integer userId,
            List<Long> contentItemIds
    );
}
