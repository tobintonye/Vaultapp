package Vaultproject.Vaultapp.Repository;
import Vaultproject.Vaultapp.Model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}