package Vaultproject.Vaultapp.Services;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

/*
    AAA partern for unit testing:
    Arrange: set up the conditions for the test (e.g. create objects, set fields, etc.)
    Act: call the method being tested
    Assert: verify that the expected results occurred (e.g. check return values, check state of objects, etc.)
*/
@ExtendWith(MockitoExtension.class)  
public class JwtServiceTest {

    @InjectMocks // Creates a real JwtService
    private JwtService jwtService;

    @Mock // creates a fake userdetailsservice
    private UserDetailsService userDetailsService;
    
    private UserDetails mockUser;

    @BeforeEach // runs before each test
    public void setUp() {
    // Set the JWT secret via reflection (since it's @Value injected)
        String base64Secret = java.util.Base64.getEncoder()
                .encodeToString("mySecretKeyThatIsLongEnoughForHS256".getBytes());

        ReflectionTestUtils.setField(jwtService, "jwtAccessTokenSecret", base64Secret);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshTokenSecret", base64Secret);
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 86400000L);

        mockUser = User.builder()
                .username("tonye@example.com")
                .password("encodedPassword")
                .authorities(List.of())
                .build();
    }

    
    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtService.generateAccessToken(mockUser.getUsername());

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_shouldReturnCorrectEmail() {
        String token = jwtService.generateAccessToken(mockUser.getUsername());

        String extractedUsername = jwtService.extractUsername(token, false);

        assertThat(extractedUsername).isEqualTo("tonye@example.com");
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        String token = jwtService.generateAccessToken(mockUser.getUsername());

        boolean isValid = jwtService.validateAccessToken(token, mockUser);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_withWrongUser_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(mockUser.getUsername());

        UserDetails wrongUser = User.builder()
                .username("hacker@evil.com")
                .password("x")
                .authorities(List.of())
                .build();

        boolean isValid = jwtService.validateAccessToken(token, wrongUser);

        assertThat(isValid).isFalse();
    }
}
