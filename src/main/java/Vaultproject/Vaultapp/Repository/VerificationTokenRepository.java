package Vaultproject.Vaultapp.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import Vaultproject.Vaultapp.Model.VerificationToken;
import Vaultproject.Vaultapp.Model.User;


@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long>{
    List<VerificationToken> findAllByExpiryDateAfter(LocalDateTime date);
    
    Optional<VerificationToken> findByUser(User user);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
