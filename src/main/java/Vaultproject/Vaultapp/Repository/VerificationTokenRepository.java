package Vaultproject.Vaultapp.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import Vaultproject.Vaultapp.Model.VerificationToken;
import Vaultproject.Vaultapp.Model.User;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long>{
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(User user);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
