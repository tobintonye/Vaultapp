package Vaultproject.Vaultapp.Repository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;


import Vaultproject.Vaultapp.Model.AuthProvider;
import Vaultproject.Vaultapp.Model.User;

// testing the UserRepository to ensure that we can save and retrieve users correctly

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {
    @Autowired
    private UserInfoRepository userRepository;
    
    @Test
    void findByEmail_whenUserExists_shouldReturnUser() {
        // Arrange: Create and save a user
        User user = new User();
        user.setFirstname("Tonye");
        user.setLastname("Tobin");
        user.setEmail("tonye@example.com");
        user.setPassword("password");
        user.setProvider(AuthProvider.LOCAL);
        userRepository.save(user);

        // ACT
        Optional<User> found = userRepository.findByEmail("tonye@example.com");

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("tonye@example.com");
    }

    @Test
    void findByEmail_whenUserDoesNotExist_shouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("ghost@nowhere.com");

        assertThat(found).isEmpty();
    }
}