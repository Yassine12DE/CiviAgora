package tn.esprit.tic.civiAgora.dao.repository;

import org.springframework.stereotype.Repository;
import tn.esprit.tic.civiAgora.dao.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.tic.civiAgora.dao.entity.User;

import java.util.Optional;


@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

    void deleteByUser(User user);

}
