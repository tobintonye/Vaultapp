package Vaultproject.Vaultapp.Services;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Repository.PasswordResetRepository;
import Vaultproject.Vaultapp.Repository.UserInfoRepository;
import Vaultproject.Vaultapp.Repository.VerificationTokenRepository;

import Vaultproject.Vaultapp.Model.AuthProvider;

@ExtendWith(MockitoExtension.class)
public class UserInfoServiceTest {
    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock 
    private VerificationTokenRepository verificationTokenRepository;
    
    @Mock 
    private  PasswordResetRepository passwordResetRepository;
    
    @Mock
    private  PasswordEncoder passwordEncoder;
    
    @Mock
    private  EmailService emailService;

    @InjectMocks
    private UserInfoService userInfoService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setEmail("tonye@gmail");
        user.setFirstname("tonye");
        user.setLastname("tobin");
        user.setPassword("password124");
        user.setProvider(AuthProvider.LOCAL);
    
    }

    @Test
    void ResgiterUser_saveToRepository() {
        when(userInfoRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(user.getPassword())).thenReturn("encodedPassword");
        when(userInfoRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

      String result = userInfoService.saveUser(user);

    assertEquals("Registration successful! Please check your email to verify your account.", result);
    }

    @Test
    void registerUser_existingEmail_throwsException() {
        when(userInfoRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userInfoService.saveUser(user);
        });

        assertEquals("Email already in use", exception.getMessage());

        verify(userInfoRepository).findByEmail(user.getEmail());
    }
}
