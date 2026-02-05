package Vaultproject.Vaultapp.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Vaultproject.Vaultapp.Model.RefreshToken;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Repository.RefreshTokenRepository;
import Vaultproject.Vaultapp.Repository.UserInfoRepository;
import Vaultproject.Vaultapp.Services.JwtService;
import Vaultproject.Vaultapp.Services.RefreshTokenService;
import Vaultproject.Vaultapp.Services.UserInfoService;
import Vaultproject.Vaultapp.dto.AuthRequestDto;
import Vaultproject.Vaultapp.dto.AuthResponse;
import Vaultproject.Vaultapp.dto.MessageResponse;
import Vaultproject.Vaultapp.dto.RefreshTokenDTO;
import Vaultproject.Vaultapp.dto.RegisterDto;
import Vaultproject.Vaultapp.dto.ResetPasswordRequest;
import Vaultproject.Vaultapp.dto.VerificationRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vaultauth-api-v1")

public class AuthController {
    private final UserInfoService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserInfoRepository userInfoRepository;

    public AuthController(UserInfoService userService, JwtService jwtService,
        AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService,
        RefreshTokenRepository refreshTokenRepository, UserInfoRepository userInfoRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userInfoRepository = userInfoRepository;
    }

    @PostMapping("/register")
    public ResponseEntity <Map<String, String>> registerUser(@RequestBody RegisterDto registerDto) {
        try {
            User user = new User();
            user.setFirstname(registerDto.getFirstName());
            user.setLastname(registerDto.getLastName());
            user.setEmail(registerDto.getEmail());
            user.setPassword(registerDto.getPassword());
        
            String message = userService.saveUser(user);
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException  e) {
            return ResponseEntity.status(500).body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity <Map<String, String>> verifyEmail(@RequestParam String token) {
         userService.verifyEmail(token);    

        return ResponseEntity.ok(
            Map.of("message", "Email verified successfully")
        );
    };

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody VerificationRequest request) {
       
        String message = userService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(message));
    }
    
    // request password reset
     @PostMapping("/forgot-password")
     public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody VerificationRequest request) {
        String message = userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(message));
     }

     @PostMapping("/resend-passwordreset")
     public ResponseEntity<MessageResponse> resendPasswordToken(@Valid @RequestBody VerificationRequest request ) {
        String message = userService.resendPasswordToken(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(message));
     }
    // reset password
     @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }
    
    @PostMapping("/login")
    public AuthResponse loginUser(@RequestBody AuthRequestDto authRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authRequestDto.getUsername(), authRequestDto.getPassword())
        );

        if(authentication.isAuthenticated()) {
            // get the user
            User user = userInfoRepository.findByEmail(authRequestDto.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("Wrong credentials"));
        
              // Delete old refresh token if exists
              refreshTokenService.deleteToken(user);

            String accessToken =  jwtService.generateAccessToken(user.getEmail());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        
            return new AuthResponse(accessToken, refreshToken.getToken());
        } else {
            throw new UsernameNotFoundException("Wrong credentials");       
        }
    }

    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(@RequestBody RefreshTokenDTO request) {
        if(!refreshTokenService.validateRefreshToken(request.getToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        // fetch refresh token from the db
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getToken()) 
        .orElseThrow(() -> new RuntimeException("Refresh token not found"));
   
        String username = storedToken.getUser().getEmail();
        
        String newAccessToken = jwtService.generateAccessToken(username);

        return new AuthResponse(newAccessToken, storedToken.getToken());
    }

    @PostMapping("/logout")
    public String logout(@RequestBody RefreshTokenDTO request) {
        RefreshToken storedToken = refreshTokenService.findByToken(request.getToken())
        .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        refreshTokenService.deleteToken(storedToken.getUser());
        return "Logged out successfully";
    }
}
