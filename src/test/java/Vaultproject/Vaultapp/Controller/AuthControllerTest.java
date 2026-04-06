package Vaultproject.Vaultapp.Controller;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.authentication.AuthenticationManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import Vaultproject.Vaultapp.Model.RefreshToken;
import Vaultproject.Vaultapp.Model.User;

import org.springframework.security.core.Authentication;


import Vaultproject.Vaultapp.Repository.RefreshTokenRepository;
import Vaultproject.Vaultapp.Repository.UserInfoRepository;
import Vaultproject.Vaultapp.Services.GoogleRecaptchaService;
import Vaultproject.Vaultapp.Services.JwtService;
import Vaultproject.Vaultapp.Services.LoginRiskService;
import Vaultproject.Vaultapp.Services.RateLimiterService;
import Vaultproject.Vaultapp.Services.RefreshTokenService;
import Vaultproject.Vaultapp.Services.UserInfoService;
import Vaultproject.Vaultapp.Utility.ClientIpUtil;
import Vaultproject.Vaultapp.dto.AuthRequestDto;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.mockito.Mockito.verify;

import java.util.Optional;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for testing
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean private UserInfoService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private RefreshTokenService refreshTokenService;
    @MockitoBean private UserInfoRepository userInfoRepository;
    @MockitoBean private RateLimiterService rateLimiterService;
    @MockitoBean private ClientIpUtil clientIpUtil;
    @MockitoBean private LoginRiskService loginRiskService;
    @MockitoBean private GoogleRecaptchaService googleRecaptchaService;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

   @Test
    void login_withValidCredentials_shouldReturn200() throws Exception {
        AuthRequestDto request = new AuthRequestDto("tonye@gmail.com", "password", null);

    User mockUser = new User();
    mockUser.setEmail("tonye@gmail.com");
    mockUser.setFailedLoginAttempts(0);
    mockUser.setLockUntil(null);
    when(userInfoRepository.findByEmail("tonye@gmail.com")).thenReturn(Optional.of(mockUser));

    Authentication mockAuth = mock(Authentication.class);
    when(mockAuth.isAuthenticated()).thenReturn(true);
    
    doReturn(mockAuth).when(authenticationManager).authenticate(any());

    when(clientIpUtil.getClientIp(any())).thenReturn("127.0.0.1");
    when(rateLimiterService.isAllowed(any(), anyInt(), any())).thenReturn(true);
    when(loginRiskService.isCaptchaRequired(any())).thenReturn(false);
    when(jwtService.generateAccessToken(anyString())).thenReturn("mock-jwt-token");

    RefreshToken mockRefreshToken = new RefreshToken();
    mockRefreshToken.setToken("mock-refresh-token");
    when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(mockRefreshToken);

    mockMvc.perform(post("/vaultauth-api-v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(print()) 
            .andExpect(status().isOk());

    // confirms the mock was actually called
    verify(authenticationManager).authenticate(any());
}
}

