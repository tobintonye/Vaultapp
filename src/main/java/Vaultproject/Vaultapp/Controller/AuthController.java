package Vaultproject.Vaultapp.Controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import Vaultproject.Vaultapp.Model.RefreshToken;
import Vaultproject.Vaultapp.Model.User;
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
import Vaultproject.Vaultapp.dto.AuthResponse;
import Vaultproject.Vaultapp.dto.MessageResponse;
import Vaultproject.Vaultapp.dto.RefreshTokenDTO;
import Vaultproject.Vaultapp.dto.RegisterDto;
import Vaultproject.Vaultapp.dto.ResetPasswordRequest;
import Vaultproject.Vaultapp.dto.VerificationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@RestController
@RequestMapping("/vaultauth-api-v1")

public class AuthController {
    private final UserInfoService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserInfoRepository userInfoRepository;
    private final RateLimiterService rateLimiterService;
    private final ClientIpUtil clientIpUtil;
    private final LoginRiskService loginRiskService;
    private final GoogleRecaptchaService googleRecaptchaService;

    public AuthController(UserInfoService userService, JwtService jwtService,
        AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService,
        RefreshTokenRepository refreshTokenRepository, UserInfoRepository userInfoRepository, 
        RateLimiterService rateLimiterService, ClientIpUtil clientIpUtil,
        LoginRiskService loginRiskService, GoogleRecaptchaService googleRecaptchaService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userInfoRepository = userInfoRepository;
        this.rateLimiterService = rateLimiterService;
        this.clientIpUtil = clientIpUtil;
        this.loginRiskService = loginRiskService;
        this.googleRecaptchaService = googleRecaptchaService;
    }

    @PostMapping("/register")
    public ResponseEntity <Map<String, String>> registerUser(@RequestBody RegisterDto registerDto, HttpServletRequest httpRequest) {
        
        String ip = clientIpUtil.getClientIp(httpRequest);
        String email = registerDto.getEmail();

        // CHECK IP LIMIT (Sliding Window: 10 requests per 1 minute)
         String key = "rate_limit:register:IP:" + ip;
        if (!rateLimiterService.isAllowed(key, 10, Duration.ofMinutes(1))) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", "Too many attempts from this IP. Please wait a minute."));
    }

        String emailKey = "rate_limit:register:EMAIL" + email;

        if (!rateLimiterService.isAllowed(emailKey, 3, Duration.ofHours(1))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many attempts from this IP. Please wait a minute."));
        }

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
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody VerificationRequest request, HttpServletRequest httpRequest ) {
       
        String key = "rate_limit:resend_v:EMAIL:" + request.getEmail();

        if (!rateLimiterService.isAllowed(key, 3, Duration.ofHours(1))) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Too many login attempts. Try again later."));
        }

        String message = userService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(message));
    }
    
    // request password reset
     @PostMapping("/forgot-password")
     public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody VerificationRequest request, HttpServletRequest httpRequest) {
        String key = "rate_limit:reset_password:EMAIL:" + request.getEmail();

        if (!rateLimiterService.isAllowed(key, 3, Duration.ofHours(1))) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Too many login attempts. Try again later."));
        }
        
        String message = userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(message));
     }

    @PostMapping("/resend-passwordreset")
    public ResponseEntity<MessageResponse> resendPasswordToken(@Valid @RequestBody VerificationRequest request, HttpServletRequest httpRequest) {
    
    String ip = clientIpUtil.getClientIp(httpRequest);
    String email = request.getEmail();

    String ipKey = "rate_limit:resend_pw:IP:" + ip; 
    if (!rateLimiterService.isAllowed(ipKey, 10, Duration.ofMinutes(1))) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new MessageResponse("Too many requests from this connection. Wait a minute."));
    }

    String emailKey = "rate_limit:resend_pw:EMAIL:" + email;
    if (!rateLimiterService.isAllowed(emailKey, 3, Duration.ofHours(1))) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new MessageResponse("Too many reset attempts for this email. Try again in an hour."));
    }

    String message = userService.resendPasswordToken(email);
    return ResponseEntity.ok(new MessageResponse(message));
}

    // reset password
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }
    

    @PostMapping("/login")
        public ResponseEntity<?> loginUser(@RequestBody AuthRequestDto authRequestDto,HttpServletRequest httpRequest) {
            String ip = clientIpUtil.getClientIp(httpRequest);
            String email = authRequestDto.getUsername();

            // IP-based rate limit (5 attempts per minute)
            String ipKey = "rate_limit:login:IP:" + ip;
            if (!rateLimiterService.isAllowed(ipKey, 5, Duration.ofMinutes(1))) {
                log.warn("RATE LIMIT HIT for IP: {}", ip);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new MessageResponse("Too many login attempts from this IP. Wait 1 minute."));
            }

            // Email-based rate limit (3 attempts per minute)
            String emailKey = "rate_limit:login:EMAIL:" + email;
            if (!rateLimiterService.isAllowed(emailKey, 3, Duration.ofMinutes(1))) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new MessageResponse("Too many login attempts for this account. Wait 1 minute."));
            }

            Optional<User> userOpt = userInfoRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Check if account is locked (after 5 failed attempts)
                try {
                    userService.checkAccountNotLocked(user);
                } catch (RuntimeException e) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new MessageResponse("Account locked due to multiple failed attempts. Try again in 15 minutes."));
                }
                
                // CAPTCHA required after 3 failed attempts
                if (loginRiskService.isCaptchaRequired(user)) {
                    log.info("CAPTCHA required for user: {} (failed attempts: {})", 
                            email, user.getFailedLoginAttempts());
                    
                    try {
                        googleRecaptchaService.verify(authRequestDto.getCaptchaToken());
                        log.info("CAPTCHA verified successfully for user: {}", email);
                    } catch (RuntimeException e) {
                        log.warn("CAPTCHA verification failed for user: {}", email);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new MessageResponse("CAPTCHA_REQUIRED"));
                    }
                }
            }

            try {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, authRequestDto.getPassword())
                );

                if (authentication.isAuthenticated()) {
                    User user = userOpt.orElseThrow(() -> 
                        new UsernameNotFoundException("User not found"));

                    userService.handleSuccessfulLogin(user);
                    log.info("Login successful for user: {}", email);

                    refreshTokenService.deleteToken(user);
                    String accessToken = jwtService.generateAccessToken(user.getEmail());
                    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

                    return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
                }
                
                throw new BadCredentialsException("Authentication failed"); // just incase 

            } catch (AuthenticationException ex) {
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userService.handleFailedLogin(user);
                    log.warn("Login failed for user: {} (failed attempts: {})", 
                            email, user.getFailedLoginAttempts());
                    
                    // CAPTCHA is now required (after 3rd failure)
                    if (loginRiskService.isCaptchaRequired(user)) {
                        if (user.getFailedLoginAttempts() >= 5) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(new MessageResponse("Account locked due to multiple failed attempts. Try again in 15 minutes."));
                        }
                        
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new MessageResponse("CAPTCHA_REQUIRED"));
                    }
                }
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Invalid credentials"));
            }
        }

    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(@RequestBody RefreshTokenDTO request, HttpServletRequest httpRequest) {

        String ip = clientIpUtil.getClientIp(httpRequest);
        String ipKey = "rate_limit:refresh:IP:" + ip;

        if (!rateLimiterService.isAllowed(ipKey, 20, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
        }

        // Limits how many times a single refresh token can be used to ask for a new access token
        String tokenKey = "rate_limit:refresh:TOKEN:" + request.getToken().hashCode();

        if (!rateLimiterService.isAllowed(tokenKey, 5, Duration.ofMinutes(15))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Token refresh limit reached. Try later.");
        }


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
