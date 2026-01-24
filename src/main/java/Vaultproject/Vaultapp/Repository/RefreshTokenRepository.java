package Vaultproject.Vaultapp.Repository;

import java.time.Instant;
import java.util.Optional;

import Vaultproject.Vaultapp.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import Vaultproject.Vaultapp.Model.RefreshToken;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);

    @Modifying
    @Transactional
    void deleteByExpiryDateBefore(Instant date);
}
