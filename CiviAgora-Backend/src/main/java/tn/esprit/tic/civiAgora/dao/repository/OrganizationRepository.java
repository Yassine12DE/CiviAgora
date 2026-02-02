package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.tic.civiAgora.dao.entity.Organization;

import java.util.Optional;


@Repository
public interface OrganizationRepository extends JpaRepository<Organization , Integer> {

    Optional<Organization> findBySlug(String slug);


}
