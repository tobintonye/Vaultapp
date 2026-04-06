package Vaultproject.Vaultapp.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import Vaultproject.Vaultapp.Model.PasswordResetToken;
import Vaultproject.Vaultapp.Model.User;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query(
        """
        SELECT t FROM PasswordResetToken t 
         WHERE t.expiryDate > :now AND t.used = false"""
    )
    List<PasswordResetToken> findAllValidTokens(@Param("now") Instant now); 
    
    @Transactional
    void deleteByUser(User user);

    Optional<PasswordResetToken> findByUser(User user);

    // Optional<PasswordResetToken> findByToken(String token);
}


