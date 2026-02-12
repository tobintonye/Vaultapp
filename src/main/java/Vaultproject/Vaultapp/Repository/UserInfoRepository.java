package Vaultproject.Vaultapp.Repository;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Model.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(
        AuthProvider provider, String providerId
    );
}