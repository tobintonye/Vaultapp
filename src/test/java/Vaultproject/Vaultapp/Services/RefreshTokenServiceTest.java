package Vaultproject.Vaultapp.Services;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.mockito.junit.jupiter.MockitoExtension;

import Vaultproject.Vaultapp.Model.RefreshToken;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Repository.RefreshTokenRepository;


@ExtendWith(MockitoExtension.class)  
public class RefreshTokenServiceTest {

    @Mock 
    private JwtService jwtService;

    @Mock 
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
   
    @BeforeEach
    public void setUp() {
        user = new User();
          user.setEmail("tonye@gmail");
          user.setPassword("password");
    }   
    @Test
    void createRefreshToken_shouldReturnNonNullToken() {
        when(jwtService.generateRefreshToken(user.getEmail())).thenReturn("mockRefreshToken");

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        RefreshToken token = refreshTokenService.createRefreshToken(user);
        assertNotNull(token);
        assertEquals("mockRefreshToken", token.getToken());
        assertEquals(user, token.getUser());
        assertNotNull(token.getExpiryDate());
        
        verify(jwtService).generateRefreshToken(user.getEmail());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test 
    void validateRefreshToken_shouldReturnFalse_whenExpired() {
        String tokenString = "mockRefresh";
        RefreshToken token = new RefreshToken();
        token.setToken(tokenString);
        token.setExpiryDate(java.time.Instant.now().minusSeconds(60)); // expired 1 minute ago

        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));

        boolean isValid = refreshTokenService.validateRefreshToken(tokenString);
        assertFalse(isValid, "Expected token to be expired");
        verify(refreshTokenRepository).findByToken(tokenString);
    }

}