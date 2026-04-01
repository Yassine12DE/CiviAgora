package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.tic.civiAgora.dao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByOrganizationId(Integer organizationId);

    List<User> findByArchived(Boolean archived);

    List<User> findByRole(Role role);

    boolean existsByEmail(String email);
    int countByOrganizationId(Integer organizationId);
    @Query("SELECT COUNT(u) FROM User u WHERE u.organization.id = :orgId")
    long countUsersByOrganization(@Param("orgId") Integer orgId);
}
